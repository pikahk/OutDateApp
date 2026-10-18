package ru.pikahk.outdate.server.routes

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import ru.pikahk.outdate.server.ServerTest
import ru.pikahk.outdate.server.field
import ru.pikahk.outdate.server.serverTest
import ru.pikahk.outdate.server.sync.CategoryDto
import ru.pikahk.outdate.server.sync.ItemDto
import ru.pikahk.outdate.server.sync.PeriodType
import ru.pikahk.outdate.server.sync.ProductDto
import ru.pikahk.outdate.server.sync.SubscriptionDto
import ru.pikahk.outdate.server.sync.SyncRequest
import ru.pikahk.outdate.server.sync.SyncResponse
import ru.pikahk.outdate.server.uniqueEmail

class SyncRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun item(id: String = UUID.randomUUID().toString(), name: String = "Молоко", updatedAt: Long = 1_000) =
        ItemDto(
            id = id,
            name = name,
            categoryId = "food",
            barcode = "4600000000001",
            expiresAt = LocalDate.of(2026, 11, 1),
            daysAfterOpening = 3,
            openedAt = LocalDate.of(2026, 10, 20),
            notifyDaysBefore = 2,
            createdAt = 500,
            updatedAt = updatedAt
        )

    private val category = CategoryDto(id = UUID.randomUUID().toString(), name = "Бытовая химия", updatedAt = 1_000)

    private val subscription = SubscriptionDto(
        id = UUID.randomUUID().toString(),
        name = "Музыка",
        priceMinor = 29_900,
        currency = "RUB",
        startedAt = LocalDate.of(2026, 9, 15),
        periodType = PeriodType.MONTHLY,
        notifyDaysBefore = 1,
        updatedAt = 1_000
    )

    private val product = ProductDto(
        barcode = "4600000000001",
        name = "Молоко",
        categoryId = "food",
        defaultDaysAfterOpening = 3,
        updatedAt = 1_000
    )

    private suspend fun ServerTest.sendSync(token: String, body: String): HttpResponse = client.post("/sync") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend fun ServerTest.sync(token: String, request: SyncRequest = SyncRequest()): SyncResponse {
        val response = sendSync(token, json.encodeToString(request))
        assertEquals(HttpStatusCode.OK, response.status)
        return json.decodeFromString(response.bodyAsText())
    }

    @Test
    fun `у нового пользователя на сервере пока ничего нет`() = serverTest {
        val response = sync(registerVerified())
        assertEquals(0, response.version)
        assertTrue(response.items.isEmpty() && response.categories.isEmpty())
    }

    @Test
    fun `данные с одного устройства появляются на другом`() = serverTest {
        val email = uniqueEmail()
        val phone = registerVerified(email)
        val sent = SyncRequest(
            categories = listOf(category),
            items = listOf(item()),
            subscriptions = listOf(subscription),
            products = listOf(product)
        )
        assertEquals(1, sync(phone, sent).version)

        val tablet = sync(login(email))
        assertEquals(1, tablet.version)
        assertEquals(sent.categories, tablet.categories)
        assertEquals(sent.items, tablet.items)
        assertEquals(sent.subscriptions, tablet.subscriptions)
        assertEquals(sent.products, tablet.products)
    }

    @Test
    fun `приходят только изменения после указанного номера`() = serverTest {
        val token = registerVerified()
        val first = item(name = "Первый")
        val second = item(name = "Второй")
        sync(token, SyncRequest(items = listOf(first)))
        val response = sync(token, SyncRequest(since = 1, items = listOf(second)))
        assertEquals(2, response.version)
        assertEquals(listOf(second), response.items)
        val nothingNew = sync(token, SyncRequest(since = 2))
        assertEquals(2, nothingNew.version)
        assertTrue(nothingNew.items.isEmpty())
    }

    @Test
    fun `побеждает более позднее изменение`() = serverTest {
        val token = registerVerified()
        val id = UUID.randomUUID().toString()
        sync(token, SyncRequest(items = listOf(item(id, name = "Новое", updatedAt = 2_000))))
        val outdated = sync(token, SyncRequest(since = 1, items = listOf(item(id, name = "Старое", updatedAt = 1_000))))
        assertEquals(listOf("Новое"), outdated.items.map { it.name })
        assertEquals(listOf("Новое"), sync(token).items.map { it.name })
        sync(token, SyncRequest(since = 1, items = listOf(item(id, name = "Новейшее", updatedAt = 3_000))))
        assertEquals(listOf("Новейшее"), sync(token).items.map { it.name })
    }

    @Test
    fun `удаление тоже передаётся на другие устройства`() = serverTest {
        val email = uniqueEmail()
        val phone = registerVerified(email)
        val milk = item(updatedAt = 1_000)
        sync(phone, SyncRequest(items = listOf(milk)))
        sync(phone, SyncRequest(since = 1, items = listOf(milk.copy(isDeleted = true, updatedAt = 2_000))))
        assertEquals(listOf(true), sync(login(email)).items.map { it.isDeleted })
    }

    @Test
    fun `данные разных пользователей не смешиваются`() = serverTest {
        val id = UUID.randomUUID().toString()
        val first = registerVerified()
        val second = registerVerified()
        sync(first, SyncRequest(items = listOf(item(id, name = "У первого"))))
        sync(second, SyncRequest(items = listOf(item(id, name = "У второго"))))
        assertEquals(listOf("У первого"), sync(first).items.map { it.name })
        assertEquals(listOf("У второго"), sync(second).items.map { it.name })
    }

    @Test
    fun `номер больше серверного приводит к полной синхронизации`() = serverTest {
        val token = registerVerified()
        val milk = item()
        sync(token, SyncRequest(items = listOf(milk)))
        assertEquals(listOf(milk), sync(token, SyncRequest(since = 999)).items)
    }

    @Test
    fun `в ответе есть все поля, даже со значениями по умолчанию`() = serverTest {
        val token = registerVerified()
        val response = sendSync(token, json.encodeToString(SyncRequest(items = listOf(item()))))
        assertTrue("\"isDeleted\":false" in response.bodyAsText())
    }

    @Test
    fun `без подтверждённой почты синхронизация недоступна`() = serverTest {
        val response = sendSync(register(uniqueEmail()), "{}")
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("email_not_verified", response.field("error"))
    }

    @Test
    fun `слишком большой пакет изменений отклоняется`() = serverTest {
        val token = registerVerified()
        val response = sendSync(token, json.encodeToString(SyncRequest(items = List(1001) { item() })))
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("too_many_changes", response.field("error"))
    }

    @Test
    fun `неверная дата возвращает ошибку в едином формате`() = serverTest {
        val token = registerVerified()
        val body = json.encodeToString(SyncRequest(items = listOf(item()))).replace("2026-11-01", "1 ноября")
        val response = sendSync(token, body)
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("bad_request", response.field("error"))
    }
}
