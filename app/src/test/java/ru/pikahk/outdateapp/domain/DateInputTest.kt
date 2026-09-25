package ru.pikahk.outdateapp.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DateInputTest {

    @Test
    fun `разбирает дату с точками`() {
        assertEquals(LocalDate.of(2026, 10, 15), parseExpiryDate("15.10.2026"))
    }

    @Test
    fun `принимает день и месяц без ведущего нуля`() {
        assertEquals(LocalDate.of(2027, 1, 5), parseExpiryDate("5.1.2027"))
    }

    @Test
    fun `принимает запятую и дефис вместо точки`() {
        assertEquals(LocalDate.of(2026, 10, 15), parseExpiryDate("15,10,2026"))
        assertEquals(LocalDate.of(2026, 10, 15), parseExpiryDate("15-10-2026"))
    }

    @Test
    fun `игнорирует пробелы по краям`() {
        assertEquals(LocalDate.of(2026, 10, 15), parseExpiryDate(" 15.10.2026 "))
    }

    @Test
    fun `пустая строка не дата`() {
        assertNull(parseExpiryDate(""))
    }

    @Test
    fun `мусор не дата`() {
        assertNull(parseExpiryDate("абв"))
    }

    @Test
    fun `недописанная дата не дата`() {
        assertNull(parseExpiryDate("15.10"))
    }

    @Test
    fun `год из двух цифр не принимается`() {
        assertNull(parseExpiryDate("15.10.26"))
    }

    @Test
    fun `несуществующая дата не принимается`() {
        assertNull(parseExpiryDate("31.02.2026"))
    }
}
