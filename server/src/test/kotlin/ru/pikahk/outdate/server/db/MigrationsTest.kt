package ru.pikahk.outdate.server.db

import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertEquals(listOf("created_at", "email", "email_verified", "id", "password_hash"), columns("users"))
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
}
