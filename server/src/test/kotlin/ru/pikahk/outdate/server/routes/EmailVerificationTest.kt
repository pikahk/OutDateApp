package ru.pikahk.outdate.server.routes

import io.ktor.http.HttpStatusCode
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import ru.pikahk.outdate.server.field
import ru.pikahk.outdate.server.serverTest
import ru.pikahk.outdate.server.uniqueEmail
import ru.pikahk.outdate.server.wrongCode

class EmailVerificationTest {

    @Test
    fun `после регистрации приходит код, и он подтверждает почту`() = serverTest {
        val email = uniqueEmail()
        val token = register(email)
        val response = post("/auth/email/verify", "code" to mailer.lastCode(email), token = token)
        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals("true", me(token).field("emailVerified"))
    }

    @Test
    fun `неверный код не подтверждает почту`() = serverTest {
        val email = uniqueEmail()
        val token = register(email)
        val response = post("/auth/email/verify", "code" to wrongCode(mailer.lastCode(email)), token = token)
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("invalid_code", response.field("error"))
        assertEquals("false", me(token).field("emailVerified"))
    }

    @Test
    fun `код перестаёт действовать через 15 минут`() = serverTest {
        val email = uniqueEmail()
        val token = register(email)
        clock.advance(Duration.ofMinutes(16))
        val response = post("/auth/email/verify", "code" to mailer.lastCode(email), token = token)
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("code_expired", response.field("error"))
    }

    @Test
    fun `после пяти ошибок код перестаёт действовать`() = serverTest {
        val email = uniqueEmail()
        val token = register(email)
        val code = mailer.lastCode(email)
        repeat(5) {
            val response = post("/auth/email/verify", "code" to wrongCode(code), token = token)
            assertEquals("invalid_code", response.field("error"))
        }
        assertEquals("code_expired", post("/auth/email/verify", "code" to code, token = token).field("error"))
    }

    @Test
    fun `новый код можно запросить не чаще раза в минуту`() = serverTest {
        val email = uniqueEmail()
        val token = register(email)
        val tooSoon = post("/auth/email/resend", token = token)
        assertEquals(HttpStatusCode.TooManyRequests, tooSoon.status)
        assertEquals("too_many_requests", tooSoon.field("error"))
        clock.advance(Duration.ofSeconds(61))
        assertEquals(HttpStatusCode.NoContent, post("/auth/email/resend", token = token).status)
        assertEquals(2, mailer.messagesTo(email).size)
        val response = post("/auth/email/verify", "code" to mailer.lastCode(email), token = token)
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `для подтверждённой почты новый код не отправляется`() = serverTest {
        val email = uniqueEmail()
        val token = register(email)
        post("/auth/email/verify", "code" to mailer.lastCode(email), token = token)
        val response = post("/auth/email/resend", token = token)
        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("already_verified", response.field("error"))
    }

    @Test
    fun `если почта не работает, регистрация проходит, а повторная отправка сообщает об ошибке`() = serverTest {
        mailer.failing = true
        val email = uniqueEmail()
        val token = register(email)
        val response = post("/auth/email/resend", token = token)
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals("mail_unavailable", response.field("error"))
        mailer.failing = false
        assertEquals(HttpStatusCode.NoContent, post("/auth/email/resend", token = token).status)
        assertEquals(1, mailer.messagesTo(email).size)
    }
}
