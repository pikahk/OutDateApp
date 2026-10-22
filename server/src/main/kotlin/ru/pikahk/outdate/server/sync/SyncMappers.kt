package ru.pikahk.outdate.server.sync

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import ru.pikahk.outdate.server.db.CategoriesTable
import ru.pikahk.outdate.server.db.FamilyItemsTable
import ru.pikahk.outdate.server.db.ItemTable
import ru.pikahk.outdate.server.db.ProductsTable
import ru.pikahk.outdate.server.db.SubscriptionsTable

fun UpdateBuilder<*>.fill(category: CategoryDto) {
    this[CategoriesTable.name] = category.name
    this[CategoriesTable.isDefault] = category.isDefault
    this[CategoriesTable.isDeleted] = category.isDeleted
}

fun UpdateBuilder<*>.fill(table: ItemTable, item: ItemDto) {
    this[table.name] = item.name
    this[table.categoryId] = item.categoryId
    this[table.barcode] = item.barcode
    this[table.expiresAt] = item.expiresAt
    this[table.daysAfterOpening] = item.daysAfterOpening
    this[table.openedAt] = item.openedAt
    this[table.notifyDaysBefore] = item.notifyDaysBefore
    this[table.createdAt] = item.createdAt
    this[table.isDeleted] = item.isDeleted
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

fun ResultRow.toItem(table: ItemTable) = ItemDto(
    id = this[table.key],
    name = this[table.name],
    categoryId = this[table.categoryId],
    barcode = this[table.barcode],
    expiresAt = this[table.expiresAt],
    daysAfterOpening = this[table.daysAfterOpening],
    openedAt = this[table.openedAt],
    notifyDaysBefore = this[table.notifyDaysBefore],
    createdAt = this[table.createdAt],
    isDeleted = this[table.isDeleted],
    addedBy = if (table is FamilyItemsTable) this[table.addedBy] else null,
    updatedAt = this[table.updatedAt]
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
