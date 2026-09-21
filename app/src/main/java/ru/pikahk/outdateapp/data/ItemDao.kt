package ru.pikahk.outdateapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items WHERE isDeleted = 0")
    fun observeAll(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE isDeleted = 0 AND categoryId = :categoryId")
    fun observeByCategory(categoryId: String): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun findById(id: String): Item?

    @Upsert
    suspend fun upsert(item: Item)

    @Upsert
    suspend fun upsertAll(items: List<Item>)

    @Query("UPDATE items SET isDeleted = 1, isPendingSync = 1, updatedAt = :now WHERE id = :id")
    suspend fun markDeleted(id: String, now: Long)

    @Delete
    suspend fun deleteForever(item: Item)
}
