package ru.pikahk.outdate.server.auth

import io.ktor.http.HttpStatusCode
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.HexFormat
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
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import ru.pikahk.outdate.server.db.SessionsTable
import ru.pikahk.outdate.server.db.UsersTable
import ru.pikahk.outdate.server.db.isUniqueViolation
import ru.pikahk.outdate.server.mail.Mailer
import ru.pikahk.outdate.server.plugins.ApiException

private const val TOKEN_BYTES = 32
private val SESSION_LIFETIME: Duration = Duration.ofDays(90)

data class AuthSession(val token: String, val email: String)

data class UserPrincipal(val id: UUID, val email: String, val emailVerified: Boolean, val token: String)

class AuthService(
    private val database: Database,
    private val mailer: Mailer,
    private val clock: Clock = Clock.systemUTC()
) {

    private val log = LoggerFactory.getLogger(AuthService::class.java)
    private val random = SecureRandom()
    private val codes = VerificationCodes(database, clock)
    private val dummyHash = PasswordHasher.hash(UUID.randomUUID().toString())

    suspend fun register(email: String, password: String): AuthSession = io {
        val normalized = normalizeEmail(email)
        if (!isValidEmail(normalized)) throw ApiException(HttpStatusCode.BadRequest, "invalid_email")
        if (!isValidPassword(password)) throw invalidPassword()
        val passwordHash = PasswordHasher.hash(password)
        val userId = UUID.randomUUID()
        val session = try {
            transaction(database) {
                if (findUserRow(normalized) != null) throw ApiException(HttpStatusCode.Conflict, "email_taken")
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
        runCatching { sendCode(userId, normalized, CodePurpose.VERIFY_EMAIL) }
        session
    }

    suspend fun login(email: String, password: String): AuthSession = io {
        val normalized = normalizeEmail(email)
        val user = transaction(database) { findUserRow(normalized) }
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
                ?.let { UserPrincipal(it[UsersTable.id], it[UsersTable.email], it[UsersTable.emailVerified], token) }
        }
    }

    suspend fun logout(token: String) {
        io { transaction(database) { SessionsTable.deleteWhere { tokenHash eq hashToken(token) } } }
    }

    suspend fun verifyEmail(user: UserPrincipal, code: String) {
        if (user.emailVerified) return
        io {
            codes.consume(user.id, CodePurpose.VERIFY_EMAIL, code)
            transaction(database) { UsersTable.update({ UsersTable.id eq user.id }) { it[emailVerified] = true } }
        }
    }

    suspend fun resendVerification(user: UserPrincipal) {
        if (user.emailVerified) throw ApiException(HttpStatusCode.Conflict, "already_verified")
        val sent = io { sendCode(user.id, user.email, CodePurpose.VERIFY_EMAIL) }
        if (!sent) throw ApiException(HttpStatusCode.TooManyRequests, "too_many_requests")
    }

    suspend fun requestPasswordReset(email: String) {
        val normalized = normalizeEmail(email)
        io {
            val user = transaction(database) { findUserRow(normalized) }
            if (user != null) sendCode(user[UsersTable.id], normalized, CodePurpose.RESET_PASSWORD)
        }
    }

    suspend fun resetPassword(email: String, code: String, password: String): AuthSession = io {
        if (!isValidPassword(password)) throw invalidPassword()
        val normalized = normalizeEmail(email)
        val user = transaction(database) { findUserRow(normalized) }
            ?: throw ApiException(HttpStatusCode.BadRequest, "code_expired")
        val userId = user[UsersTable.id]
        codes.consume(userId, CodePurpose.RESET_PASSWORD, code)
        val passwordHash = PasswordHasher.hash(password)
        transaction(database) {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.passwordHash] = passwordHash
                it[emailVerified] = true
            }
            SessionsTable.deleteWhere { SessionsTable.userId eq userId }
            AuthSession(createSession(userId), normalized)
        }
    }

    private fun sendCode(userId: UUID, email: String, purpose: CodePurpose): Boolean {
        val code = codes.issue(userId, purpose) ?: return false
        try {
            mailer.send(email, codeEmail(purpose, code))
        } catch (e: Exception) {
            codes.revoke(userId, purpose)
            log.error("Failed to send email", e)
            throw ApiException(HttpStatusCode.ServiceUnavailable, "mail_unavailable")
        }
        return true
    }

    private fun findUserRow(email: String): ResultRow? =
        UsersTable.selectAll().where { UsersTable.email eq email }.singleOrNull()

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

    private fun invalidPassword() = ApiException(HttpStatusCode.BadRequest, "invalid_password")

    private fun invalidCredentials() = ApiException(HttpStatusCode.Unauthorized, "invalid_credentials")

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
}

private fun hashToken(token: String): String =
    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))
