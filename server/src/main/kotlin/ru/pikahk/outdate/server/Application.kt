package ru.pikahk.outdate.server

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.netty.EngineMain
import io.ktor.server.routing.routing
import org.jetbrains.exposed.v1.jdbc.Database
import ru.pikahk.outdate.server.db.connectDatabase
import ru.pikahk.outdate.server.db.databaseSettings
import ru.pikahk.outdate.server.plugins.configureMonitoring
import ru.pikahk.outdate.server.plugins.configureSerialization
import ru.pikahk.outdate.server.plugins.configureStatusPages
import ru.pikahk.outdate.server.routes.healthRoutes

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val connected = connectDatabase(environment.config.databaseSettings())
    monitor.subscribe(ApplicationStopped) { connected.close() }
    configure(connected.database)
}

fun Application.configure(database: Database) {
    configureSerialization()
    configureMonitoring()
    configureStatusPages()
    routing {
        healthRoutes(database)
    }
}
