package ru.pikahk.outdateapp.data

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class ItemRepository(private val dao: ItemDao) {

    fun observeAll(): Flow<List<Item>> = dao.observeAll()

    fun observeById(id: String): Flow<Item?> = dao.observeById(id)

    suspend fun save(item: Item) {
        dao.upsert(item.copy(updatedAt = System.currentTimeMillis(), isPendingSync = true))
    }

    suspend fun setOpenedAt(id: String, date: LocalDate?) {
        val item = dao.findById(id) ?: return
        save(item.copy(openedAt = date))
    }

    suspend fun markDeleted(id: String) {
        dao.markDeleted(id, System.currentTimeMillis())
    }
}
