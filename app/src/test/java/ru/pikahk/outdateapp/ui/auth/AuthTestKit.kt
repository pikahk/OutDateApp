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
    override val session = current

    override suspend fun save(session: Session?) {
        current.value = session
    }
}

internal fun authRepository(
    dispatcher: CoroutineDispatcher,
    store: SessionStore = FakeSessionStore(),
    handler: MockRequestHandler
): AuthRepository {
    val engine = MockEngine(
        MockEngineConfig().apply {
            this.dispatcher = dispatcher
            addHandler(handler)
        }
    )
    return AuthRepository(OutDateApi("https://api.test/", engine), store)
}

internal fun io.ktor.client.engine.mock.MockRequestHandleScope.json(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK
): HttpResponseData = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
