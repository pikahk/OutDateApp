package ru.pikahk.outdateapp.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageDatesTest {

    private val today = LocalDate.of(2026, 10, 22)

    private fun dates(text: String) = findDates(text, today)

    @Test
    fun `находится полная дата в тексте упаковки`() {
        assertEquals(listOf(LocalDate.of(2026, 11, 14)), dates("Годен до: 14.11.2026"))
    }

    @Test
    fun `год из двух цифр читается как 20xx`() {
        assertEquals(listOf(LocalDate.of(2026, 11, 14)), dates("14.11.26"))
    }

    @Test
    fun `понимаются косая черта, дефис и пробелы после точек`() {
        val expected = listOf(LocalDate.of(2026, 11, 14))
        assertEquals(expected, dates("14/11/26"))
        assertEquals(expected, dates("14-11-2026"))
        assertEquals(expected, dates("14. 11. 26"))
    }

    @Test
    fun `месяц и год означают последний день месяца`() {
        assertEquals(listOf(LocalDate.of(2027, 2, 28)), dates("Годен до 02.2027"))
    }

    @Test
    fun `полная дата не считается ещё и месяцем с годом`() {
        assertEquals(listOf(LocalDate.of(2026, 11, 14)), dates("14.11.2026"))
    }

    @Test
    fun `исправляются буквы вместо цифр`() {
        assertEquals(listOf(LocalDate.of(2026, 11, 14)), dates("14.1l.2O26"))
    }

    @Test
    fun `несуществующие даты, время, цены и далёкие годы отбрасываются`() {
        assertTrue(dates("31.02.2026").isEmpty())
        assertTrue(dates("12:30 99.90 руб 0.5 л").isEmpty())
        assertTrue(dates("14.11.2066").isEmpty())
        assertTrue(dates("партия L2611 05").isEmpty())
    }

    @Test
    fun `две даты — ранняя изготовление, поздняя срок годности`() {
        val reading = readPackageDates("ИЗГ 14.10.26 ГОДЕН ДО 14.11.26 ПАРТИЯ 1245", today)
        assertEquals(DateReading.Expiry(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 10, 14)), reading)
    }

    @Test
    fun `из трёх дат берутся самая ранняя и самая поздняя`() {
        val reading = readDates(
            listOf(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1)),
            today
        )
        assertEquals(DateReading.Expiry(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 9, 1)), reading)
    }

    @Test
    fun `одна дата в прошлом — это дата изготовления`() {
        assertEquals(DateReading.Production(LocalDate.of(2026, 9, 1)), readPackageDates("01.09.2026", today))
    }

    @Test
    fun `одна дата в будущем — это срок годности`() {
        assertEquals(DateReading.Expiry(LocalDate.of(2027, 3, 1), null), readPackageDates("01.03.27", today))
    }

    @Test
    fun `одна сегодняшняя дата требует уточнения`() {
        assertEquals(DateReading.Unclear(today), readPackageDates("22.10.2026", today))
    }

    @Test
    fun `без дат результата нет`() {
        assertNull(readPackageDates("Кефир 3,2% 930 г", today))
    }
}
