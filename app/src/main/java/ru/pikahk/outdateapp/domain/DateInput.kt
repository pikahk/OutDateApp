package ru.pikahk.outdateapp.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

private val expiryDateFormat =
    DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT)

fun parseExpiryDate(text: String): LocalDate? {
    val normalized = text.trim().replace(',', '.').replace('-', '.').replace('/', '.')
    return runCatching { LocalDate.parse(normalized, expiryDateFormat) }.getOrNull()
}
