package ru.pikahk.outdate.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ru.pikahk.outdate.server.family.CreateFamilyRequest
import ru.pikahk.outdate.server.family.FamilyService
import ru.pikahk.outdate.server.family.FamilySyncRequest
import ru.pikahk.outdate.server.family.JoinFamilyRequest
import ru.pikahk.outdate.server.plugins.AUTH_SESSION
import ru.pikahk.outdate.server.plugins.user

fun Route.familyRoutes(familyService: FamilyService) {
    authenticate(AUTH_SESSION) {
        route("/family") {
            get {
                call.respond(familyService.get(call.user()))
            }
            post {
                val request = call.receive<CreateFamilyRequest>()
                call.respond(HttpStatusCode.Created, familyService.create(call.user(), request))
            }
            delete {
                familyService.delete(call.user())
                call.respond(HttpStatusCode.NoContent)
            }
            post("/join") {
                val request = call.receive<JoinFamilyRequest>()
                call.respond(familyService.join(call.user(), request))
            }
            post("/invite") {
                call.respond(familyService.invite(call.user()))
            }
            post("/leave") {
                familyService.leave(call.user())
                call.respond(HttpStatusCode.NoContent)
            }
            delete("/members/{id}") {
                familyService.removeMember(call.user(), call.parameters["id"].orEmpty())
                call.respond(HttpStatusCode.NoContent)
            }
            post("/sync") {
                val request = call.receive<FamilySyncRequest>()
                call.respond(familyService.sync(call.user(), request))
            }
        }
    }
}
