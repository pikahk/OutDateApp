package ru.pikahk.outdateapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.pikahk.outdateapp.data.dao.SyncDao
import ru.pikahk.outdateapp.data.model.Family
import ru.pikahk.outdateapp.data.model.Session
import ru.pikahk.outdateapp.data.remote.ApiException
import ru.pikahk.outdateapp.data.remote.FamilySyncRequest
import ru.pikahk.outdateapp.data.remote.OutDateApi
import ru.pikahk.outdateapp.data.remote.SyncRequest
import ru.pikahk.outdateapp.data.remote.toDto
import ru.pikahk.outdateapp.data.remote.toModel

const val SYNC_BATCH_SIZE = 1000
private const val MAX_ROUNDS = 50

class SyncRepository(private val api: OutDateApi, private val store: SessionStore, private val dao: SyncDao) {

    private val mutex = Mutex()

    val session: Flow<Session?> = store.session

    val pendingCount: Flow<Int> = dao.observePendingCount()

    val family: Flow<Family?> = store.family

    suspend fun sync() {
        mutex.withLock {
            val session = store.session.first()?.takeIf { it.emailVerified } ?: return
            syncAccount(session)
            syncFamily(session)
        }
    }

    suspend fun updateFamily(family: Family?) {
        mutex.withLock { applyFamily(family) }
    }

    suspend fun signOut() {
        val session = mutex.withLock {
            val session = store.session.first() ?: return
            store.save(null)
            dao.clearAccountData()
            session
        }
        runCatching { api.logout(session.token) }
    }

    private suspend fun syncAccount(session: Session) {
        var since = store.syncVersion.first()
        repeat(MAX_ROUNDS) {
            val request = pendingChanges(since)
            val response = send { api.sync(session.token, request) }
            dao.saveFromServer(
                categories = response.categories.map { it.toModel() },
                items = response.items.map { it.toModel() },
                subscriptions = response.subscriptions.map { it.toModel() },
                products = response.products.map { it.toModel() }
            )
            store.saveSyncVersion(response.version)
            since = response.version
            if (!request.hasMore()) return
        }
    }

    private suspend fun syncFamily(session: Session) {
        val family = send { api.family(session.token) }
        applyFamily(family)
        if (family == null) return
        var since = store.familyVersion.first()
        repeat(MAX_ROUNDS) {
            val pending = dao.pendingFamilyItems(family.id, SYNC_BATCH_SIZE)
            val request = FamilySyncRequest(family.id, since, pending.map { it.toDto() })
            val response = try {
                send { api.familySync(session.token, request) }
            } catch (e: ApiException) {
                if (e.code != ApiException.NO_FAMILY) throw e
                applyFamily(null)
                return
            }
            if (response.familyId != family.id) return
            dao.saveFamilyFromServer(family.id, response.items.map { it.toModel(family.id) })
            store.saveFamilyVersion(response.version)
            since = response.version
            if (pending.size < SYNC_BATCH_SIZE) return
        }
    }

    private suspend fun applyFamily(family: Family?) {
        if (store.family.first()?.id != family?.id) dao.deleteFamilyItems()
        store.saveFamily(family)
    }

    private suspend fun pendingChanges(since: Long) = SyncRequest(
        since = since,
        categories = dao.pendingCategories(SYNC_BATCH_SIZE).map { it.toDto() },
        items = dao.pendingItems(SYNC_BATCH_SIZE).map { it.toDto() },
        subscriptions = dao.pendingSubscriptions(SYNC_BATCH_SIZE).map { it.toDto() },
        products = dao.pendingProducts(SYNC_BATCH_SIZE).map { it.toDto() }
    )

    private suspend fun <T> send(call: suspend () -> T): T = try {
        call()
    } catch (e: ApiException) {
        if (e.code == ApiException.UNAUTHORIZED) store.save(null)
        throw e
    }

    private fun SyncRequest.hasMore() =
        listOf(categories, items, subscriptions, products).any { it.size == SYNC_BATCH_SIZE }
}
