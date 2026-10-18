package ru.pikahk.outdate.server.routes

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.exposed.v1.jdbc.Database
import ru.pikahk.outdate.server.FakeMailer
import ru.pikahk.outdate.server.configure
import ru.pikahk.outdate.server.field
import ru.pikahk.outdate.server.serverTest

class HealthRoutesTest {

    @Test
    fun `сервер отвечает, что он и база работают`() = serverTest {
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok", response.field("status"))
        assertEquals("ok", response.field("database"))
    }

    @Test
    fun `если база недоступна, сервер сообщает об этом`() = testApplication {
        val unreachable = Database.connect("jdbc:postgresql://localhost:1/none", user = "none", password = "none")
        application { configure(unreachable, FakeMailer()) }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals("down", response.field("database"))
    }

    @Test
    fun `неизвестный адрес возвращает ошибку в едином формате`() = serverTest {
        val response = client.get("/nothing-here")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("not_found", response.field("error"))
    }
}
