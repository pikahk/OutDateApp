package ru.pikahk.outdateapp.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.pikahk.outdateapp.data.Item

class ReminderTest {

    private val today: LocalDate = LocalDate.parse("2026-10-05")

    private fun item(name: String, expiresAt: String, notifyDaysBefore: Int = 3, isDeleted: Boolean = false) = Item(
        name = name,
        categoryId = null,
        barcode = null,
        expiresAt = LocalDate.parse(expiresAt),
        daysAfterOpening = null,
        openedAt = null,
        notifyDaysBefore = notifyDaysBefore,
        isDeleted = isDeleted
    )

    private fun names(items: List<Item>) = itemsToRemind(items, today).map { it.name }

    @Test
    fun `напоминает, когда до конца срока осталось не больше заданного числа дней`() {
        val items = listOf(
            item("Через 3 дня", "2026-10-08"),
            item("Через 4 дня", "2026-10-09")
        )
        assertEquals(listOf("Через 3 дня"), names(items))
    }

    @Test
    fun `напоминает в последний день срока`() {
        assertEquals(listOf("Сегодня"), names(listOf(item("Сегодня", "2026-10-05"))))
    }

    @Test
    fun `не напоминает о просроченном`() {
        assertEquals(emptyList<String>(), names(listOf(item("Вчера", "2026-10-04"))))
    }

    @Test
    fun `учитывает своё число дней у каждого продукта`() {
        val items = listOf(
            item("За неделю", "2026-10-10", notifyDaysBefore = 7),
            item("За день", "2026-10-10", notifyDaysBefore = 1)
        )
        assertEquals(listOf("За неделю"), names(items))
    }

    @Test
    fun `не напоминает об удалённом`() {
        assertEquals(emptyList<String>(), names(listOf(item("Удалён", "2026-10-06", isDeleted = true))))
    }

    @Test
    fun `сначала то, что истекает раньше`() {
        val items = listOf(
            item("Послезавтра", "2026-10-07"),
            item("Сегодня", "2026-10-05"),
            item("Завтра", "2026-10-06")
        )
        assertEquals(listOf("Сегодня", "Завтра", "Послезавтра"), names(items))
    }
}
