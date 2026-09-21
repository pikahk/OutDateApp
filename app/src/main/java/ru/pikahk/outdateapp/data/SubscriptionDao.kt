package ru.pikahk.outdateapp.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions WHERE isDeleted = 0")
    fun observeAll(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE isDeleted = 0 AND isCancelled = 0")
    fun observeActive(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun findById(id: String): Subscription?

    @Upsert
    suspend fun upsert(subscription: Subscription)

    @Upsert
    suspend fun upsertAll(subscriptions: List<Subscription>)

    @Query("UPDATE subscriptions SET isDeleted = 1, isPendingSync = 1, updatedAt = :now WHERE id = :id")
    suspend fun markDeleted(id: String, now: Long)

    @Query("UPDATE subscriptions SET isCancelled = 1, isPendingSync = 1, updatedAt = :now WHERE id = :id")
    suspend fun markCancelled(id: String, now: Long)
}
