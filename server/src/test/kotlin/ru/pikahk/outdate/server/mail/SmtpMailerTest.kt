package ru.pikahk.outdate.server.mail

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

private const val SMTP_PORT = 1025
private const val API_PORT = 8025

class MailpitContainer : GenericContainer<MailpitContainer>(DockerImageName.parse("axllent/mailpit:v1.31")) {
    init {
        withExposedPorts(SMTP_PORT, API_PORT)
        waitingFor(Wait.forHttp("/livez").forPort(API_PORT))
    }

    fun get(path: String): JsonObject {
        val uri = URI("http://$host:${getMappedPort(API_PORT)}$path")
        val body = HttpClient.newHttpClient()
            .send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString())
            .body()
        return Json.parseToJsonElement(body).jsonObject
    }
}

class SmtpMailerTest {

    @Test
    fun `письмо доходит до почтового сервера`() {
        MailpitContainer().use { mailpit ->
            mailpit.start()
            val settings = MailSettings(
                host = mailpit.host,
                port = mailpit.getMappedPort(SMTP_PORT),
                user = "",
                password = "",
                security = MailSecurity.NONE,
                from = "outDate <noreply@outdateapp.ru>"
            )
            SmtpMailer(settings).send("user@example.com", EmailMessage("Проверка", "Код подтверждения: 123456"))

            val summary = mailpit.get("/api/v1/messages").getValue("messages").jsonArray.single().jsonObject
            assertEquals("Проверка", summary.getValue("Subject").jsonPrimitive.content)
            val to = summary.getValue("To").jsonArray.single().jsonObject
            assertEquals("user@example.com", to.getValue("Address").jsonPrimitive.content)
            val from = summary.getValue("From").jsonObject
            assertEquals("noreply@outdateapp.ru", from.getValue("Address").jsonPrimitive.content)

            val message = mailpit.get("/api/v1/message/${summary.getValue("ID").jsonPrimitive.content}")
            assertContains(message.getValue("Text").jsonPrimitive.content, "Код подтверждения: 123456")
        }
    }
}
