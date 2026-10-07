package ru.pikahk.outdateapp.ui.subscriptions

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.pikahk.outdateapp.data.model.PeriodType
import ru.pikahk.outdateapp.data.model.Subscription

class SubscriptionFormTest {

    private val today: LocalDate = LocalDate.parse("2026-10-06")

    private val filled = SubscriptionForm(
        name = " Яндекс Плюс ",
        price = "399",
        startedAt = "07.03.2025",
        notifyDaysBefore = 7
    )

    @Test
    fun `заполненная форма превращается в подписку`() {
        val subscription = filled.toSubscription(null, today)!!
        assertEquals("Яндекс Плюс", subscription.name)
        assertEquals(39_900L, subscription.priceMinor)
        assertEquals(LocalDate.parse("2025-03-07"), subscription.startedAt)
        assertEquals(PeriodType.MONTHLY, subscription.periodType)
        assertNull(subscription.periodDays)
        assertEquals(7, subscription.notifyDaysBefore)
    }

    @Test
    fun `стоимость не числом не сохраняется`() {
        val form = filled.copy(price = "3,500,0")
        assertNull(form.priceMinor)
        assertNull(form.toSubscription(null, today))
    }

    @Test
    fun `дата оформления в будущем не сохраняется`() {
        val form = filled.copy(startedAt = "18.12.2026")
        assertTrue(form.startsInFuture(today))
        assertNull(form.toSubscription(null, today))
        assertNull(form.nextCharge(today))
    }

    @Test
    fun `для периода в днях нужно число дней`() {
        val form = filled.copy(periodType = PeriodType.CUSTOM_DAYS)
        assertNull(form.toSubscription(null, today))
        assertEquals(30, form.copy(periodDays = "30").toSubscription(null, today)!!.periodDays)
        assertNull(form.copy(periodDays = "0").toSubscription(null, today))
    }

    @Test
    fun `число дней не сохраняется для месячной подписки`() {
        val subscription = filled.copy(periodDays = "30").toSubscription(null, today)!!
        assertNull(subscription.periodDays)
    }

    @Test
    fun `без названия не сохраняется`() {
        assertNull(filled.copy(name = "  ").toSubscription(null, today))
    }

    @Test
    fun `изменение сохраняет id и отмену`() {
        val base = Subscription(
            id = "sub",
            name = "Старое",
            priceMinor = 100,
            startedAt = today,
            periodType = PeriodType.YEARLY,
            periodDays = null,
            isCancelled = true
        )
        val subscription = filled.toSubscription(base, today)!!
        assertEquals("sub", subscription.id)
        assertTrue(subscription.isCancelled)
        assertEquals(PeriodType.MONTHLY, subscription.periodType)
    }

    @Test
    fun `сводка показывает ближайшее списание`() {
        assertEquals(LocalDate.parse("2026-10-07"), filled.nextCharge(today))
        assertFalse(filled.startsInFuture(today))
    }

    @Test
    fun `подписка превращается обратно в форму`() {
        val subscription = filled.toSubscription(null, today)!!
        assertEquals("399", subscription.toForm().price)
        assertEquals("07.03.2025", subscription.toForm().startedAt)
        assertEquals("399,90", subscription.copy(priceMinor = 39_990).toForm().price)
        assertEquals("399,05", subscription.copy(priceMinor = 39_905).toForm().price)
    }
}
