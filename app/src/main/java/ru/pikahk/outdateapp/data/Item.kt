package ru.pikahk.outdateapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "items",
    indices = [
        Index("categoryId"),
        Index("barcode"),
        Index("expiresAt"),
    ],
)
data class Item (
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val name: String,

    val categoryId: String?,

    val barcode: String?,

    val expiresAt: LocalDate,

    val daysAfterOpening: Int?,

    val openedAt: LocalDate?,

    val notifyDaysBefore: Int = 3,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis(),

    val isDeleted: Boolean = false,

    val isPendingSync: Boolean = true,
)
