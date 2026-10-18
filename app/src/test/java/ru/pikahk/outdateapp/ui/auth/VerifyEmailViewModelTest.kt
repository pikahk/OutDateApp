package ru.pikahk.outdateapp.ui.auth

import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.pikahk.outdateapp.data.model.Session

@OptIn(ExperimentalCoroutinesApi::class)
class VerifyEmailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val store = FakeSessionStore(Session("t", "misha@mail.ru", emailVerified = false))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `шестая цифра сразу отправляет код`() = runTest(dispatcher) {
        var sent = ""
        val auth = authRepository(dispatcher, store) { request ->
            sent = request.body.toByteArray().decodeToString()
            respond("", HttpStatusCode.NoContent)
        }
        val viewModel = VerifyEmailViewModel(auth, codeSent = true)
        runCurrent()
        assertEquals("misha@mail.ru", viewModel.email)
        viewModel.updateCode("12 34 5")
        runCurrent()
        assertNull(viewModel.outcome)
        viewModel.updateCode("123456")
        runCurrent()
        assertEquals(Unit, viewModel.outcome)
        assertEquals("""{"code":"123456"}""", sent)
        assertEquals(true, store.current.value?.emailVerified)
    }

    @Test
    fun `новый код можно запросить только через минуту`() = runTest(dispatcher) {
        var requests = 0
        val auth = authRepository(dispatcher, store) {
            requests++
            respond("", HttpStatusCode.NoContent)
        }
        val viewModel = VerifyEmailViewModel(auth, codeSent = true)
        runCurrent()
        assertEquals(60, viewModel.resendIn)
        viewModel.resend()
        runCurrent()
        assertEquals(0, requests)
        advanceTimeBy(60_001)
        assertEquals(0, viewModel.resendIn)
        viewModel.resend()
        runCurrent()
        assertEquals(1, requests)
        assertEquals(60, viewModel.resendIn)
    }

    @Test
    fun `после входа новый код можно запросить сразу`() = runTest(dispatcher) {
        var requests = 0
        val auth = authRepository(dispatcher, store) {
            requests++
            respond("", HttpStatusCode.NoContent)
        }
        val viewModel = VerifyEmailViewModel(auth, codeSent = false)
        runCurrent()
        assertEquals(0, viewModel.resendIn)
        viewModel.resend()
        runCurrent()
        assertEquals(1, requests)
    }

    @Test
    fun `без сети ошибка показывается над кнопкой`() = runTest(dispatcher) {
        val auth = authRepository(dispatcher, store) { throw IOException("offline") }
        val viewModel = VerifyEmailViewModel(auth, codeSent = true)
        viewModel.updateCode("123456")
        runCurrent()
        assertEquals(AuthError.NETWORK, viewModel.error.on(AuthField.FORM))
    }
}
