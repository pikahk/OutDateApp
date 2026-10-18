package ru.pikahk.outdate.server.routes

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import ru.pikahk.outdate.server.PASSWORD
import ru.pikahk.outdate.server.field
import ru.pikahk.outdate.server.serverTest
import ru.pikahk.outdate.server.uniqueEmail

class AuthRoutesTest {

    @Test
    fun `после регистрации пользователь сразу вошёл`() = serverTest {
        val email = uniqueEmail()
        val token = register(" ${email.uppercase()} ")
        val response = me(token)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(email, response.field("email"))
        assertEquals("false", response.field("emailVerified"))
    }

    @Test
    fun `нельзя зарегистрироваться с неправильной почтой`() = serverTest {
        val response = post("/auth/register", "email" to "not-an-email", "password" to PASSWORD)
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("invalid_email", response.field("error"))
    }

    @Test
    fun `пароль должен быть от 8 до 64 символов`() = serverTest {
        for (wrong in listOf("a".repeat(7), "a".repeat(65))) {
            val response = post("/auth/register", "email" to uniqueEmail(), "password" to wrong)
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("invalid_password", response.field("error"))
        }
    }

    @Test
    fun `нельзя зарегистрировать одну почту дважды`() = serverTest {
        val email = uniqueEmail()
        register(email)
        val response = post("/auth/register", "email" to email.uppercase(), "password" to PASSWORD)
        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("email_taken", response.field("error"))
    }

    @Test
    fun `вход по почте и паролю выдаёт новый токен`() = serverTest {
        val email = uniqueEmail()
        val first = register(email)
        val response = post("/auth/login", "email" to email, "password" to PASSWORD)
        assertEquals(HttpStatusCode.OK, response.status)
        val second = checkNotNull(response.field("token"))
        assertNotEquals(first, second)
        assertEquals(HttpStatusCode.OK, me(second).status)
    }

    @Test
    fun `неверный пароль и неизвестная почта дают одинаковую ошибку`() = serverTest {
        val email = uniqueEmail()
        register(email)
        for (response in listOf(
            post("/auth/login", "email" to email, "password" to "wrong-password"),
            post("/auth/login", "email" to uniqueEmail(), "password" to PASSWORD)
        )) {
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("invalid_credentials", response.field("error"))
        }
    }

    @Test
    fun `после выхода токен перестаёт работать`() = serverTest {
        val token = register(uniqueEmail())
        assertEquals(HttpStatusCode.NoContent, post("/auth/logout", token = token).status)
        assertEquals(HttpStatusCode.Unauthorized, me(token).status)
    }

    @Test
    fun `без токена закрытые адреса недоступны`() = serverTest {
        for (response in listOf(client.get("/me"), me("made-up-token"))) {
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("unauthorized", response.field("error"))
        }
    }

    @Test
    fun `сломанный запрос возвращает ошибку в едином формате`() = serverTest {
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("{")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("bad_request", response.field("error"))
    }
}
