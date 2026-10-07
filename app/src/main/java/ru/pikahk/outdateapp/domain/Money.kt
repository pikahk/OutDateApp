package ru.pikahk.outdateapp.domain

import java.text.NumberFormat
import java.util.Locale

private const val MINOR_IN_MAJOR = 100

private val priceFormat = Regex("""\d{1,7}([.,]\d{1,2})?""")

fun parsePriceMinor(text: String): Long? {
    val normalized = text.filterNot(Char::isWhitespace)
    if (!priceFormat.matches(normalized)) return null
    val parts = normalized.split('.', ',')
    val kopecks = parts.getOrNull(1)?.padEnd(2, '0')?.toLong() ?: 0
    return parts[0].toLong() * MINOR_IN_MAJOR + kopecks
}

fun formatAmount(minor: Long, locale: Locale, roundToMajor: Boolean = false): String {
    val fractionDigits = if (roundToMajor || minor % MINOR_IN_MAJOR == 0L) 0 else 2
    val format = NumberFormat.getNumberInstance(locale)
    format.minimumFractionDigits = fractionDigits
    format.maximumFractionDigits = fractionDigits
    return format.format(minor.toDouble() / MINOR_IN_MAJOR)
}
