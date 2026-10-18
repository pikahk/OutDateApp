package ru.pikahk.outdate.server.sync

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import ru.pikahk.outdate.server.db.CategoriesTable
import ru.pikahk.outdate.server.db.ItemsTable
import ru.pikahk.outdate.server.db.ProductsTable
import ru.pikahk.outdate.server.db.SubscriptionsTable

fun UpdateBuilder<*>.fill(category: CategoryDto) {
    this[CategoriesTable.name] = category.name
    this[CategoriesTable.isDefault] = category.isDefault
    this[CategoriesTable.isDeleted] = category.isDeleted
}

fun UpdateBuilder<*>.fill(item: ItemDto) {
    this[ItemsTable.name] = item.name
    this[ItemsTable.categoryId] = item.categoryId
    this[ItemsTable.barcode] = item.barcode
    this[ItemsTable.expiresAt] = item.expiresAt
    this[ItemsTable.daysAfterOpening] = item.daysAfterOpening
    this[ItemsTable.openedAt] = item.openedAt
    this[ItemsTable.notifyDaysBefore] = item.notifyDaysBefore
    this[ItemsTable.createdAt] = item.createdAt
    this[ItemsTable.isDeleted] = item.isDeleted
}

fun UpdateBuilder<*>.fill(subscription: SubscriptionDto) {
    this[SubscriptionsTable.name] = subscription.name
    this[SubscriptionsTable.priceMinor] = subscription.priceMinor
    this[SubscriptionsTable.currency] = subscription.currency
    this[SubscriptionsTable.startedAt] = subscription.startedAt
    this[SubscriptionsTable.periodType] = subscription.periodType.name
    this[SubscriptionsTable.periodDays] = subscription.periodDays
    this[SubscriptionsTable.notifyDaysBefore] = subscription.notifyDaysBefore
    this[SubscriptionsTable.isCancelled] = subscription.isCancelled
    this[SubscriptionsTable.isDeleted] = subscription.isDeleted
}

fun UpdateBuilder<*>.fill(product: ProductDto) {
    this[ProductsTable.name] = product.name
    this[ProductsTable.categoryId] = product.categoryId
    this[ProductsTable.defaultDaysAfterOpening] = product.defaultDaysAfterOpening
    this[ProductsTable.defaultNotifyDaysBefore] = product.defaultNotifyDaysBefore
}

fun ResultRow.toCategory() = CategoryDto(
    id = this[CategoriesTable.key],
    name = this[CategoriesTable.name],
    isDefault = this[CategoriesTable.isDefault],
    isDeleted = this[CategoriesTable.isDeleted],
    updatedAt = this[CategoriesTable.updatedAt]
)

fun ResultRow.toItem() = ItemDto(
    id = this[ItemsTable.key],
    name = this[ItemsTable.name],
    categoryId = this[ItemsTable.categoryId],
    barcode = this[ItemsTable.barcode],
    expiresAt = this[ItemsTable.expiresAt],
    daysAfterOpening = this[ItemsTable.daysAfterOpening],
    openedAt = this[ItemsTable.openedAt],
    notifyDaysBefore = this[ItemsTable.notifyDaysBefore],
    createdAt = this[ItemsTable.createdAt],
    isDeleted = this[ItemsTable.isDeleted],
    updatedAt = this[ItemsTable.updatedAt]
)

fun ResultRow.toSubscription() = SubscriptionDto(
    id = this[SubscriptionsTable.key],
    name = this[SubscriptionsTable.name],
    priceMinor = this[SubscriptionsTable.priceMinor],
    currency = this[SubscriptionsTable.currency],
    startedAt = this[SubscriptionsTable.startedAt],
    periodType = PeriodType.valueOf(this[SubscriptionsTable.periodType]),
    periodDays = this[SubscriptionsTable.periodDays],
    notifyDaysBefore = this[SubscriptionsTable.notifyDaysBefore],
    isCancelled = this[SubscriptionsTable.isCancelled],
    isDeleted = this[SubscriptionsTable.isDeleted],
    updatedAt = this[SubscriptionsTable.updatedAt]
)

fun ResultRow.toProduct() = ProductDto(
    barcode = this[ProductsTable.key],
    name = this[ProductsTable.name],
    categoryId = this[ProductsTable.categoryId],
    defaultDaysAfterOpening = this[ProductsTable.defaultDaysAfterOpening],
    defaultNotifyDaysBefore = this[ProductsTable.defaultNotifyDaysBefore],
    updatedAt = this[ProductsTable.updatedAt]
)
