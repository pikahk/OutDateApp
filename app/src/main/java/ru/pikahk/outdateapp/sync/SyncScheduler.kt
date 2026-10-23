package ru.pikahk.outdateapp.sync

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import ru.pikahk.outdateapp.data.SyncProvider
import ru.pikahk.outdateapp.data.model.Session

const val SYNC_DELAY_MS = 2_000L

object SyncScheduler {

    private val started = AtomicBoolean(false)

    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        val sync = SyncProvider.get(appContext)
        SyncWorker.schedulePeriodic(appContext)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            syncRequests(sync.session, sync.pendingCount).collect { SyncWorker.request(appContext) }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
fun syncRequests(session: Flow<Session?>, pendingCount: Flow<Int>): Flow<Unit> = session
    .map { current -> current?.takeIf { it.emailVerified }?.token }
    .distinctUntilChanged()
    .flatMapLatest { token ->
        if (token == null) {
            emptyFlow()
        } else {
            pendingCount.filter { it > 0 }.map { }.onStart { emit(Unit) }
        }
    }
    .debounce(SYNC_DELAY_MS)
