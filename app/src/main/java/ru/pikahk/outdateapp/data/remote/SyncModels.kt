package ru.pikahk.outdateapp.data.remote

import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.pikahk.outdateapp.data.model.PeriodType

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): LocalDate = try {
        LocalDate.parse(decoder.decodeString())
    } catch (e: DateTimeParseException) {
        throw SerializationException(e.message)
    }
}

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val isDeleted: Boolean,
    val updatedAt: Long
)

@Serializable
data class ItemDto(
    val id: String,
    val name: String,
    val categoryId: String?,
    val barcode: String?,
    @Serializable(with = LocalDateSerializer::class)
    val expiresAt: LocalDate,
    val daysAfterOpening: Int?,
    @Serializable(with = LocalDateSerializer::class)
    val openedAt: LocalDate?,
    val notifyDaysBefore: Int,
    val createdAt: Long,
    val isDeleted: Boolean,
    val updatedAt: Long,
    val addedBy: String? = null
)

@Serializable
data class SubscriptionDto(
    val id: String,
    val name: String,
    val priceMinor: Long,
    val currency: String,
    @Serializable(with = LocalDateSerializer::class)
    val startedAt: LocalDate,
    val periodType: PeriodType,
    val periodDays: Int?,
    val notifyDaysBefore: Int,
    val isCancelled: Boolean,
    val isDeleted: Boolean,
    val updatedAt: Long
)

@Serializable
data class ProductDto(
    val barcode: String,
    val name: String,
    val categoryId: String?,
    val defaultDaysAfterOpening: Int?,
    val defaultNotifyDaysBefore: Int?,
    val updatedAt: Long
)

@Serializable
data class SyncRequest(
    val since: Long,
    val categories: List<CategoryDto>,
    val items: List<ItemDto>,
    val subscriptions: List<SubscriptionDto>,
    val products: List<ProductDto>
)

@Serializable
data class SyncResponse(
    val version: Long,
    val categories: List<CategoryDto>,
    val items: List<ItemDto>,
    val subscriptions: List<SubscriptionDto>,
    val products: List<ProductDto>
)
