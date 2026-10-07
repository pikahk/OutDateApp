package ru.pikahk.outdateapp.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.pikahk.outdateapp.data.model.PeriodType
import ru.pikahk.outdateapp.data.model.Subscription

class SubscriptionBillingTest {

    private val today: LocalDate = LocalDate.parse("2026-10-06")

    private fun subscription(
        startedAt: String,
        periodType: PeriodType = PeriodType.MONTHLY,
        periodDays: Int? = null,
        priceMinor: Long = 39_900,
        isCancelled: Boolean = false,
        isDeleted: Boolean = false,
        name: String = "Подписка",
        notifyDaysBefore: Int = 3
    ) = Subscription(
        name = name,
        priceMinor = priceMinor,
        startedAt = LocalDate.parse(startedAt),
        periodType = periodType,
        periodDays = periodDays,
        notifyDaysBefore = notifyDaysBefore,
        isCancelled = isCancelled,
        isDeleted = isDeleted
    )

    private fun upcomingNames(vararg subscriptions: Subscription) =
        upcomingCharges(subscriptions.toList(), today).map { it.subscription.name }

    private fun next(subscription: Subscription, on: LocalDate = today) = nextChargeDate(subscription, on).toString()

    @Test
    fun `ежемесячная списывается в тот же день месяца`() {
        assertEquals("2026-10-07", next(subscription("2025-03-07")))
    }

    @Test
    fun `сегодняшнее списание считается ближайшим`() {
        assertEquals("2026-10-06", next(subscription("2025-03-06")))
    }

    @Test
    fun `оформленная сегодня списывается через период`() {
        assertEquals("2026-11-06", next(subscription("2026-10-06")))
    }

    @Test
    fun `в коротком месяце списание переносится на последний день`() {
        val subscription = subscription("2026-01-31")
        assertEquals("2026-02-28", next(subscription, LocalDate.parse("2026-02-10")))
        assertEquals("2026-03-31", next(subscription, LocalDate.parse("2026-03-01")))
    }

    @Test
    fun `ежегодная списывается раз в год`() {
        assertEquals("2026-11-22", next(subscription("2024-11-22", PeriodType.YEARLY)))
    }

    @Test
    fun `ежегодная с 29 февраля в обычный год списывается 28-го`() {
        val subscription = subscription("2024-02-29", PeriodType.YEARLY)
        assertEquals("2026-02-28", next(subscription, LocalDate.parse("2025-03-01")))
    }

    @Test
    fun `произвольный период считается в днях`() {
        assertEquals("2026-10-18", next(subscription("2026-09-18", PeriodType.CUSTOM_DAYS, periodDays = 30)))
        assertEquals("2026-10-08", next(subscription("2026-01-01", PeriodType.CUSTOM_DAYS, periodDays = 14)))
    }

    @Test
    fun `сумма в месяц приводит все периоды к месяцу`() {
        val subscriptions = listOf(
            subscription("2025-03-07", priceMinor = 39_900),
            subscription("2024-06-15", priceMinor = 14_900),
            subscription("2024-11-22", PeriodType.YEARLY, priceMinor = 299_000),
            subscription("2026-09-18", PeriodType.CUSTOM_DAYS, periodDays = 30, priceMinor = 350_000)
        )
        assertEquals(434_821L, monthlyTotalMinor(subscriptions))
    }

    @Test
    fun `отменённые и удалённые в сумму не входят`() {
        val subscriptions = listOf(
            subscription("2025-03-07", priceMinor = 39_900),
            subscription("2025-03-07", priceMinor = 39_900, isCancelled = true),
            subscription("2025-03-07", priceMinor = 39_900, isDeleted = true)
        )
        assertEquals(39_900L, monthlyTotalMinor(subscriptions))
    }

    @Test
    fun `напоминает о списании в пределах заданного числа дней`() {
        val names = upcomingNames(
            subscription("2025-03-09", name = "Через 3 дня"),
            subscription("2025-03-10", name = "Через 4 дня")
        )
        assertEquals(listOf("Через 3 дня"), names)
    }

    @Test
    fun `напоминает в день списания`() {
        assertEquals(listOf("Сегодня"), upcomingNames(subscription("2025-03-06", name = "Сегодня")))
    }

    @Test
    fun `не напоминает об отменённой и с выключенным напоминанием`() {
        val names = upcomingNames(
            subscription("2025-03-07", name = "Отменена", isCancelled = true),
            subscription("2025-03-07", name = "Без напоминания", notifyDaysBefore = REMINDER_OFF)
        )
        assertEquals(emptyList<String>(), names)
    }

    @Test
    fun `сначала ближайшие списания`() {
        val names = upcomingNames(
            subscription("2025-03-08", name = "Послезавтра"),
            subscription("2025-03-07", name = "Завтра")
        )
        assertEquals(listOf("Завтра", "Послезавтра"), names)
    }
}
