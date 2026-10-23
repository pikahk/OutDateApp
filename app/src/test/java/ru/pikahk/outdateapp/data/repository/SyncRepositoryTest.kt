package ru.pikahk.outdateapp.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.pikahk.outdateapp.data.dao.SyncDao
import ru.pikahk.outdateapp.data.model.Category
import ru.pikahk.outdateapp.data.model.Family
import ru.pikahk.outdateapp.data.model.FamilyMember
import ru.pikahk.outdateapp.data.model.Item
import ru.pikahk.outdateapp.data.model.Session
import ru.pikahk.outdateapp.data.remote.ApiException
import ru.pikahk.outdateapp.data.remote.CategoryDto
import ru.pikahk.outdateapp.data.remote.FamilyStateResponse
import ru.pikahk.outdateapp.data.remote.FamilySyncRequest
import ru.pikahk.outdateapp.data.remote.FamilySyncResponse
import ru.pikahk.outdateapp.data.remote.OutDateApi
import ru.pikahk.outdateapp.data.remote.ProductDto
import ru.pikahk.outdateapp.data.remote.SyncRequest
import ru.pikahk.outdateapp.data.remote.SyncResponse
import ru.pikahk.outdateapp.data.remote.toDto
import ru.pikahk.outdateapp.ui.auth.FakeSessionStore

class SyncRepositoryTest {

    private val json = Json { explicitNulls = false }

    private val signedIn = Session("token", "misha@mail.ru", emailVerified = true)

    private val requests = mutableListOf<SyncRequest>()

    private val paths = mutableListOf<String>()

    private fun item(id: String, updatedAt: Long, pending: Boolean = true, familyId: String? = null) = Item(
        id = id,
        name = "Молоко",
        categoryId = null,
        barcode = null,
        expiresAt = LocalDate.of(2026, 11, 1),
        daysAfterOpening = null,
        openedAt = null,
        updatedAt = updatedAt,
        isPendingSync = pending,
        familyId = familyId
    )

    private fun echo(version: Long, request: SyncRequest) = SyncResponse(
        version = version,
        categories = request.categories,
        items = request.items,
        subscriptions = request.subscriptions,
        products = request.products
    )

    private val familyRequests = mutableListOf<FamilySyncRequest>()

    private val home = Family(
        id = "f1",
        name = "Квартира",
        isOwner = true,
        members = listOf(FamilyMember("u1", "Миша", isOwner = true, isMe = true, joinedAt = 0))
    )

    private fun repository(
        store: SessionStore,
        dao: SyncDao,
        family: Family? = null,
        familyServer: suspend MockRequestHandleScope.(FamilySyncRequest) -> HttpResponseData = {
            reply(FamilySyncResponse(it.familyId, 1, it.items))
        },
        server: suspend MockRequestHandleScope.(SyncRequest) -> HttpResponseData
    ) = SyncRepository(
        OutDateApi(
            "https://api.test/",
            MockEngine { request ->
                paths += request.url.encodedPath
                when (request.url.encodedPath) {
                    "/sync" -> {
                        val body = json.decodeFromString<SyncRequest>(request.body.toByteArray().decodeToString())
                        requests += body
                        server(body)
                    }
                    "/family" -> {
                        val body = json.encodeToString(FamilyStateResponse(family))
                        respond(body, HttpStatusCode.OK, jsonHeaders)
                    }
                    "/family/sync" -> {
                        val body = json.decodeFromString<FamilySyncRequest>(request.body.toByteArray().decodeToString())
                        familyRequests += body
                        familyServer(body)
                    }
                    else -> respond("", HttpStatusCode.NoContent)
                }
            }
        ),
        store,
        dao
    )

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun MockRequestHandleScope.reply(response: SyncResponse): HttpResponseData =
        respond(json.encodeToString(response), HttpStatusCode.OK, jsonHeaders)

    private fun MockRequestHandleScope.reply(response: FamilySyncResponse): HttpResponseData =
        respond(json.encodeToString(response), HttpStatusCode.OK, jsonHeaders)

