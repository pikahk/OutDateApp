package ru.pikahk.outdateapp.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.pikahk.outdateapp.data.model.Session

@OptIn(ExperimentalCoroutinesApi::class)
class SyncRequestsTest {

    private val session = MutableStateFlow<Session?>(null)

    private val pending = MutableStateFlow(0)

    private var requests = 0

    private fun TestScope.collect() {
        backgroundScope.launch { syncRequests(session, pending).collect { requests++ } }
    }

    private fun TestScope.wait() = advanceTimeBy(SYNC_DELAY_MS * 5)

    @Test
    fun `без входа синхронизация не запускается`() = runTest {
        collect()
        pending.value = 3
        wait()
        session.value = Session("t", "misha@mail.ru", emailVerified = false)
        wait()
        assertEquals(0, requests)
    }

    @Test
    fun `после входа синхронизация запускается сразу`() = runTest {
        collect()
        session.value = Session("t", "misha@mail.ru", emailVerified = true)
        wait()
        assertEquals(1, requests)
    }

    @Test
    fun `частые изменения отправляются одним запуском`() = runTest {
        session.value = Session("t", "misha@mail.ru", emailVerified = true)
        collect()
        wait()
        pending.value = 1
        advanceTimeBy(SYNC_DELAY_MS / 4)
        pending.value = 2
        advanceTimeBy(SYNC_DELAY_MS / 4)
        pending.value = 3
        wait()
        pending.value = 0
        wait()
        assertEquals(2, requests)
    }
}
