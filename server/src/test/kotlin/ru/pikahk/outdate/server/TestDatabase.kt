package ru.pikahk.outdate.server

import org.testcontainers.postgresql.PostgreSQLContainer
import ru.pikahk.outdate.server.db.ConnectedDatabase
import ru.pikahk.outdate.server.db.DatabaseSettings
import ru.pikahk.outdate.server.db.connectDatabase

object TestDatabase {
    private val container: PostgreSQLContainer by lazy {
        PostgreSQLContainer("postgres:18-alpine").apply { start() }
    }

    val connected: ConnectedDatabase by lazy {
        connectDatabase(DatabaseSettings(container.jdbcUrl, container.username, container.password))
    }
}
