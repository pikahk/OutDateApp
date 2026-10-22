package ru.pikahk.outdate.server.family

import io.ktor.http.HttpStatusCode
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import ru.pikahk.outdate.server.auth.UserPrincipal
import ru.pikahk.outdate.server.db.FamiliesTable
import ru.pikahk.outdate.server.db.FamilyInvitesTable
import ru.pikahk.outdate.server.db.FamilyItemsTable
import ru.pikahk.outdate.server.db.FamilyMembersTable
import ru.pikahk.outdate.server.db.isUniqueViolation
import ru.pikahk.outdate.server.plugins.ApiException
import ru.pikahk.outdate.server.sync.changedSince
import ru.pikahk.outdate.server.sync.checkRecords
import ru.pikahk.outdate.server.sync.fill
import ru.pikahk.outdate.server.sync.save
import ru.pikahk.outdate.server.sync.toItem

private const val MAX_FAMILY_NAME = 40
private const val MAX_MEMBER_NAME = 24
private const val INVITE_LENGTH = 6
private const val INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
private val INVITE_LIFETIME: Duration = Duration.ofHours(24)
private val FAMILY_CATEGORIES = setOf("food", "medicine", "cosmetics")

class FamilyService(private val database: Database, private val clock: Clock = Clock.systemUTC()) {

    private val random = SecureRandom()

    suspend fun get(user: UserPrincipal): FamilyState = io(user) {
        FamilyState(membership(user.id)?.let { describe(it[FamilyMembersTable.familyId], user.id) })
    }

    suspend fun create(user: UserPrincipal, request: CreateFamilyRequest): FamilyDto {
        val name = cleanName(request.name, MAX_FAMILY_NAME)
        val memberName = cleanName(request.memberName, MAX_MEMBER_NAME)
        return io(user) {
            if (membership(user.id) != null) throw alreadyInFamily()
            val familyId = UUID.randomUUID()
            FamiliesTable.insert {
                it[id] = familyId
                it[FamiliesTable.name] = name
                it[ownerId] = user.id
            }
            addMember(familyId, user.id, memberName)
            describe(familyId, user.id)
        }
    }

    suspend fun join(user: UserPrincipal, request: JoinFamilyRequest): FamilyDto {
        val memberName = cleanName(request.memberName, MAX_MEMBER_NAME)
        val code = request.code.uppercase().filter { it.isLetterOrDigit() }
        return io(user) {
            if (membership(user.id) != null) throw alreadyInFamily()
            val familyId = FamilyInvitesTable.select(FamilyInvitesTable.familyId)
                .where { (FamilyInvitesTable.code eq code) and (FamilyInvitesTable.expiresAt greater now()) }
                .singleOrNull()
                ?.get(FamilyInvitesTable.familyId)
                ?: throw ApiException(HttpStatusCode.BadRequest, "invalid_code")
            addMember(familyId, user.id, memberName)
            describe(familyId, user.id)
        }
    }

    suspend fun invite(user: UserPrincipal): InviteResponse = io(user) {
        val familyId = ownedFamily(user.id)
        val code = generateSequence { newCode() }.first { candidate ->
            FamilyInvitesTable.selectAll().where { FamilyInvitesTable.code eq candidate }.empty()
        }
        val expiresAt = now().plus(INVITE_LIFETIME)
        FamilyInvitesTable.upsert {
            it[FamilyInvitesTable.familyId] = familyId
            it[FamilyInvitesTable.code] = code
            it[FamilyInvitesTable.expiresAt] = expiresAt
        }
        InviteResponse(code, expiresAt.toInstant().toEpochMilli())
    }

    suspend fun removeMember(user: UserPrincipal, memberId: String) {
        io(user) {
            val familyId = ownedFamily(user.id)
            val target = runCatching { UUID.fromString(memberId) }.getOrNull()
            val removed = if (target == null || target == user.id) {
                0
            } else {
                FamilyMembersTable.deleteWhere {
                    (FamilyMembersTable.userId eq target) and (FamilyMembersTable.familyId eq familyId)
                }
            }
            if (removed == 0) throw ApiException(HttpStatusCode.NotFound, "member_not_found")
        }
    }

    suspend fun leave(user: UserPrincipal) {
        io(user) {
            val familyId = familyOf(user.id)
            if (ownerOf(familyId) == user.id) throw ApiException(HttpStatusCode.Conflict, "owner_cannot_leave")
            FamilyMembersTable.deleteWhere { FamilyMembersTable.userId eq user.id }
        }
    }

