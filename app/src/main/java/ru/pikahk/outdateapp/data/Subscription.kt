package ru.pikahk.outdateapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val name: String,

    val priceMinor: Long,

    val currency: String = "RUB",

    val startedAt: LocalDate,

    val periodType: PeriodType,

    val periodDays: Int?,

    val notifyDaysBefore: Int = 3,

    val isCancelled: Boolean = false,

    val updatedAt: Long = System.currentTimeMillis(),

    val isDeleted: Boolean = false,

    val isPendingSync: Boolean = true

)

enum class PeriodType { MONTHLY, YEARLY, CUSTOM_DAYS }
