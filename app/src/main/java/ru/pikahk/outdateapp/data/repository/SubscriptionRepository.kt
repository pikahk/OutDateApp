package ru.pikahk.outdateapp.data.repository

import kotlinx.coroutines.flow.Flow
import ru.pikahk.outdateapp.data.dao.SubscriptionDao
import ru.pikahk.outdateapp.data.model.Subscription

class SubscriptionRepository(private val dao: SubscriptionDao) {

    fun observeAll(): Flow<List<Subscription>> = dao.observeAll()

    suspend fun find(id: String): Subscription? = dao.findById(id)

    suspend fun save(subscription: Subscription) {
        dao.upsert(subscription.copy(updatedAt = System.currentTimeMillis(), isPendingSync = true))
    }

    suspend fun setCancelled(id: String, cancelled: Boolean) {
        val subscription = dao.findById(id) ?: return
        save(subscription.copy(isCancelled = cancelled))
    }

    suspend fun markDeleted(id: String) {
        dao.markDeleted(id, System.currentTimeMillis())
    }
}
