package ru.pikahk.outdate.server.routes

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.Database
import ru.pikahk.outdate.server.TestDatabase
import ru.pikahk.outdate.server.configure

class HealthRoutesTest {

    private fun field(body: String, name: String): String? =
        Json.parseToJsonElement(body).jsonObject[name]?.jsonPrimitive?.content

    @Test
    fun `сервер отвечает, что он и база работают`() = testApplication {
        application { configure(TestDatabase.connected.database) }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok", field(response.bodyAsText(), "status"))
        assertEquals("ok", field(response.bodyAsText(), "database"))
    }

    @Test
    fun `если база недоступна, сервер сообщает об этом`() = testApplication {
        val unreachable = Database.connect("jdbc:postgresql://localhost:1/none", user = "none", password = "none")
        application { configure(unreachable) }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals("down", field(response.bodyAsText(), "database"))
    }

    @Test
    fun `неизвестный адрес возвращает ошибку в едином формате`() = testApplication {
        application { configure(TestDatabase.connected.database) }
        val response = client.get("/nothing-here")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("not_found", field(response.bodyAsText(), "error"))
    }
}
