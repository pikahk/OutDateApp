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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.pikahk.outdateapp.data.model.Family
import ru.pikahk.outdateapp.data.model.Item
import ru.pikahk.outdateapp.data.model.Session
import ru.pikahk.outdateapp.data.remote.ApiException
import ru.pikahk.outdateapp.data.remote.OutDateApi
import ru.pikahk.outdateapp.ui.auth.FakeSessionStore

private typealias FamilyRoute = suspend MockRequestHandleScope.(body: String) -> HttpResponseData

class FamilyRepositoryTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val family = """{"id":"f1","name":"Квартира","isOwner":true,""" +
        """"members":[{"id":"u1","name":"Миша","isOwner":true,"isMe":true,"joinedAt":0}]}"""

    private val store = FakeSessionStore(Session("token", "misha@mail.ru", emailVerified = true))

    private val dao = FakeSyncDao()

    private val calls = mutableListOf<String>()

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, jsonHeaders)

    private fun repository(routes: Map<String, FamilyRoute>): FamilyRepository {
        val api = OutDateApi(
            "https://api.test/",
            MockEngine { request ->
                val key = "${request.method.value} ${request.url.encodedPath}"
                calls += key
                val route = checkNotNull(routes[key]) { key }
                route(request.body.toByteArray().decodeToString())
            }
        )
        return FamilyRepository(api, store, SyncRepository(api, store, dao))
    }

    private fun sharedItem() = Item(
        id = "shared",
        name = "Сыр",
        categoryId = "food",
        barcode = null,
        expiresAt = LocalDate.of(2026, 11, 1),
        daysAfterOpening = null,
        openedAt = null,
        isPendingSync = false,
        familyId = "f1"
    )

    @Test
    fun `созданная группа сохраняется на устройстве`() = runBlocking {
        var sent = ""
        val repository = repository(
            mapOf(
                "POST /family" to { body ->
                    sent = body
                    json(family, HttpStatusCode.Created)
                }
            )
        )
        repository.create("  Квартира ", " Миша ")
        assertEquals("""{"name":"Квартира","memberName":"Миша"}""", sent)
        assertEquals("Квартира", store.currentFamily.value?.name)
    }

    @Test
    fun `код из приглашения отправляется как есть`() = runBlocking {
        var sent = ""
        val repository = repository(
            mapOf(
                "POST /family/join" to { body ->
                    sent = body
                    json(family)
                }
            )
        )
        repository.join("k7p qx4", "Аня")
        assertEquals("""{"code":"k7p qx4","memberName":"Аня"}""", sent)
        assertEquals("f1", store.currentFamily.value?.id)
    }

    @Test
    fun `после выхода общие продукты удаляются`() = runBlocking {
        store.saveFamily(repositoryFamily())
        dao.upsertItems(listOf(sharedItem()))
        repository(mapOf("POST /family/leave" to { respond("", HttpStatusCode.NoContent) })).leave()
        assertNull(store.currentFamily.value)
        assertTrue(dao.items.value.isEmpty())
    }

    @Test
    fun `удаление группы идёт запросом DELETE`() = runBlocking {
        store.saveFamily(repositoryFamily())
        repository(mapOf("DELETE /family" to { respond("", HttpStatusCode.NoContent) })).delete()
        assertEquals(listOf("DELETE /family"), calls)
        assertNull(store.currentFamily.value)
    }

    @Test
    fun `если группы уже нет, она убирается с устройства`() = runBlocking {
        store.saveFamily(repositoryFamily())
        dao.upsertItems(listOf(sharedItem()))
        val repository = repository(
            mapOf("POST /family/invite" to { json("""{"error":"no_family"}""", HttpStatusCode.NotFound) })
        )
        val error = runCatching { repository.invite() }.exceptionOrNull()
        assertEquals(ApiException.NO_FAMILY, (error as ApiException).code)
        assertNull(store.currentFamily.value)
        assertTrue(dao.items.value.isEmpty())
    }

    @Test
    fun `неверный код не меняет состояние`() = runBlocking {
        val repository = repository(
            mapOf("POST /family/join" to { json("""{"error":"invalid_code"}""", HttpStatusCode.BadRequest) })
        )
        val error = runCatching { repository.join("AAAAAA", "Аня") }.exceptionOrNull()
        assertEquals("invalid_code", (error as ApiException).code)
        assertNull(store.currentFamily.value)
    }

    private fun repositoryFamily() = Family("f1", "Квартира", isOwner = true, members = emptyList())
}
