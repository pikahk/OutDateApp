package ru.pikahk.outdateapp.data

import kotlinx.coroutines.flow.Flow

class ItemRepository(private val dao: ItemDao) {

    fun observeAll(): Flow<List<Item>> = dao.observeAll()

    suspend fun save(item: Item) {
        dao.upsert(item.copy(updatedAt = System.currentTimeMillis(), isPendingSync = true))
    }

    suspend fun markDeleted(id: String) {
        dao.markDeleted(id, System.currentTimeMillis())
    }
}
