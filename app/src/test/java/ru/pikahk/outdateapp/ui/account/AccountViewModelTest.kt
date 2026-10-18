package ru.pikahk.outdateapp.ui.account

import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import java.time.LocalDate
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.pikahk.outdateapp.data.model.Item
import ru.pikahk.outdateapp.data.model.Session
import ru.pikahk.outdateapp.data.repository.AuthRepository
import ru.pikahk.outdateapp.data.repository.FakeSyncDao
import ru.pikahk.outdateapp.data.repository.SyncRepository
import ru.pikahk.outdateapp.ui.auth.FakeSessionStore
import ru.pikahk.outdateapp.ui.auth.mockApi

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val noContent: MockRequestHandler = { respond("", HttpStatusCode.NoContent) }

    private val milk = Item(
        name = "Молоко",
        categoryId = null,
        barcode = null,
        expiresAt = LocalDate.of(2026, 11, 1),
        daysAfterOpening = null,
        openedAt = null
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(store: FakeSessionStore, dao: FakeSyncDao = FakeSyncDao()): AccountViewModel {
        val api = mockApi(dispatcher, noContent)
        return AccountViewModel(AuthRepository(api, store), SyncRepository(api, store, dao))
    }

    @Test
    fun `профиль показывает почту только подтверждённого аккаунта`() = runTest(dispatcher) {
        val store = FakeSessionStore(Session("t", "misha@mail.ru", emailVerified = false))
        val viewModel = viewModel(store)
        assertNull(viewModel.email.first { true })
        store.current.value = Session("t", "misha@mail.ru", emailVerified = true)
        assertEquals("misha@mail.ru", viewModel.email.first { it != null })
    }

    @Test
    fun `перед выходом видно, сколько изменений не отправлено`() = runTest(dispatcher) {
        val dao = FakeSyncDao()
        dao.upsertItems(listOf(milk, milk.copy(id = "second")))
        val viewModel = viewModel(FakeSessionStore(Session("t", "misha@mail.ru", emailVerified = true)), dao)
        assertEquals(2, viewModel.pendingChanges.first { it > 0 })
    }

    @Test
    fun `после выхода профиль снова без входа, а данные удалены`() = runTest(dispatcher) {
        val store = FakeSessionStore(Session("t", "misha@mail.ru", emailVerified = true))
        val dao = FakeSyncDao()
        dao.upsertItems(listOf(milk))
        val viewModel = viewModel(store, dao)
        viewModel.logout()
        advanceUntilIdle()
        assertNull(store.current.value)
        assertTrue(dao.items.value.isEmpty())
        assertNull(viewModel.email.first { it == null })
    }
}
