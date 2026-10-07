package ru.pikahk.outdateapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val name: String,

    val isDefault: Boolean,

    val updatedAt: Long = System.currentTimeMillis(),

    val isDeleted: Boolean = false,

    val isPendingSync: Boolean = true
)
enum class DefaultCategory(val id: String) {
    FOOD("food"),
    MEDICINE("medicine"),
    COSMETICS("cosmetics");

    companion object {
        fun of(id: String?): DefaultCategory? = entries.firstOrNull { it.id == id }
    }
}
