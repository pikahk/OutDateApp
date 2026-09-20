package ru.pikahk.outdateapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class Category (
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val name: String,

    val isDefault: Boolean,

    val updatedAt: Long = System.currentTimeMillis(),

    val isDeleted: Boolean = false,

    val isPendingSync: Boolean = true,
)
