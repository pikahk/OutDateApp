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
import ru.pikahk.outdate.server.module

class HealthRoutesTest {

    private fun field(body: String, name: String): String? =
        Json.parseToJsonElement(body).jsonObject[name]?.jsonPrimitive?.content

    @Test
    fun `сервер отвечает, что он работает`() = testApplication {
        application { module() }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok", field(response.bodyAsText(), "status"))
    }

    @Test
    fun `неизвестный адрес возвращает ошибку в едином формате`() = testApplication {
        application { module() }
        val response = client.get("/nothing-here")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("not_found", field(response.bodyAsText(), "error"))
    }
}
