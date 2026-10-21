package ru.pikahk.outdateapp.domain

private const val GROUP_SEPARATOR = '\u001D'
private const val GTIN_PREFIX = "01"
private const val GTIN_LENGTH = 14
private val PRODUCT_CODE_LENGTHS = setOf(8, 12, 13, 14)

fun normalizeBarcode(raw: String): String? {
    val text = raw.trim().removePrefix("]d2").trimStart(GROUP_SEPARATOR)
    val code = when {
        text.length in PRODUCT_CODE_LENGTHS && text.all(Char::isDigit) -> text
        text.startsWith(GTIN_PREFIX) && text.length >= GTIN_PREFIX.length + GTIN_LENGTH ->
            text.substring(GTIN_PREFIX.length, GTIN_PREFIX.length + GTIN_LENGTH).takeIf { it.all(Char::isDigit) }
        else -> null
    }
    if (code == null || !hasValidCheckDigit(code)) return null
    return when (code.length) {
        12 -> "0$code"
        14 -> code.removePrefix("0")
        else -> code
    }
}

private fun hasValidCheckDigit(code: String): Boolean {
    val digits = code.map { it - '0' }
    val sum = digits.dropLast(1).asReversed().withIndex().sumOf { (index, digit) ->
        if (index % 2 == 0) digit * 3 else digit
    }
    return (10 - sum % 10) % 10 == digits.last()
}
