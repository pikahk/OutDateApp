package ru.pikahk.outdateapp.domain
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.pikahk.outdateapp.data.Item

class ExpiryTest {

    private val today: LocalDate = LocalDate.parse("2026-09-15")

    private fun item(expiresAt: String, openedAt: String? = null, daysAfterOpening: Int? = null) = Item(
        name = "Тестовый расходник",
        categoryId = null,
        barcode = null,
        expiresAt = LocalDate.parse(expiresAt),
        daysAfterOpening = daysAfterOpening,
        openedAt = openedAt?.let(LocalDate::parse)
    )

    @Test
    fun `запечатанный считается по дате с упаковки`() {
        val result = effectiveExpiryDate(item(expiresAt = "2026-09-19"))
        assertEquals(LocalDate.parse("2026-09-19"), result)
    }

    @Test
    fun `вскрытый считается по дате вскрытия, если она наступает раньше`() {
        val result = effectiveExpiryDate(
            item(expiresAt = "2026-09-19", openedAt = "2026-09-14", daysAfterOpening = 3)
        )
        assertEquals(LocalDate.parse("2026-09-17"), result)
    }

    @Test
    fun `вскрытие не продлевает срок за пределы упаковки`() {
        // Вскрыли 18-го, после вскрытия 3 дня — это 21-е.
        // Но упаковка кончается 19-го, значит остаётся 19-е.
        val result = effectiveExpiryDate(
            item(expiresAt = "2026-09-19", openedAt = "2026-09-18", daysAfterOpening = 3)
        )
        assertEquals(LocalDate.parse("2026-09-19"), result)
    }

    @Test
    fun `вскрытый без срока после вскрытия считается по упаковке`() {
        val result = effectiveExpiryDate(
            item(expiresAt = "2026-09-19", openedAt = "2026-09-10", daysAfterOpening = null)
        )
        assertEquals(LocalDate.parse("2026-09-19"), result)
    }

    @Test
    fun `запечатанный со сроком после вскрытия считается по упаковке`() {
        val result = effectiveExpiryDate(
            item(expiresAt = "2026-09-19", openedAt = null, daysAfterOpening = 3)
        )
        assertEquals(LocalDate.parse("2026-09-19"), result)
    }

    @Test
    fun `считает оставшиеся дни`() {
        assertEquals(4L, daysLeft(item(expiresAt = "2026-09-19"), today))
    }

    @Test
    fun `истекающий сегодня даёт ноль`() {
        assertEquals(0L, daysLeft(item(expiresAt = "2026-09-15"), today))
    }

    @Test
    fun `просроченный даёт отрицательное число`() {
        assertEquals(-2L, daysLeft(item(expiresAt = "2026-09-13"), today))
    }

    @Test
    fun `долгий срок на упаковке не спасает вскрытый расходник`() {
        val result = daysLeft(
            item(expiresAt = "2026-10-20", openedAt = "2026-09-10", daysAfterOpening = 3),
            today
        )
        assertEquals(-2L, result)
    }

    @Test
    fun `просроченное помечается как EXPIRED`() {
        assertEquals(Urgency.EXPIRED, urgency(-1))
    }

    @Test
    fun `сегодня и завтра помечаются как CRITICAL`() {
        assertEquals(Urgency.CRITICAL, urgency(0))
        assertEquals(Urgency.CRITICAL, urgency(1))
    }

    @Test
    fun `от двух до семи дней помечается как SOON`() {
        assertEquals(Urgency.SOON, urgency(2))
        assertEquals(Urgency.SOON, urgency(7))
    }

    @Test
    fun `больше недели помечается как OK`() {
        assertEquals(Urgency.OK, urgency(8))
    }
}
