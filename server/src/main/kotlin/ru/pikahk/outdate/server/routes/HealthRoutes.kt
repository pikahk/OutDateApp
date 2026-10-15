package ru.pikahk.outdate.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Serializable
data class HealthResponse(val status: String, val database: String)

fun Route.healthRoutes(database: Database) {
    get("/health") {
        if (isDatabaseUp(database)) {
            call.respond(HealthResponse(status = "ok", database = "ok"))
        } else {
            call.respond(HttpStatusCode.ServiceUnavailable, HealthResponse(status = "error", database = "down"))
        }
    }
}

private suspend fun isDatabaseUp(database: Database): Boolean = withContext(Dispatchers.IO) {
    runCatching { transaction(database) { exec("SELECT 1") } }.isSuccess
}
