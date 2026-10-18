package ru.pikahk.outdateapp.ui.auth

import ru.pikahk.outdateapp.data.remote.ApiException

const val CODE_LENGTH = 6
private const val PASSWORD_MIN_LENGTH = 8
private const val PASSWORD_MAX_LENGTH = 64
private const val EMAIL_MAX_LENGTH = 254

private val emailFormat = Regex("""[^\s@]+@[^\s@]+\.[^\s@]+""")

enum class AuthField { EMAIL, PASSWORD, CODE, FORM }

enum class AuthError(val field: AuthField) {
    INVALID_EMAIL(AuthField.EMAIL),
    EMAIL_TAKEN(AuthField.EMAIL),
    INVALID_PASSWORD(AuthField.PASSWORD),
    INVALID_CREDENTIALS(AuthField.PASSWORD),
    INVALID_CODE(AuthField.CODE),
    CODE_EXPIRED(AuthField.CODE),
    TOO_SOON(AuthField.CODE),
    MAIL_UNAVAILABLE(AuthField.FORM),
    NETWORK(AuthField.FORM),
    SERVER(AuthField.FORM)
}

fun authError(code: String): AuthError = when (code) {
    "invalid_email" -> AuthError.INVALID_EMAIL
    "email_taken" -> AuthError.EMAIL_TAKEN
    "invalid_password" -> AuthError.INVALID_PASSWORD
    "invalid_credentials" -> AuthError.INVALID_CREDENTIALS
    "invalid_code" -> AuthError.INVALID_CODE
    "code_expired" -> AuthError.CODE_EXPIRED
    "too_many_requests" -> AuthError.TOO_SOON
    "mail_unavailable" -> AuthError.MAIL_UNAVAILABLE
    ApiException.NETWORK -> AuthError.NETWORK
    else -> AuthError.SERVER
}

fun emailError(email: String): AuthError? {
    val trimmed = email.trim()
    val valid = trimmed.length <= EMAIL_MAX_LENGTH && emailFormat.matches(trimmed)
    return if (valid) null else AuthError.INVALID_EMAIL
}

fun passwordError(password: String): AuthError? =
    if (password.length in PASSWORD_MIN_LENGTH..PASSWORD_MAX_LENGTH) null else AuthError.INVALID_PASSWORD

fun AuthError?.on(field: AuthField): AuthError? = this?.takeIf { it.field == field }
