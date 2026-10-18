package ru.pikahk.outdateapp.ui.account

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.pikahk.outdateapp.data.model.Session
import ru.pikahk.outdateapp.ui.auth.FakeSessionStore
import ru.pikahk.outdateapp.ui.auth.authRepository

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `профиль показывает почту только подтверждённого аккаунта`() = runTest(dispatcher) {
        val store = FakeSessionStore(Session("t", "misha@mail.ru", emailVerified = false))
        val viewModel = AccountViewModel(authRepository(dispatcher, store) { respond("", HttpStatusCode.NoContent) })
        assertNull(viewModel.email.first { true })
        store.current.value = Session("t", "misha@mail.ru", emailVerified = true)
        assertEquals("misha@mail.ru", viewModel.email.first { it != null })
    }

    @Test
    fun `после выхода профиль снова без входа`() = runTest(dispatcher) {
        val store = FakeSessionStore(Session("t", "misha@mail.ru", emailVerified = true))
        val viewModel = AccountViewModel(authRepository(dispatcher, store) { respond("", HttpStatusCode.NoContent) })
        viewModel.logout()
        advanceUntilIdle()
        assertNull(store.current.value)
        assertNull(viewModel.email.first { it == null })
    }
}
