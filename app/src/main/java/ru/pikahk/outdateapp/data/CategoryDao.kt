package ru.pikahk.outdateapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(categories: List<Category>)

    @Query("UPDATE categories SET isDeleted = 1, isPendingSync = 1, updatedAt = :now WHERE id = :id")
    suspend fun markDeleted(id: String, now: Long)

    @Query("UPDATE items SET categoryId = NULL, isPendingSync = 1, updatedAt = :now WHERE categoryId = :id")
    suspend fun detachItems(id: String, now: Long)

    @Query("UPDATE products SET categoryId = NULL, isPendingSync = 1, updatedAt = :now WHERE categoryId = :id")
    suspend fun detachProducts(id: String, now: Long)

    @Transaction
    suspend fun delete(id: String, now: Long) {
        detachItems(id, now)
        detachProducts(id, now)
        markDeleted(id, now)
    }
}
