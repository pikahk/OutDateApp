package ru.pikahk.outdate.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal
import ru.pikahk.outdate.server.auth.AuthService
import ru.pikahk.outdate.server.auth.UserPrincipal

const val AUTH_SESSION = "session"

fun Application.configureSecurity(authService: AuthService) {
    install(Authentication) {
        bearer(AUTH_SESSION) {
            authenticate { credential -> authService.findUser(credential.token) }
        }
    }
}

fun ApplicationCall.user(): UserPrincipal = checkNotNull(principal<UserPrincipal>())
