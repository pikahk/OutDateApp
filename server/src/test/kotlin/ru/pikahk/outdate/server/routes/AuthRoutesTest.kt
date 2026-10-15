package ru.pikahk.outdate.server.routes

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.pikahk.outdate.server.TestDatabase
import ru.pikahk.outdate.server.configure

class AuthRoutesTest {

    private val password = "password123"

    private fun uniqueEmail() = "user-${UUID.randomUUID()}@example.com"

    private fun field(body: String, name: String): String? =
        Json.parseToJsonElement(body).jsonObject[name]?.jsonPrimitive?.content

    private fun authTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application { configure(TestDatabase.connected.database) }
        block()
    }

    private suspend fun HttpClient.sendCredentials(path: String, email: String, password: String): HttpResponse =
        post(path) {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }

    private suspend fun HttpClient.register(email: String): String {
        val response = sendCredentials("/auth/register", email, password)
        assertEquals(HttpStatusCode.Created, response.status)
        return checkNotNull(field(response.bodyAsText(), "token"))
    }

    private suspend fun HttpClient.me(token: String): HttpResponse = get("/me") { bearerAuth(token) }

    @Test
    fun `после регистрации пользователь сразу вошёл`() = authTest {
        val email = uniqueEmail()
        val token = client.register(" ${email.uppercase()} ")
        val response = client.me(token)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(email, field(response.bodyAsText(), "email"))
    }

    @Test
    fun `нельзя зарегистрироваться с неправильной почтой`() = authTest {
        val response = client.sendCredentials("/auth/register", "not-an-email", password)
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("invalid_email", field(response.bodyAsText(), "error"))
    }

    @Test
    fun `пароль должен быть от 8 до 64 символов`() = authTest {
        for (wrong in listOf("a".repeat(7), "a".repeat(65))) {
            val response = client.sendCredentials("/auth/register", uniqueEmail(), wrong)
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("invalid_password", field(response.bodyAsText(), "error"))
        }
    }

    @Test
    fun `нельзя зарегистрировать одну почту дважды`() = authTest {
        val email = uniqueEmail()
        client.register(email)
        val response = client.sendCredentials("/auth/register", email.uppercase(), password)
        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("email_taken", field(response.bodyAsText(), "error"))
    }

    @Test
    fun `вход по почте и паролю выдаёт новый токен`() = authTest {
        val email = uniqueEmail()
        val first = client.register(email)
        val response = client.sendCredentials("/auth/login", email, password)
        assertEquals(HttpStatusCode.OK, response.status)
        val second = checkNotNull(field(response.bodyAsText(), "token"))
        assertNotEquals(first, second)
        assertEquals(HttpStatusCode.OK, client.me(second).status)
    }

    @Test
    fun `неверный пароль и неизвестная почта дают одинаковую ошибку`() = authTest {
        val email = uniqueEmail()
        client.register(email)
        for (response in listOf(
            client.sendCredentials("/auth/login", email, "wrong-password"),
            client.sendCredentials("/auth/login", uniqueEmail(), password)
        )) {
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("invalid_credentials", field(response.bodyAsText(), "error"))
        }
    }

    @Test
    fun `после выхода токен перестаёт работать`() = authTest {
        val token = client.register(uniqueEmail())
        assertEquals(HttpStatusCode.NoContent, client.post("/auth/logout") { bearerAuth(token) }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.me(token).status)
    }

    @Test
    fun `без токена закрытые адреса недоступны`() = authTest {
        for (response in listOf(client.get("/me"), client.me("made-up-token"))) {
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("unauthorized", field(response.bodyAsText(), "error"))
        }
    }

    @Test
    fun `сломанный запрос возвращает ошибку в едином формате`() = authTest {
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("{")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("bad_request", field(response.bodyAsText(), "error"))
    }
}