    suspend fun delete(user: UserPrincipal) {
        io(user) {
            val familyId = ownedFamily(user.id)
            FamiliesTable.deleteWhere { FamiliesTable.id eq familyId }
        }
    }

    suspend fun sync(user: UserPrincipal, request: FamilySyncRequest): FamilySyncResponse {
        checkRecords(request.items)
        return io(user) {
            val member = membership(user.id) ?: throw noFamily()
            val familyId = member[FamilyMembersTable.familyId]
            val current = FamiliesTable.select(FamiliesTable.syncVersion)
                .where { FamiliesTable.id eq familyId }
                .forUpdate()
                .single()[FamiliesTable.syncVersion]
            val since = if (request.familyId == familyId.toString() && request.since in 0..current) request.since else 0
            val next = current + 1
            val items = request.items.map { it.copy(categoryId = it.categoryId?.takeIf(FAMILY_CATEGORIES::contains)) }
            val received = FamilyItemsTable.save(familyId, next, items, listOf(FamilyItemsTable.addedBy)) { item ->
                fill(FamilyItemsTable, item)
                this[FamilyItemsTable.addedBy] = member[FamilyMembersTable.name]
            }
            val version = if (received > 0) next else current
            if (received > 0) FamiliesTable.update({ FamiliesTable.id eq familyId }) { it[syncVersion] = version }
            FamilySyncResponse(
                familyId = familyId.toString(),
                version = version,
                items = FamilyItemsTable.changedSince(familyId, since) { it.toItem(FamilyItemsTable) }
            )
        }
    }

    private fun membership(userId: UUID): ResultRow? =
        FamilyMembersTable.selectAll().where { FamilyMembersTable.userId eq userId }.singleOrNull()

    private fun familyOf(userId: UUID): UUID = membership(userId)?.get(FamilyMembersTable.familyId) ?: throw noFamily()

    private fun ownerOf(familyId: UUID): UUID = FamiliesTable.select(FamiliesTable.ownerId)
        .where { FamiliesTable.id eq familyId }
        .single()[FamiliesTable.ownerId]

    private fun ownedFamily(userId: UUID): UUID {
        val familyId = familyOf(userId)
        if (ownerOf(familyId) != userId) throw ApiException(HttpStatusCode.Forbidden, "not_owner")
        return familyId
    }

    private fun addMember(familyId: UUID, userId: UUID, name: String) {
        FamilyMembersTable.insert {
            it[FamilyMembersTable.userId] = userId
            it[FamilyMembersTable.familyId] = familyId
            it[FamilyMembersTable.name] = name
            it[joinedAt] = now()
        }
    }

    private fun describe(familyId: UUID, me: UUID): FamilyDto {
        val family = FamiliesTable.selectAll().where { FamiliesTable.id eq familyId }.single()
        val ownerId = family[FamiliesTable.ownerId]
        val members = FamilyMembersTable.selectAll()
            .where { FamilyMembersTable.familyId eq familyId }
            .orderBy(FamilyMembersTable.joinedAt)
            .map { row ->
                val id = row[FamilyMembersTable.userId]
                MemberDto(
                    id = id.toString(),
                    name = row[FamilyMembersTable.name],
                    isOwner = id == ownerId,
                    isMe = id == me,
                    joinedAt = row[FamilyMembersTable.joinedAt].toInstant().toEpochMilli()
                )
            }
        return FamilyDto(familyId.toString(), family[FamiliesTable.name], ownerId == me, members)
    }

    private fun newCode(): String = buildString {
        repeat(INVITE_LENGTH) { append(INVITE_ALPHABET[random.nextInt(INVITE_ALPHABET.length)]) }
    }

    private fun cleanName(value: String, maxLength: Int): String {
        val name = value.trim()
        if (name.isEmpty() || name.length > maxLength) throw ApiException(HttpStatusCode.BadRequest, "invalid_name")
        return name
    }

    private fun now(): OffsetDateTime = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)

    private fun noFamily() = ApiException(HttpStatusCode.NotFound, "no_family")

    private fun alreadyInFamily() = ApiException(HttpStatusCode.Conflict, "already_in_family")

    private suspend fun <T> io(user: UserPrincipal, block: () -> T): T {
        if (!user.emailVerified) throw ApiException(HttpStatusCode.Forbidden, "email_not_verified")
        return try {
            withContext(Dispatchers.IO) { transaction(database) { block() } }
        } catch (e: Exception) {
            if (e.isUniqueViolation()) throw alreadyInFamily()
            throw e
        }
    }
}
