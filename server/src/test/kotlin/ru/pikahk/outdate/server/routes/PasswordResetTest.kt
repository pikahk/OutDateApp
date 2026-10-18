package ru.pikahk.outdate.server.routes

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.pikahk.outdate.server.PASSWORD
import ru.pikahk.outdate.server.ServerTest
import ru.pikahk.outdate.server.field
import ru.pikahk.outdate.server.serverTest
import ru.pikahk.outdate.server.uniqueEmail
import ru.pikahk.outdate.server.wrongCode

class PasswordResetTest {

    private val newPassword = "new-password-456"

    private suspend fun ServerTest.reset(email: String, code: String, password: String = newPassword) =
        post("/auth/password/reset", "email" to email, "code" to code, "password" to password)

    @Test
    fun `код из письма позволяет задать новый пароль`() = serverTest {
        val email = uniqueEmail()
        val oldToken = register(email)
        assertEquals(HttpStatusCode.NoContent, post("/auth/password/forgot", "email" to email).status)
        val response = reset(email, mailer.lastCode(email))
        assertEquals(HttpStatusCode.OK, response.status)
        val newToken = checkNotNull(response.field("token"))
        assertEquals(HttpStatusCode.Unauthorized, me(oldToken).status)
        assertEquals("true", me(newToken).field("emailVerified"))
        assertEquals(HttpStatusCode.Unauthorized, post("/auth/login", "email" to email, "password" to PASSWORD).status)
        assertEquals(HttpStatusCode.OK, post("/auth/login", "email" to email, "password" to newPassword).status)
    }

    @Test
    fun `для незарегистрированной почты ответ такой же, но письмо не уходит`() = serverTest {
        val email = uniqueEmail()
        assertEquals(HttpStatusCode.NoContent, post("/auth/password/forgot", "email" to email).status)
        assertTrue(mailer.messagesTo(email).isEmpty())
    }

    @Test
    fun `повторный запрос раньше чем через минуту не отправляет новое письмо`() = serverTest {
        val email = uniqueEmail()
        register(email)
        repeat(2) {
            assertEquals(HttpStatusCode.NoContent, post("/auth/password/forgot", "email" to email).status)
        }
        assertEquals(2, mailer.messagesTo(email).size)
    }

    @Test
    fun `с неверным кодом пароль не меняется`() = serverTest {
        val email = uniqueEmail()
        register(email)
        post("/auth/password/forgot", "email" to email)
        val response = reset(email, wrongCode(mailer.lastCode(email)))
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("invalid_code", response.field("error"))
        assertEquals(HttpStatusCode.OK, post("/auth/login", "email" to email, "password" to PASSWORD).status)
    }

    @Test
    fun `код подтверждения почты не подходит для сброса пароля`() = serverTest {
        val email = uniqueEmail()
        register(email)
        val response = reset(email, mailer.lastCode(email))
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("code_expired", response.field("error"))
    }

    @Test
    fun `новый пароль проверяется так же, как при регистрации`() = serverTest {
        val email = uniqueEmail()
        register(email)
        post("/auth/password/forgot", "email" to email)
        val code = mailer.lastCode(email)
        val response = reset(email, code, password = "short")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("invalid_password", response.field("error"))
        assertEquals(HttpStatusCode.OK, reset(email, code).status)
    }
}
