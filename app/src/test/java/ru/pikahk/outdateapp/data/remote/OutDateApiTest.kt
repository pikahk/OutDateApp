package ru.pikahk.outdateapp.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OutDateApiTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(handler: MockRequestHandler) = OutDateApi("https://api.test/", MockEngine(handler))

    private fun failure(api: OutDateApi, request: suspend OutDateApi.() -> Unit): String =
        assertThrows(ApiException::class.java) { runBlocking { api.request() } }.code

    @Test
    fun `регистрация отправляет почту и пароль и возвращает токен`() = runBlocking {
        var body = ""
        val api = api { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("https://api.test/auth/register", request.url.toString())
            body = request.body.toByteArray().decodeToString()
            respond("""{"token":"abc","email":"misha@example.com"}""", HttpStatusCode.Created, jsonHeaders)
        }
        val response = api.register("misha@example.com", "password123")
        assertEquals(AuthResponse("abc", "misha@example.com"), response)
        assertEquals("""{"email":"misha@example.com","password":"password123"}""", body)
    }

    @Test
    fun `запросы от имени пользователя передают токен`() = runBlocking {
        var header: String? = null
        val api = api { request ->
            header = request.headers[HttpHeaders.Authorization]
            respond("""{"email":"misha@example.com","emailVerified":true}""", HttpStatusCode.OK, jsonHeaders)
        }
        assertEquals(MeResponse("misha@example.com", emailVerified = true), api.me("abc"))
        assertEquals("Bearer abc", header)
    }

    @Test
    fun `ошибка сервера превращается в её код`() {
        val api = api { respond("""{"error":"email_taken"}""", HttpStatusCode.Conflict, jsonHeaders) }
        assertEquals("email_taken", failure(api) { register("misha@example.com", "password123") })
    }

    @Test
    fun `ответ без нужных данных считается ошибкой сервера`() {
        val html = headersOf(HttpHeaders.ContentType, "text/html")
        val api = api { respond("<html>Bad gateway</html>", HttpStatusCode.BadGateway, html) }
        assertEquals(ApiException.SERVER, failure(api) { login("misha@example.com", "password123") })
        val portal = api { respond("<html>Wi-Fi</html>", HttpStatusCode.OK, html) }
        assertEquals(ApiException.SERVER, failure(portal) { login("misha@example.com", "password123") })
    }

    @Test
    fun `нет сети`() {
        val api = api { throw IOException("offline") }
        assertEquals(ApiException.NETWORK, failure(api) { forgotPassword("misha@example.com") })
    }
}
