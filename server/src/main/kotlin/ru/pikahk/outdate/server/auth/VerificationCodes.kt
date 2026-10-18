package ru.pikahk.outdate.server.auth

import io.ktor.http.HttpStatusCode
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import ru.pikahk.outdate.server.db.EmailCodesTable
import ru.pikahk.outdate.server.plugins.ApiException

private const val CODE_BOUND = 1_000_000
private const val CODE_LENGTH = 6
private const val MAX_ATTEMPTS = 5
private val RESEND_INTERVAL: Duration = Duration.ofMinutes(1)
val CODE_LIFETIME: Duration = Duration.ofMinutes(15)

enum class CodePurpose(val key: String) {
    VERIFY_EMAIL("verify_email"),
    RESET_PASSWORD("reset_password")
}

private enum class CodeCheck { VALID, INVALID, EXPIRED }

class VerificationCodes(private val database: Database, private val clock: Clock) {

    private val random = SecureRandom()

    fun issue(userId: UUID, purpose: CodePurpose): String? = transaction(database) {
        val now = now()
        val previous = find(userId, purpose)
        if (previous != null && previous[EmailCodesTable.createdAt].isAfter(now.minus(RESEND_INTERVAL))) {
            return@transaction null
        }
        remove(userId, purpose)
        val code = random.nextInt(CODE_BOUND).toString().padStart(CODE_LENGTH, '0')
        EmailCodesTable.insert {
            it[EmailCodesTable.userId] = userId
            it[EmailCodesTable.purpose] = purpose.key
            it[EmailCodesTable.code] = code
            it[createdAt] = now
            it[expiresAt] = now.plus(CODE_LIFETIME)
        }
        code
    }

    fun consume(userId: UUID, purpose: CodePurpose, code: String) {
        val result = transaction(database) {
            val stored = find(userId, purpose)
            when {
                stored == null || !stored[EmailCodesTable.expiresAt].isAfter(now()) -> {
                    remove(userId, purpose)
                    CodeCheck.EXPIRED
                }
                MessageDigest.isEqual(stored[EmailCodesTable.code].toByteArray(), code.trim().toByteArray()) -> {
                    remove(userId, purpose)
                    CodeCheck.VALID
                }
                else -> {
                    val attempts = stored[EmailCodesTable.attempts] + 1
                    if (attempts >= MAX_ATTEMPTS) {
                        remove(userId, purpose)
                    } else {
                        EmailCodesTable.update({ matches(userId, purpose) }) { it[EmailCodesTable.attempts] = attempts }
                    }
                    CodeCheck.INVALID
                }
            }
        }
        when (result) {
            CodeCheck.VALID -> Unit
            CodeCheck.INVALID -> throw ApiException(HttpStatusCode.BadRequest, "invalid_code")
            CodeCheck.EXPIRED -> throw ApiException(HttpStatusCode.BadRequest, "code_expired")
        }
    }

    fun revoke(userId: UUID, purpose: CodePurpose) {
        transaction(database) { remove(userId, purpose) }
    }

    private fun find(userId: UUID, purpose: CodePurpose) =
        EmailCodesTable.selectAll().where { matches(userId, purpose) }.forUpdate().singleOrNull()

    private fun remove(userId: UUID, purpose: CodePurpose) {
        EmailCodesTable.deleteWhere { matches(userId, purpose) }
    }

    private fun matches(userId: UUID, purpose: CodePurpose) =
        (EmailCodesTable.userId eq userId) and (EmailCodesTable.purpose eq purpose.key)

    private fun now(): OffsetDateTime = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
}
