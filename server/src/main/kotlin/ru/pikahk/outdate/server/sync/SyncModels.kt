package ru.pikahk.outdate.server.sync

import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): LocalDate = try {
        LocalDate.parse(decoder.decodeString())
    } catch (e: DateTimeParseException) {
        throw SerializationException(e.message)
    }
}

interface SyncRecord {
    val key: String
    val updatedAt: Long
}

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val isDefault: Boolean = false,
    val isDeleted: Boolean = false,
    override val updatedAt: Long
) : SyncRecord {
    override val key get() = id
}

@Serializable
data class ItemDto(
    val id: String,
    val name: String,
    val categoryId: String? = null,
    val barcode: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val expiresAt: LocalDate,
    val daysAfterOpening: Int? = null,
    @Serializable(with = LocalDateSerializer::class)
    val openedAt: LocalDate? = null,
    val notifyDaysBefore: Int,
    val createdAt: Long,
    val isDeleted: Boolean = false,
    override val updatedAt: Long
) : SyncRecord {
    override val key get() = id
}

@Serializable
enum class PeriodType { MONTHLY, YEARLY, CUSTOM_DAYS }

@Serializable
data class SubscriptionDto(
    val id: String,
    val name: String,
    val priceMinor: Long,
    val currency: String,
    @Serializable(with = LocalDateSerializer::class)
    val startedAt: LocalDate,
    val periodType: PeriodType,
    val periodDays: Int? = null,
    val notifyDaysBefore: Int,
    val isCancelled: Boolean = false,
    val isDeleted: Boolean = false,
    override val updatedAt: Long
) : SyncRecord {
    override val key get() = id
}

@Serializable
data class ProductDto(
    val barcode: String,
    val name: String,
    val categoryId: String? = null,
    val defaultDaysAfterOpening: Int? = null,
    val defaultNotifyDaysBefore: Int? = null,
    override val updatedAt: Long
) : SyncRecord {
    override val key get() = barcode
}

@Serializable
data class SyncRequest(
    val since: Long = 0,
    val categories: List<CategoryDto> = emptyList(),
    val items: List<ItemDto> = emptyList(),
    val subscriptions: List<SubscriptionDto> = emptyList(),
    val products: List<ProductDto> = emptyList()
)

@Serializable
data class SyncResponse(
    val version: Long,
    val categories: List<CategoryDto>,
    val items: List<ItemDto>,
    val subscriptions: List<SubscriptionDto>,
    val products: List<ProductDto>
)
