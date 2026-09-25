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
    fun `месяц и год означают конец месяца`() {
        assertEquals(LocalDate.of(2026, 10, 31), parseExpiryDate("10.2026"))
    }

    @Test
    fun `месяц и год учитывают високосный февраль`() {
        assertEquals(LocalDate.of(2028, 2, 29), parseExpiryDate("2.2028"))
        assertEquals(LocalDate.of(2027, 2, 28), parseExpiryDate("02.2027"))
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
        assertNull(parseExpiryDate("10.26"))
    }

    @Test
    fun `несуществующая дата не принимается`() {
        assertNull(parseExpiryDate("31.02.2026"))
    }

    @Test
    fun `несуществующий месяц не принимается`() {
        assertNull(parseExpiryDate("13.2026"))
    }

    @Test
    fun `дата изготовления разбирается полностью`() {
        assertEquals(LocalDate.of(2026, 9, 15), parseDate("15.09.2026"))
    }

    @Test
    fun `для даты изготовления нужен день`() {
        assertNull(parseDate("09.2026"))
    }

    @Test
    fun `дата для показа с ведущими нулями`() {
        assertEquals("05.01.2026", formatDate(LocalDate.of(2026, 1, 5)))
    }

    @Test
    fun `точки расставляются по мере ввода`() {
        assertEquals("1", formatDateDigits("1"))
        assertEquals("15", formatDateDigits("15"))
        assertEquals("15.1", formatDateDigits("151"))
        assertEquals("15.10", formatDateDigits("1510"))
        assertEquals("15.10.2", formatDateDigits("15102"))
        assertEquals("15.10.2026", formatDateDigits("15102026"))
    }

    @Test
    fun `месяц и год получают одну точку`() {
        assertEquals("10.2026", formatDateDigits("102026"))
    }

    @Test
    fun `лишние цифры отбрасываются`() {
        assertEquals("15.10.2026", formatDateDigits("151020269"))
        assertEquals("10.2026", formatDateDigits("1020269"))
    }

    @Test
    fun `повторное форматирование ничего не меняет`() {
        assertEquals("15.10.2026", formatDateDigits("15.10.2026"))
        assertEquals("15.10", formatDateDigits("15.10."))
    }
}
