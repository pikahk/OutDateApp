package ru.pikahk.outdateapp.ui.auth

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

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
    fun `почта неправильного вида не отправляется на сервер`() = runTest(dispatcher) {
        var requests = 0
        val viewModel = SignInViewModel(
            authRepository(dispatcher) {
                requests++
                json("{}")
            },
            email = "misha"
        )
        viewModel.updatePassword("password123")
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(AuthError.INVALID_EMAIL, viewModel.error)
        assertEquals(0, requests)
    }

    @Test
    fun `неверный пароль показывается под полем пароля`() = runTest(dispatcher) {
        val auth =
            authRepository(dispatcher) { json("""{"error":"invalid_credentials"}""", HttpStatusCode.Unauthorized) }
        val viewModel = SignInViewModel(auth, email = "misha@mail.ru")
        viewModel.updatePassword("wrong-password")
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(AuthError.INVALID_CREDENTIALS, viewModel.error.on(AuthField.PASSWORD))
        assertNull(viewModel.outcome)
        assertFalse(viewModel.loading)
    }

    @Test
    fun `после входа известно, нужно ли подтверждать почту`() = runTest(dispatcher) {
        val auth = authRepository(dispatcher) { request ->
            when (request.url.encodedPath) {
                "/auth/login" -> json("""{"token":"t","email":"misha@mail.ru"}""")
                else -> json("""{"email":"misha@mail.ru","emailVerified":false}""")
            }
        }
        val viewModel = SignInViewModel(auth, email = " misha@mail.ru ")
        viewModel.updatePassword("password123")
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(false, viewModel.outcome?.emailVerified)
    }
}
