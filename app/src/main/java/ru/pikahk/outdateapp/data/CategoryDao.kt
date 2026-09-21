package ru.pikahk.outdateapp.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY name")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun findById(id: String): Category?

    @Upsert
    suspend fun upsert(category: Category)

    @Upsert
    suspend fun upsertAll(categories: List<Category>)

    @Query("UPDATE categories SET isDeleted = 1, isPendingSync = 1, updatedAt = :now WHERE id = :id")
    suspend fun markDeleted(id: String, now: Long)
}
