package ru.pikahk.outdate.server.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object UsersTable : Table("users") {
    val id = javaUUID("id")
    val email = text("email")
    val passwordHash = text("password_hash")
    val emailVerified = bool("email_verified").default(false)
    val syncVersion = long("sync_version").default(0)

    override val primaryKey = PrimaryKey(id)
}

object SessionsTable : Table("sessions") {
    val tokenHash = text("token_hash")
    val userId = javaUUID("user_id").references(UsersTable.id)
    val expiresAt = timestampWithTimeZone("expires_at")

    override val primaryKey = PrimaryKey(tokenHash)
}

object EmailCodesTable : Table("email_codes") {
    val userId = javaUUID("user_id").references(UsersTable.id)
    val purpose = text("purpose")
    val code = text("code")
    val attempts = integer("attempts").default(0)
    val createdAt = timestampWithTimeZone("created_at")
    val expiresAt = timestampWithTimeZone("expires_at")

    override val primaryKey = PrimaryKey(userId, purpose)
}

abstract class SyncedTable(name: String, keyColumn: String) : Table(name) {
    val userId = javaUUID("user_id").references(UsersTable.id)
    val key = text(keyColumn)
    val updatedAt = long("updated_at")
    val version = long("version")

    override val primaryKey = PrimaryKey(userId, key)
}

object CategoriesTable : SyncedTable("categories", "id") {
    val name = text("name")
    val isDefault = bool("is_default")
    val isDeleted = bool("is_deleted")
}

object ItemsTable : SyncedTable("items", "id") {
    val name = text("name")
    val categoryId = text("category_id").nullable()
    val barcode = text("barcode").nullable()
    val expiresAt = date("expires_at")
    val daysAfterOpening = integer("days_after_opening").nullable()
    val openedAt = date("opened_at").nullable()
    val notifyDaysBefore = integer("notify_days_before")
    val createdAt = long("created_at")
    val isDeleted = bool("is_deleted")
}

object SubscriptionsTable : SyncedTable("subscriptions", "id") {
    val name = text("name")
    val priceMinor = long("price_minor")
    val currency = text("currency")
    val startedAt = date("started_at")
    val periodType = text("period_type")
    val periodDays = integer("period_days").nullable()
    val notifyDaysBefore = integer("notify_days_before")
    val isCancelled = bool("is_cancelled")
    val isDeleted = bool("is_deleted")
}

object ProductsTable : SyncedTable("products", "barcode") {
    val name = text("name")
    val categoryId = text("category_id").nullable()
    val defaultDaysAfterOpening = integer("default_days_after_opening").nullable()
    val defaultNotifyDaysBefore = integer("default_notify_days_before").nullable()
}
