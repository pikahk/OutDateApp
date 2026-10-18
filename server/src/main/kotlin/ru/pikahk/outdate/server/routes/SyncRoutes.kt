package ru.pikahk.outdate.server.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import ru.pikahk.outdate.server.plugins.AUTH_SESSION
import ru.pikahk.outdate.server.plugins.user
import ru.pikahk.outdate.server.sync.SyncRequest
import ru.pikahk.outdate.server.sync.SyncService

fun Route.syncRoutes(syncService: SyncService) {
    authenticate(AUTH_SESSION) {
        post("/sync") {
            val request = call.receive<SyncRequest>()
            call.respond(syncService.sync(call.user(), request))
        }
    }
}
