package ru.pikahk.outdate.server

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import io.ktor.server.routing.routing
import ru.pikahk.outdate.server.plugins.configureMonitoring
import ru.pikahk.outdate.server.plugins.configureSerialization
import ru.pikahk.outdate.server.plugins.configureStatusPages
import ru.pikahk.outdate.server.routes.healthRoutes

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    configureSerialization()
    configureMonitoring()
    configureStatusPages()
    routing {
        healthRoutes()
    }
}
