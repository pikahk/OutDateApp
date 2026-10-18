package ru.pikahk.outdate.server.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object UsersTable : Table("users") {
    val id = javaUUID("id")
    val email = text("email")
    val passwordHash = text("password_hash")
    val emailVerified = bool("email_verified").default(false)

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
