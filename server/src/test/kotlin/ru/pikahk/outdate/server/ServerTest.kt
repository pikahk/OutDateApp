package ru.pikahk.outdate.server

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
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val PASSWORD = "password123"

class ServerTest(val client: HttpClient, val mailer: FakeMailer, val clock: TestClock) {

    suspend fun post(path: String, vararg fields: Pair<String, String>, token: String? = null): HttpResponse =
        client.post(path) {
            token?.let { bearerAuth(it) }
            if (fields.isNotEmpty()) {
                contentType(ContentType.Application.Json)
                setBody(JsonObject(fields.associate { (name, value) -> name to JsonPrimitive(value) }).toString())
            }
        }

    suspend fun me(token: String): HttpResponse = client.get("/me") { bearerAuth(token) }

    suspend fun register(email: String, password: String = PASSWORD): String {
        val response = post("/auth/register", "email" to email, "password" to password)
        assertEquals(HttpStatusCode.Created, response.status)
        return checkNotNull(response.field("token"))
    }
}

fun serverTest(block: suspend ServerTest.() -> Unit) = testApplication {
    val mailer = FakeMailer()
    val clock = TestClock()
    application { configure(TestDatabase.connected.database, mailer, clock) }
    ServerTest(client, mailer, clock).block()
}

suspend fun HttpResponse.field(name: String): String? =
    Json.parseToJsonElement(bodyAsText()).jsonObject[name]?.jsonPrimitive?.content

fun uniqueEmail() = "user-${UUID.randomUUID()}@example.com"

fun wrongCode(code: String) = ((code.toInt() + 1) % 1_000_000).toString().padStart(6, '0')
