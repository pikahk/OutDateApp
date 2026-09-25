package ru.pikahk.outdateapp.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

private val fullDateFormat =
    DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT)

private val monthYearFormat =
    DateTimeFormatter.ofPattern("M.uuuu").withResolverStyle(ResolverStyle.STRICT)

fun parseExpiryDate(text: String): LocalDate? {
    val normalized = text.trim().replace(',', '.').replace('-', '.').replace('/', '.')
    return runCatching { LocalDate.parse(normalized, fullDateFormat) }.getOrNull()
        ?: runCatching { YearMonth.parse(normalized, monthYearFormat).atEndOfMonth() }.getOrNull()
}
