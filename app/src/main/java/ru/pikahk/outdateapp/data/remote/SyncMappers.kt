package ru.pikahk.outdateapp.data.remote

import ru.pikahk.outdateapp.data.model.Category
import ru.pikahk.outdateapp.data.model.Item
import ru.pikahk.outdateapp.data.model.Product
import ru.pikahk.outdateapp.data.model.Subscription

fun Category.toDto() = CategoryDto(
    id = id,
    name = name,
    isDefault = isDefault,
    isDeleted = isDeleted,
    updatedAt = updatedAt
)

fun CategoryDto.toModel() = Category(
    id = id,
    name = name,
    isDefault = isDefault,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    isPendingSync = false
)

fun Item.toDto() = ItemDto(
    id = id,
    name = name,
    categoryId = categoryId,
    barcode = barcode,
    expiresAt = expiresAt,
    daysAfterOpening = daysAfterOpening,
    openedAt = openedAt,
    notifyDaysBefore = notifyDaysBefore,
    createdAt = createdAt,
    isDeleted = isDeleted,
    updatedAt = updatedAt
)

fun ItemDto.toModel() = Item(
    id = id,
    name = name,
    categoryId = categoryId,
    barcode = barcode,
    expiresAt = expiresAt,
    daysAfterOpening = daysAfterOpening,
    openedAt = openedAt,
    notifyDaysBefore = notifyDaysBefore,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    isPendingSync = false
)

fun Subscription.toDto() = SubscriptionDto(
    id = id,
    name = name,
    priceMinor = priceMinor,
    currency = currency,
    startedAt = startedAt,
    periodType = periodType,
    periodDays = periodDays,
    notifyDaysBefore = notifyDaysBefore,
    isCancelled = isCancelled,
    isDeleted = isDeleted,
    updatedAt = updatedAt
)

fun SubscriptionDto.toModel() = Subscription(
    id = id,
    name = name,
    priceMinor = priceMinor,
    currency = currency,
    startedAt = startedAt,
    periodType = periodType,
    periodDays = periodDays,
    notifyDaysBefore = notifyDaysBefore,
    isCancelled = isCancelled,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    isPendingSync = false
)

fun Product.toDto() = ProductDto(
    barcode = barcode,
    name = name,
    categoryId = categoryId,
    defaultDaysAfterOpening = defaultDaysAfterOpening,
    defaultNotifyDaysBefore = defaultNotifyDaysBefore,
    updatedAt = updatedAt
)

fun ProductDto.toModel() = Product(
    barcode = barcode,
    name = name,
    categoryId = categoryId,
    defaultDaysAfterOpening = defaultDaysAfterOpening,
    defaultNotifyDaysBefore = defaultNotifyDaysBefore,
    updatedAt = updatedAt,
    isPendingSync = false
)
