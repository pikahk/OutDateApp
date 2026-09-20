package ru.pikahk.outdateapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "products")
data class Product (
    @PrimaryKey
    val barcode: String,

    val name: String,

    val categoryId: String?,

    val defaultDaysAfterOpening: Int?,

    val defaultNotifyDaysBefore: Int?,

    val updatedAt: Long = System.currentTimeMillis(),

    val isPendingSync: Boolean = true,
)
