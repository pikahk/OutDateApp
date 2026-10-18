package ru.pikahk.outdateapp.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.pikahk.outdateapp.data.model.Session
import ru.pikahk.outdateapp.data.remote.OutDateApi

private typealias Route = MockRequestHandleScope.() -> HttpResponseData

class AuthRepositoryTest {

    private class FakeSessionStore(initial: Session? = null) : SessionStore {
        val current = MutableStateFlow(initial)
        override val session = current

        override suspend fun save(session: Session?) {
            current.value = session
        }
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val signedIn = Session("old-token", "misha@example.com", emailVerified = false)

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ): HttpResponseData = respond(body, status, jsonHeaders)

    private fun repository(store: SessionStore, routes: Map<String, Route>) = AuthRepository(
        OutDateApi(
            "https://api.test/",
            MockEngine { request -> checkNotNull(routes[request.url.encodedPath]) { request.url }.invoke(this) }
        ),
        store
    )

    @Test
    fun `после регистрации сохраняется вход с неподтверждённой почтой`() = runBlocking {
        val store = FakeSessionStore()
        val auth = repository(
            store,
            mapOf(
                "/auth/register" to { json("""{"token":"t1","email":"misha@example.com"}""", HttpStatusCode.Created) }
            )
        )
        auth.register("Misha@Example.com", "password123")
        assertEquals(Session("t1", "misha@example.com", emailVerified = false), store.current.value)
    }

    @Test
    fun `при входе приложение узнаёт, подтверждена ли почта`() = runBlocking {
        val store = FakeSessionStore()
        val auth = repository(
            store,
            mapOf(
                "/auth/login" to { json("""{"token":"t2","email":"misha@example.com"}""") },
                "/me" to { json("""{"email":"misha@example.com","emailVerified":true}""") }
            )
        )
        auth.login("misha@example.com", "password123")
        assertEquals(Session("t2", "misha@example.com", emailVerified = true), store.current.value)
    }

    @Test
    fun `после ввода кода почта отмечается подтверждённой`() = runBlocking {
        val store = FakeSessionStore(signedIn)
        val auth = repository(store, mapOf("/auth/email/verify" to { respond("", HttpStatusCode.NoContent) }))
        auth.verifyEmail("123456")
        assertEquals(signedIn.copy(emailVerified = true), store.current.value)
    }

    @Test
    fun `после сброса пароля сохраняется новый вход`() = runBlocking {
        val store = FakeSessionStore(signedIn)
        val auth = repository(
            store,
            mapOf("/auth/password/reset" to { json("""{"token":"t3","email":"misha@example.com"}""") })
        )
        auth.resetPassword("misha@example.com", "123456", "new-password")
        assertEquals(Session("t3", "misha@example.com", emailVerified = true), store.current.value)
    }

    @Test
    fun `выход работает и без сети`() = runBlocking {
        val store = FakeSessionStore(signedIn)
        val auth = repository(store, mapOf("/auth/logout" to { throw IOException("offline") }))
        auth.logout()
        assertNull(store.current.value)
    }

    @Test
    fun `если сервер больше не принимает токен, вход сбрасывается`() = runBlocking {
        val store = FakeSessionStore(signedIn)
        val auth = repository(
            store,
            mapOf("/me" to { json("""{"error":"unauthorized"}""", HttpStatusCode.Unauthorized) })
        )
        auth.refresh()
        assertNull(store.current.value)
    }

    @Test
    fun `без сети вход сохраняется`() = runBlocking {
        val store = FakeSessionStore(signedIn)
        val auth = repository(store, mapOf("/me" to { throw IOException("offline") }))
        auth.refresh()
        assertEquals(signedIn, store.current.value)
    }
}
