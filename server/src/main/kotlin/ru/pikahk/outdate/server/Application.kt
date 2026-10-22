package ru.pikahk.outdate.server

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.netty.EngineMain
import io.ktor.server.routing.routing
import java.time.Clock
import org.jetbrains.exposed.v1.jdbc.Database
import ru.pikahk.outdate.server.auth.AuthService
import ru.pikahk.outdate.server.db.connectDatabase
import ru.pikahk.outdate.server.db.databaseSettings
import ru.pikahk.outdate.server.family.FamilyService
import ru.pikahk.outdate.server.mail.Mailer
import ru.pikahk.outdate.server.mail.SmtpMailer
import ru.pikahk.outdate.server.mail.mailSettings
import ru.pikahk.outdate.server.plugins.configureMonitoring
import ru.pikahk.outdate.server.plugins.configureSecurity
import ru.pikahk.outdate.server.plugins.configureSerialization
import ru.pikahk.outdate.server.plugins.configureStatusPages
import ru.pikahk.outdate.server.routes.authRoutes
import ru.pikahk.outdate.server.routes.familyRoutes
import ru.pikahk.outdate.server.routes.healthRoutes
import ru.pikahk.outdate.server.routes.syncRoutes
import ru.pikahk.outdate.server.sync.SyncService

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val connected = connectDatabase(environment.config.databaseSettings())
    monitor.subscribe(ApplicationStopped) { connected.close() }
    configure(connected.database, SmtpMailer(environment.config.mailSettings()))
}

fun Application.configure(database: Database, mailer: Mailer, clock: Clock = Clock.systemUTC()) {
    val authService = AuthService(database, mailer, clock)
    val syncService = SyncService(database)
    val familyService = FamilyService(database, clock)
    configureSerialization()
    configureMonitoring()
    configureStatusPages()
    configureSecurity(authService)
    routing {
        healthRoutes(database)
        authRoutes(authService)
        syncRoutes(syncService)
        familyRoutes(familyService)
    }
}
