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
        Index("expiresAt")
    ]
)
data class Item(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val name: String,

    val categoryId: String?,

    /** Штрихкод, если расходник добавлен сканированием. */
    val barcode: String?,

    /** Срок годности, указанный на упаковке. */
    val expiresAt: LocalDate,

    /** Сколько дней расходник годен после вскрытия. null — отдельного срока нет. */
    val daysAfterOpening: Int?,

    /** Дата вскрытия. null означает, что расходник запечатан. */
    val openedAt: LocalDate?,

    /** За сколько дней до истечения срока прислать уведомление. */
    val notifyDaysBefore: Int = 3,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis(),

    val isDeleted: Boolean = false,

    val isPendingSync: Boolean = true
)
