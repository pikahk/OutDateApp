package ru.pikahk.outdate.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import ru.pikahk.outdate.server.auth.AuthService
import ru.pikahk.outdate.server.plugins.AUTH_SESSION
import ru.pikahk.outdate.server.plugins.user

@Serializable
data class CredentialsRequest(val email: String, val password: String)

@Serializable
data class EmailRequest(val email: String)

@Serializable
data class CodeRequest(val code: String)

@Serializable
data class ResetPasswordRequest(val email: String, val code: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val email: String)

@Serializable
data class MeResponse(val email: String, val emailVerified: Boolean)

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/register") {
            val request = call.receive<CredentialsRequest>()
            val session = authService.register(request.email, request.password)
            call.respond(HttpStatusCode.Created, AuthResponse(session.token, session.email))
        }
        post("/login") {
            val request = call.receive<CredentialsRequest>()
            val session = authService.login(request.email, request.password)
            call.respond(AuthResponse(session.token, session.email))
        }
        post("/password/forgot") {
            val request = call.receive<EmailRequest>()
            authService.requestPasswordReset(request.email)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/password/reset") {
            val request = call.receive<ResetPasswordRequest>()
            val session = authService.resetPassword(request.email, request.code, request.password)
            call.respond(AuthResponse(session.token, session.email))
        }
        authenticate(AUTH_SESSION) {
            post("/logout") {
                authService.logout(call.user().token)
                call.respond(HttpStatusCode.NoContent)
            }
            post("/email/verify") {
                val request = call.receive<CodeRequest>()
                authService.verifyEmail(call.user(), request.code)
                call.respond(HttpStatusCode.NoContent)
            }
            post("/email/resend") {
                authService.resendVerification(call.user())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
    authenticate(AUTH_SESSION) {
        get("/me") {
            val user = call.user()
            call.respond(MeResponse(user.email, user.emailVerified))
        }
    }
}
