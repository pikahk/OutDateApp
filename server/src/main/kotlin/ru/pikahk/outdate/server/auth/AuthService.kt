package ru.pikahk.outdate.server.auth

import io.ktor.http.HttpStatusCode
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.SQLException
import java.time.Clock
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.HexFormat
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import ru.pikahk.outdate.server.db.SessionsTable
import ru.pikahk.outdate.server.db.UsersTable
import ru.pikahk.outdate.server.plugins.ApiException

private const val TOKEN_BYTES = 32
private const val UNIQUE_VIOLATION = "23505"
private val SESSION_LIFETIME: Duration = Duration.ofDays(90)

data class AuthSession(val token: String, val email: String)

data class UserPrincipal(val id: UUID, val email: String, val token: String)

class AuthService(private val database: Database, private val clock: Clock = Clock.systemUTC()) {

    private val random = SecureRandom()
    private val dummyHash = PasswordHasher.hash(UUID.randomUUID().toString())

    suspend fun register(email: String, password: String): AuthSession = io {
        val normalized = normalizeEmail(email)
        if (!isValidEmail(normalized)) throw ApiException(HttpStatusCode.BadRequest, "invalid_email")
        if (!isValidPassword(password)) throw ApiException(HttpStatusCode.BadRequest, "invalid_password")
        val passwordHash = PasswordHasher.hash(password)
        try {
            transaction(database) {
                if (UsersTable.selectAll().where { UsersTable.email eq normalized }.any()) {
                    throw ApiException(HttpStatusCode.Conflict, "email_taken")
                }
                val userId = UUID.randomUUID()
                UsersTable.insert {
                    it[id] = userId
                    it[UsersTable.email] = normalized
                    it[UsersTable.passwordHash] = passwordHash
                }
                AuthSession(createSession(userId), normalized)
            }
        } catch (e: Exception) {
            if (e.isUniqueViolation()) throw ApiException(HttpStatusCode.Conflict, "email_taken")
            throw e
        }
    }

    suspend fun login(email: String, password: String): AuthSession = io {
        val normalized = normalizeEmail(email)
        val user = transaction(database) {
            UsersTable.selectAll().where { UsersTable.email eq normalized }.singleOrNull()
        }
        if (user == null) {
            PasswordHasher.verify(password, dummyHash)
            throw invalidCredentials()
        }
        if (!PasswordHasher.verify(password, user[UsersTable.passwordHash])) throw invalidCredentials()
        transaction(database) { AuthSession(createSession(user[UsersTable.id]), normalized) }
    }

    suspend fun findUser(token: String): UserPrincipal? = io {
        transaction(database) {
            (SessionsTable innerJoin UsersTable)
                .selectAll()
                .where { (SessionsTable.tokenHash eq hashToken(token)) and (SessionsTable.expiresAt greater now()) }
                .singleOrNull()
                ?.let { UserPrincipal(it[UsersTable.id], it[UsersTable.email], token) }
        }
    }

    suspend fun logout(token: String) {
        io { transaction(database) { SessionsTable.deleteWhere { tokenHash eq hashToken(token) } } }
    }

    private fun createSession(userId: UUID): String {
        val bytes = ByteArray(TOKEN_BYTES).also(random::nextBytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        SessionsTable.insert {
            it[tokenHash] = hashToken(token)
            it[SessionsTable.userId] = userId
            it[expiresAt] = now().plus(SESSION_LIFETIME)
        }
        return token
    }

    private fun now(): OffsetDateTime = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)

    private fun invalidCredentials() = ApiException(HttpStatusCode.Unauthorized, "invalid_credentials")

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
}

private fun hashToken(token: String): String =
    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))

private fun Throwable.isUniqueViolation(): Boolean =
    generateSequence(this) { it.cause }.any { it is SQLException && it.sqlState == UNIQUE_VIOLATION }
