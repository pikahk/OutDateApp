package ru.pikahk.outdate.server.auth

const val PASSWORD_MIN_LENGTH = 8
const val PASSWORD_MAX_LENGTH = 64
private const val EMAIL_MAX_LENGTH = 254

private val emailFormat = Regex("""[^\s@]+@[^\s@]+\.[^\s@]+""")

fun normalizeEmail(email: String): String = email.trim().lowercase()

fun isValidEmail(email: String): Boolean = email.length <= EMAIL_MAX_LENGTH && emailFormat.matches(email)

fun isValidPassword(password: String): Boolean = password.length in PASSWORD_MIN_LENGTH..PASSWORD_MAX_LENGTH
