package ru.pikahk.outdate.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database

private const val MAX_POOL_SIZE = 5

data class DatabaseSettings(val url: String, val user: String, val password: String)

fun ApplicationConfig.databaseSettings() = DatabaseSettings(
    url = property("database.url").getString(),
    user = property("database.user").getString(),
    password = property("database.password").getString()
)

class ConnectedDatabase(val database: Database, private val dataSource: HikariDataSource) : AutoCloseable {
    override fun close() = dataSource.close()
}

fun connectDatabase(settings: DatabaseSettings): ConnectedDatabase {
    val dataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = settings.url
            username = settings.user
            password = settings.password
            maximumPoolSize = MAX_POOL_SIZE
        }
    )
    Flyway.configure().dataSource(dataSource).load().migrate()
    return ConnectedDatabase(Database.connect(dataSource), dataSource)
}
