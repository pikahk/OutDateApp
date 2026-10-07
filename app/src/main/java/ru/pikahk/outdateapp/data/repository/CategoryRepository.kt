package ru.pikahk.outdateapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pikahk.outdateapp.data.dao.CategoryDao
import ru.pikahk.outdateapp.data.model.Category
import ru.pikahk.outdateapp.data.model.DefaultCategory

class CategoryRepository(private val dao: CategoryDao) {

    fun observeAll(): Flow<List<Category>> = dao.observeAll().map { list ->
        val (defaults, custom) = list.partition { it.isDefault }
        defaults.sortedBy { DefaultCategory.of(it.id)?.ordinal } + custom.sortedBy { it.name.lowercase() }
    }

    suspend fun save(category: Category) {
        dao.upsert(category.copy(updatedAt = System.currentTimeMillis(), isPendingSync = true))
    }

    suspend fun delete(id: String) {
        if (DefaultCategory.of(id) != null) return
        dao.delete(id, System.currentTimeMillis())
    }

    suspend fun addDefaults() {
        val defaults = DefaultCategory.entries.map {
            Category(id = it.id, name = it.id, isDefault = true, isPendingSync = false)
        }
        dao.insertIfAbsent(defaults)
    }
}
