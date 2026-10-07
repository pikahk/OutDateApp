package ru.pikahk.outdateapp.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    private val russian = Locale.forLanguageTag("ru")

    private fun format(minor: Long, roundToMajor: Boolean = false) =
        formatAmount(minor, russian, roundToMajor).filterNot(Char::isWhitespace)

    @Test
    fun `целая сумма переводится в копейки`() {
        assertEquals(39_900L, parsePriceMinor("399"))
    }

    @Test
    fun `копейки можно писать через запятую или точку`() {
        assertEquals(39_990L, parsePriceMinor("399,9"))
        assertEquals(39_990L, parsePriceMinor("399.90"))
        assertEquals(39_905L, parsePriceMinor("399,05"))
    }

    @Test
    fun `пробелы между разрядами не мешают`() {
        assertEquals(299_000L, parsePriceMinor(" 2 990 "))
    }

    @Test
    fun `бесплатная подписка допустима`() {
        assertEquals(0L, parsePriceMinor("0"))
    }

    @Test
    fun `не число не принимается`() {
        assertNull(parsePriceMinor(""))
        assertNull(parsePriceMinor("abc"))
        assertNull(parsePriceMinor("3,500,0"))
        assertNull(parsePriceMinor("1,234"))
        assertNull(parsePriceMinor(",5"))
    }

    @Test
    fun `целые рубли выводятся без копеек`() {
        assertEquals("399", format(39_900))
        assertEquals("2990", format(299_000))
    }

    @Test
    fun `копейки выводятся двумя знаками`() {
        assertEquals("399,90", format(39_990))
    }

    @Test
    fun `сумму можно округлить до рублей`() {
        assertEquals("4348", format(434_821, roundToMajor = true))
    }
}
