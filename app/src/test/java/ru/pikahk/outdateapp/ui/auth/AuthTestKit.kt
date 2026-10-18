package ru.pikahk.outdateapp.ui.auth

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import ru.pikahk.outdateapp.data.model.Session
import ru.pikahk.outdateapp.data.remote.OutDateApi
import ru.pikahk.outdateapp.data.repository.AuthRepository
import ru.pikahk.outdateapp.data.repository.SessionStore

internal class FakeSessionStore(initial: Session? = null) : SessionStore {
    val current = MutableStateFlow(initial)
    val cursor = MutableStateFlow(0L)
    override val session = current
    override val syncVersion = cursor

    override suspend fun save(session: Session?) {
        if (session?.token != current.value?.token) cursor.value = 0
        current.value = session
    }

    override suspend fun saveSyncVersion(version: Long) {
        cursor.value = version
    }
}

internal fun mockApi(dispatcher: CoroutineDispatcher, handler: MockRequestHandler): OutDateApi {
    val engine = MockEngine(
        MockEngineConfig().apply {
            this.dispatcher = dispatcher
            addHandler(handler)
        }
    )
    return OutDateApi("https://api.test/", engine)
}

internal fun authRepository(
    dispatcher: CoroutineDispatcher,
    store: SessionStore = FakeSessionStore(),
    handler: MockRequestHandler
): AuthRepository = AuthRepository(mockApi(dispatcher, handler), store)

internal fun io.ktor.client.engine.mock.MockRequestHandleScope.json(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK
): HttpResponseData = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
