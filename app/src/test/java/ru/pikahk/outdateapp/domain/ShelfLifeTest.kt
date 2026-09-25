package ru.pikahk.outdateapp.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ShelfLifeTest {

    private val producedAt: LocalDate = LocalDate.parse("2026-09-15")

    @Test
    fun `сутки прибавляются к дате изготовления`() {
        val result = expiryFromProduction(producedAt, 10, ShelfLifeUnit.DAYS)
        assertEquals(LocalDate.parse("2026-09-25"), result)
    }

    @Test
    fun `часы переводятся в сутки`() {
        val result = expiryFromProduction(producedAt, 72, ShelfLifeUnit.HOURS)
        assertEquals(LocalDate.parse("2026-09-18"), result)
    }

    @Test
    fun `неполные сутки из часов отбрасываются`() {
        val result = expiryFromProduction(producedAt, 36, ShelfLifeUnit.HOURS)
        assertEquals(LocalDate.parse("2026-09-16"), result)
    }

    @Test
    fun `месяцы прибавляются к дате изготовления`() {
        val result = expiryFromProduction(producedAt, 12, ShelfLifeUnit.MONTHS)
        assertEquals(LocalDate.parse("2027-09-15"), result)
    }

    @Test
    fun `месяц от 31 января кончается в конце февраля`() {
        val result = expiryFromProduction(LocalDate.parse("2026-01-31"), 1, ShelfLifeUnit.MONTHS)
        assertEquals(LocalDate.parse("2026-02-28"), result)
    }

    @Test
    fun `годы прибавляются к дате изготовления`() {
        val result = expiryFromProduction(producedAt, 2, ShelfLifeUnit.YEARS)
        assertEquals(LocalDate.parse("2028-09-15"), result)
    }
}
