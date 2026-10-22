package ru.pikahk.outdateapp.domain

import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private const val MAX_YEARS_AWAY = 15L
private const val CENTURY = 2000

private const val DIGIT = "[0-9OoIl|]"
private const val SEPARATOR = """\s?[./-]\s?"""
private const val NO_DIGIT_BEFORE = "(?<![0-9OoIl|])"
private const val NO_DIGIT_AFTER = "(?![0-9OoIl|])"

private val fullDate = Regex(
    "$NO_DIGIT_BEFORE($DIGIT{1,2})$SEPARATOR($DIGIT{1,2})$SEPARATOR($DIGIT{4}|$DIGIT{2})$NO_DIGIT_AFTER"
)
private val monthYear = Regex("$NO_DIGIT_BEFORE($DIGIT{1,2})$SEPARATOR($DIGIT{4})$NO_DIGIT_AFTER")

sealed interface DateReading {
    data class Expiry(val expiresAt: LocalDate, val producedAt: LocalDate?) : DateReading

    data class Production(val producedAt: LocalDate) : DateReading

    data class Unclear(val date: LocalDate) : DateReading
}

fun readPackageDates(text: String, today: LocalDate): DateReading? = readDates(findDates(text, today), today)

fun findDates(text: String, today: LocalDate): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var rest = text
    fullDate.findAll(text).forEach { match ->
        val (day, month, year) = match.destructured
        dateOrNull { LocalDate.of(fullYear(year), number(month), number(day)) }?.let(dates::add)
        rest = rest.replaceRange(match.range, " ".repeat(match.value.length))
    }
    monthYear.findAll(rest).forEach { match ->
        val (month, year) = match.destructured
        dateOrNull { YearMonth.of(number(year), number(month)).atEndOfMonth() }?.let(dates::add)
    }
    return dates
        .filter { abs(ChronoUnit.YEARS.between(it, today)) <= MAX_YEARS_AWAY }
        .distinct()
        .sorted()
}

fun readDates(dates: List<LocalDate>, today: LocalDate): DateReading? {
    val sorted = dates.distinct().sorted()
    val single = sorted.singleOrNull()
    return when {
        sorted.isEmpty() -> null
        single == null -> DateReading.Expiry(expiresAt = sorted.last(), producedAt = sorted.first())
        single < today -> DateReading.Production(single)
        single > today -> DateReading.Expiry(expiresAt = single, producedAt = null)
        else -> DateReading.Unclear(single)
    }
}

private fun number(text: String): Int = text.map { char ->
    when (char) {
        'O', 'o' -> '0'
        'I', 'l', '|' -> '1'
        else -> char
    }
}.joinToString("").toInt()

private fun fullYear(text: String): Int = number(text).let { if (text.length == 2) CENTURY + it else it }

private fun dateOrNull(block: () -> LocalDate): LocalDate? = try {
    block()
} catch (e: DateTimeException) {
    null
}
