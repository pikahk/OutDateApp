package ru.pikahk.outdate.server.db

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import ru.pikahk.outdate.server.TestDatabase

class MigrationsTest {

    private fun columns(table: String): List<String> = transaction(TestDatabase.connected.database) {
        exec(
            "SELECT column_name FROM information_schema.columns WHERE table_name = '$table' ORDER BY column_name"
        ) { result ->
            buildList { while (result.next()) add(result.getString(1)) }
        }.orEmpty()
    }

    @Test
    fun `миграции создают таблицу пользователей`() {
        assertEquals(
            listOf("created_at", "email", "email_verified", "id", "password_hash", "sync_version"),
            columns("users")
        )
    }

    @Test
    fun `миграции создают таблицу сессий`() {
        assertEquals(listOf("created_at", "expires_at", "token_hash", "user_id"), columns("sessions"))
    }

    @Test
    fun `миграции создают таблицу кодов из писем`() {
        assertEquals(
            listOf("attempts", "code", "created_at", "expires_at", "purpose", "user_id"),
            columns("email_codes")
        )
    }

    @Test
    fun `миграции создают таблицы для синхронизации`() {
        for (table in listOf("categories", "items", "subscriptions", "products")) {
            assertTrue(columns(table).containsAll(listOf("user_id", "updated_at", "version")), table)
        }
    }

    @Test
    fun `миграции создают таблицы для семейных групп`() {
        assertEquals(listOf("created_at", "id", "name", "owner_id", "sync_version"), columns("families"))
        assertEquals(listOf("family_id", "joined_at", "name", "user_id"), columns("family_members"))
        assertEquals(listOf("code", "expires_at", "family_id"), columns("family_invites"))
        assertTrue(columns("family_items").containsAll(listOf("family_id", "added_by", "updated_at", "version")))
    }
}
