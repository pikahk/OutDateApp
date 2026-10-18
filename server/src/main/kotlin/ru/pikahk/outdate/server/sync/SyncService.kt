package ru.pikahk.outdate.server.sync

import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import ru.pikahk.outdate.server.auth.UserPrincipal
import ru.pikahk.outdate.server.db.CategoriesTable
import ru.pikahk.outdate.server.db.ItemsTable
import ru.pikahk.outdate.server.db.ProductsTable
import ru.pikahk.outdate.server.db.SubscriptionsTable
import ru.pikahk.outdate.server.db.SyncedTable
import ru.pikahk.outdate.server.db.UsersTable
import ru.pikahk.outdate.server.plugins.ApiException

private const val MAX_RECORDS = 1000
private const val MAX_KEY_LENGTH = 64

class SyncService(private val database: Database) {

    suspend fun sync(user: UserPrincipal, request: SyncRequest): SyncResponse {
        if (!user.emailVerified) throw ApiException(HttpStatusCode.Forbidden, "email_not_verified")
        validate(request)
        return withContext(Dispatchers.IO) {
            transaction(database) {
                val current = lockVersion(user.id)
                val since = if (request.since in 0..current) request.since else 0
                val next = current + 1
                val received = CategoriesTable.save(user.id, next, request.categories) { fill(it) } +
                    ItemsTable.save(user.id, next, request.items) { fill(it) } +
                    SubscriptionsTable.save(user.id, next, request.subscriptions) { fill(it) } +
                    ProductsTable.save(user.id, next, request.products) { fill(it) }
                val version = if (received > 0) next else current
                if (received > 0) UsersTable.update({ UsersTable.id eq user.id }) { it[syncVersion] = version }
                SyncResponse(
                    version = version,
                    categories = CategoriesTable.changedSince(user.id, since) { it.toCategory() },
                    items = ItemsTable.changedSince(user.id, since) { it.toItem() },
                    subscriptions = SubscriptionsTable.changedSince(user.id, since) { it.toSubscription() },
                    products = ProductsTable.changedSince(user.id, since) { it.toProduct() }
                )
            }
        }
    }

    private fun validate(request: SyncRequest) {
        val lists = listOf(request.categories, request.items, request.subscriptions, request.products)
        if (lists.any { it.size > MAX_RECORDS }) throw ApiException(HttpStatusCode.BadRequest, "too_many_changes")
        if (lists.flatten().any { it.key.isBlank() || it.key.length > MAX_KEY_LENGTH }) {
            throw ApiException(HttpStatusCode.BadRequest, "bad_request")
        }
    }

    private fun lockVersion(owner: UUID): Long = UsersTable
        .select(UsersTable.syncVersion)
        .where { UsersTable.id eq owner }
        .forUpdate()
        .single()[UsersTable.syncVersion]

    private fun <T : SyncRecord> SyncedTable.save(
        owner: UUID,
        newVersion: Long,
        records: List<T>,
        fill: UpdateBuilder<*>.(T) -> Unit
    ): Int {
        if (records.isEmpty()) return 0
        val latest = records.sortedBy { it.updatedAt }.associateBy { it.key }
        val stored = select(key, updatedAt)
            .where { (userId eq owner) and (key inList latest.keys) }
            .associate { it[key] to it[updatedAt] }
        val (newer, outdated) = latest.values.partition { record ->
            stored[record.key]?.let { record.updatedAt > it } ?: true
        }
        batchUpsert(newer) { record ->
            this[userId] = owner
            this[key] = record.key
            this[updatedAt] = record.updatedAt
            this[version] = newVersion
            fill(record)
        }
        if (outdated.isNotEmpty()) {
            update({ (userId eq owner) and (key inList outdated.map { it.key }) }) { it[version] = newVersion }
        }
        return latest.size
    }

    private fun <T> SyncedTable.changedSince(owner: UUID, since: Long, map: (ResultRow) -> T): List<T> =
        selectAll().where { (userId eq owner) and (version greater since) }.map(map)
}