    @Test
    fun `без подтверждённой почты синхронизация не идёт`() = runBlocking {
        val dao = FakeSyncDao()
        dao.upsertItems(listOf(item("a", 100)))
        repository(FakeSessionStore(signedIn.copy(emailVerified = false)), dao) { reply(echo(1, it)) }.sync()
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `изменения с устройства уходят на сервер и больше не ждут отправки`() = runBlocking {
        val store = FakeSessionStore(signedIn)
        val dao = FakeSyncDao()
        dao.upsertItems(listOf(item("a", 100)))
        repository(store, dao) { reply(echo(1, it)) }.sync()
        assertEquals(0L, requests.single().since)
        assertEquals(listOf("a"), requests.single().items.map { it.id })
        assertFalse(dao.items.value.getValue("a").isPendingSync)
        assertEquals(1L, store.cursor.value)
        assertEquals(0, dao.observePendingCount().first())
    }

    @Test
    fun `данные с сервера сохраняются на устройстве`() = runBlocking {
        val store = FakeSessionStore(signedIn)
        store.cursor.value = 5
        val dao = FakeSyncDao()
        val response = SyncResponse(
            version = 6,
            categories = listOf(
                CategoryDto(id = "home", name = "Бытовая химия", isDefault = false, isDeleted = false, updatedAt = 10)
            ),
            items = listOf(item("b", 50, pending = false).toDto()),
            subscriptions = emptyList(),
            products = listOf(ProductDto("4600000000000", "Молоко", null, 7, null, 10))
        )
        repository(store, dao) { reply(response) }.sync()
        assertEquals(5L, requests.single().since)
        assertEquals("Бытовая химия", dao.categories.value.getValue("home").name)
        assertFalse(dao.items.value.getValue("b").isPendingSync)
        assertEquals(7, dao.products.value.getValue("4600000000000").defaultDaysAfterOpening)
        assertEquals(6L, store.cursor.value)
    }

    @Test
    fun `более новая правка на устройстве не затирается старой с сервера`() = runBlocking {
        val dao = FakeSyncDao()
        val server = SyncResponse(
            version = 1,
            categories = emptyList(),
            items = listOf(item("a", 100).toDto()),
            subscriptions = emptyList(),
            products = emptyList()
        )
        val repository = repository(FakeSessionStore(signedIn), dao) {
            dao.upsertItems(listOf(item("a", 200)))
            reply(server)
        }
        repository.sync()
        val local = dao.items.value.getValue("a")
        assertEquals(200L, local.updatedAt)
        assertTrue(local.isPendingSync)
    }

    @Test
    fun `больше тысячи изменений уходят несколькими запросами`() = runBlocking {
        val dao = FakeSyncDao()
        dao.upsertItems((1..SYNC_BATCH_SIZE + 1).map { item("i$it", it.toLong()) })
        var version = 0L
        repository(FakeSessionStore(signedIn), dao) { reply(echo(++version, it)) }.sync()
        assertEquals(listOf(SYNC_BATCH_SIZE, 1), requests.map { it.items.size })
        assertEquals(listOf(0L, 1L), requests.map { it.since })
        assertEquals(0, dao.observePendingCount().first())
    }

    @Test
    fun `если сервер не узнал вход, вход сбрасывается`() = runBlocking {
        val store = FakeSessionStore(signedIn)
        val repository = repository(store, FakeSyncDao()) {
            respond("""{"error":"unauthorized"}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }
        val error = runCatching { repository.sync() }.exceptionOrNull()
        assertEquals(ApiException.UNAUTHORIZED, (error as ApiException).code)
        assertNull(store.current.value)
    }

    @Test
    fun `выход удаляет данные аккаунта с устройства`() = runBlocking {
        val store = FakeSessionStore(signedIn)
        store.cursor.value = 7
        val dao = FakeSyncDao()
        dao.upsertCategories(
            listOf(
                Category(id = "food", name = "food", isDefault = true, isPendingSync = false),
                Category(id = "home", name = "Бытовая химия", isDefault = false)
            )
        )
        dao.upsertItems(listOf(item("a", 100)))
        repository(store, dao) { reply(echo(1, it)) }.signOut()
        assertEquals(setOf("food"), dao.categories.value.keys)
        assertTrue(dao.items.value.isEmpty())
        assertNull(store.current.value)
        assertEquals(0L, store.cursor.value)
        assertEquals(listOf("/auth/logout"), paths)
    }

    @Test
    fun `без группы общая синхронизация не идёт`() = runBlocking {
        repository(FakeSessionStore(signedIn), FakeSyncDao()) { reply(echo(1, it)) }.sync()
        assertEquals(listOf("/sync", "/family"), paths)
        assertTrue(familyRequests.isEmpty())
    }

    @Test
    fun `общий список приходит с сервера с именем добавившего`() = runBlocking {
        val store = FakeSessionStore(signedIn)
        val dao = FakeSyncDao()
        val cheese = item("c", 100, pending = false).toDto().copy(addedBy = "Аня")
        repository(store, dao, family = home, familyServer = { reply(FamilySyncResponse("f1", 4, listOf(cheese))) }) {
            reply(echo(1, it))
        }.sync()
        val saved = dao.items.value.getValue("c")
        assertEquals("f1", saved.familyId)
        assertEquals("Аня", saved.addedBy)
        assertFalse(saved.isPendingSync)
        assertEquals(home, store.currentFamily.value)
        assertEquals(4L, store.familyCursor.value)
    }

    @Test
    fun `общие продукты уходят только в общую синхронизацию`() = runBlocking {
        val store = FakeSessionStore(signedIn)
        store.saveFamily(home)
        store.saveFamilyVersion(3)
        val dao = FakeSyncDao()
        dao.upsertItems(listOf(item("mine", 100), item("shared", 100, familyId = "f1")))
        repository(store, dao, family = home) { reply(echo(1, it)) }.sync()
        assertEquals(listOf("mine"), requests.single().items.map { it.id })
        assertEquals(listOf("shared"), familyRequests.single().items.map { it.id })
        assertEquals(3L, familyRequests.single().since)
        assertEquals("f1", familyRequests.single().familyId)
        assertEquals(0, dao.observePendingCount().first())
        assertEquals("f1", dao.items.value.getValue("shared").familyId)
    }

    @Test
    fun `после выхода из группы общие продукты пропадают с устройства`() = runBlocking {
        val store = FakeSessionStore(signedIn)
        store.saveFamily(home)
        val dao = FakeSyncDao()
        dao.upsertItems(
            listOf(item("mine", 100, pending = false), item("shared", 100, pending = false, familyId = "f1"))
        )
        repository(store, dao, family = null) { reply(echo(1, it)) }.sync()
        assertEquals(setOf("mine"), dao.items.value.keys)
        assertNull(store.currentFamily.value)
    }

    @Test
    fun `если участника удалили во время синхронизации, общий список очищается`() = runBlocking {
        val store = FakeSessionStore(signedIn)
        val dao = FakeSyncDao()
        dao.upsertItems(listOf(item("shared", 100, familyId = "f1")))
        val repository = repository(
            store,
            dao,
            family = home,
            familyServer = { respond("""{"error":"no_family"}""", HttpStatusCode.NotFound, jsonHeaders) }
        ) { reply(echo(1, it)) }
        repository.sync()
        assertTrue(dao.items.value.isEmpty())
        assertNull(store.currentFamily.value)
    }
}
