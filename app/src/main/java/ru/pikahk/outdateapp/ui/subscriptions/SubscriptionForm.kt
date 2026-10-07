package ru.pikahk.outdateapp.ui.subscriptions

import java.time.LocalDate
import ru.pikahk.outdateapp.data.model.PeriodType
import ru.pikahk.outdateapp.data.model.Subscription
import ru.pikahk.outdateapp.domain.DEFAULT_REMINDER_DAYS
import ru.pikahk.outdateapp.domain.formatDate
import ru.pikahk.outdateapp.domain.nextChargeDate
import ru.pikahk.outdateapp.domain.parseDate
import ru.pikahk.outdateapp.domain.parsePriceMinor

data class SubscriptionForm(
    val name: String = "",
    val price: String = "",
    val startedAt: String = "",
    val periodType: PeriodType = PeriodType.MONTHLY,
    val periodDays: String = "",
    val notifyDaysBefore: Int = DEFAULT_REMINDER_DAYS
) {
    val priceMinor: Long? get() = parsePriceMinor(price)

    val startDate: LocalDate? get() = parseDate(startedAt)

    val days: Int? get() = periodDays.toIntOrNull()?.takeIf { it > 0 }

    fun startsInFuture(today: LocalDate): Boolean = startDate?.isAfter(today) == true

    fun toSubscription(base: Subscription?, today: LocalDate): Subscription? {
        val price = priceMinor
        val start = validStart(today)
        if (name.isBlank() || price == null || start == null || !isPeriodValid()) return null
        val template = base ?: Subscription(
            name = name,
            priceMinor = price,
            startedAt = start,
            periodType = periodType,
            periodDays = null
        )
        return template.copy(
            name = name.trim(),
            priceMinor = price,
            startedAt = start,
            periodType = periodType,
            periodDays = customDays(),
            notifyDaysBefore = notifyDaysBefore
        )
    }

    fun nextCharge(today: LocalDate): LocalDate? {
        val start = validStart(today)
        if (start == null || !isPeriodValid()) return null
        val preview = Subscription(
            name = name,
            priceMinor = 0,
            startedAt = start,
            periodType = periodType,
            periodDays = customDays()
        )
        return nextChargeDate(preview, today)
    }

    private fun validStart(today: LocalDate): LocalDate? = startDate?.takeUnless { it.isAfter(today) }

    private fun isPeriodValid(): Boolean = periodType != PeriodType.CUSTOM_DAYS || days != null

    private fun customDays(): Int? = days.takeIf { periodType == PeriodType.CUSTOM_DAYS }
}

fun Subscription.toForm() = SubscriptionForm(
    name = name,
    price = priceInput(priceMinor),
    startedAt = formatDate(startedAt),
    periodType = periodType,
    periodDays = periodDays?.toString().orEmpty(),
    notifyDaysBefore = notifyDaysBefore
)

private fun priceInput(minor: Long): String {
    val rubles = minor / 100
    val kopecks = minor % 100
    return if (kopecks == 0L) rubles.toString() else "$rubles," + kopecks.toString().padStart(2, '0')
}
