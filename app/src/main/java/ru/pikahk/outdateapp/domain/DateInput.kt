package ru.pikahk.outdateapp.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

private val fullDateFormat =
    DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT)

private val monthYearFormat =
    DateTimeFormatter.ofPattern("M.uuuu").withResolverStyle(ResolverStyle.STRICT)

private val displayFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu")

fun parseDate(text: String): LocalDate? = runCatching { LocalDate.parse(normalize(text), fullDateFormat) }.getOrNull()

fun parseExpiryDate(text: String): LocalDate? =
    parseDate(text) ?: runCatching { YearMonth.parse(normalize(text), monthYearFormat).atEndOfMonth() }.getOrNull()

fun formatDate(date: LocalDate): String = date.format(displayFormat)

private fun normalize(text: String): String = text.trim().replace(',', '.').replace('-', '.').replace('/', '.')
