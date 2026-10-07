package ru.pikahk.outdateapp.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.pikahk.outdateapp.data.model.Item

class ExpiredWhileAwayTest {

    private val today: LocalDate = LocalDate.parse("2026-10-06")

    private fun item(
        name: String,
        expiresAt: String,
        openedAt: String? = null,
        daysAfterOpening: Int? = null,
        isDeleted: Boolean = false
    ) = Item(
        name = name,
        categoryId = null,
        barcode = null,
        expiresAt = LocalDate.parse(expiresAt),
        daysAfterOpening = daysAfterOpening,
        openedAt = openedAt?.let(LocalDate::parse),
        isDeleted = isDeleted
    )

    private fun names(items: List<Item>, lastOpened: String) =
        expiredWhileAway(items, LocalDate.parse(lastOpened), today).map { it.name }

    @Test
    fun `показывает то, что истекло, пока приложение не открывали`() {
        val items = listOf(
            item("Кефир", "2026-10-01"),
            item("Молоко", "2026-10-05")
        )
        assertEquals(listOf("Кефир", "Молоко"), names(items, "2026-09-29"))
    }

    @Test
    fun `не показывает то, что истекло ещё до прошлого запуска`() {
        assertEquals(emptyList<String>(), names(listOf(item("Йогурт", "2026-09-20")), "2026-09-29"))
    }

    @Test
    fun `учитывает продукт, у которого в день прошлого запуска был последний день`() {
        assertEquals(listOf("Сметана"), names(listOf(item("Сметана", "2026-09-29")), "2026-09-29"))
    }

    @Test
    fun `не показывает то, что истекает сегодня`() {
        assertEquals(emptyList<String>(), names(listOf(item("Хлеб", "2026-10-06")), "2026-09-29"))
    }

    @Test
    fun `учитывает срок после вскрытия`() {
        val items = listOf(item("Сок", "2027-01-01", openedAt = "2026-09-30", daysAfterOpening = 3))
        assertEquals(listOf("Сок"), names(items, "2026-09-29"))
    }

    @Test
    fun `ничего не показывает, если приложение открывали вчера`() {
        assertEquals(emptyList<String>(), names(listOf(item("Молоко", "2026-10-05")), "2026-10-05"))
    }

    @Test
    fun `не показывает удалённое`() {
        assertEquals(emptyList<String>(), names(listOf(item("Удалён", "2026-10-01", isDeleted = true)), "2026-09-29"))
    }

    @Test
    fun `сначала то, что истекло раньше`() {
        val items = listOf(
            item("Молоко", "2026-10-05"),
            item("Кефир", "2026-10-01"),
            item("Сметана", "2026-10-03")
        )
        assertEquals(listOf("Кефир", "Сметана", "Молоко"), names(items, "2026-09-29"))
    }
}
