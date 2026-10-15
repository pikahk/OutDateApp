package ru.pikahk.outdate.server.db

import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import ru.pikahk.outdate.server.TestDatabase

class MigrationsTest {

    @Test
    fun `миграции создают таблицу пользователей`() {
        val columns = transaction(TestDatabase.connected.database) {
            exec(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'users' ORDER BY column_name"
            ) { result ->
                buildList { while (result.next()) add(result.getString(1)) }
            }
        }
        assertEquals(listOf("created_at", "email", "email_verified", "id", "password_hash"), columns)
    }
}
