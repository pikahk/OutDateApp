package ru.pikahk.outdateapp.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong
import ru.pikahk.outdateapp.data.model.PeriodType
import ru.pikahk.outdateapp.data.model.Subscription

private const val MONTHS_IN_YEAR = 12
private const val AVERAGE_MONTH_DAYS = 365.25 / MONTHS_IN_YEAR

data class UpcomingCharge(val subscription: Subscription, val date: LocalDate)

fun nextChargeDate(subscription: Subscription, today: LocalDate): LocalDate {
    var count = maxOf(1L, periodsBetween(subscription, today))
    var date = chargeDate(subscription, count)
    while (date < today) {
        count++
        date = chargeDate(subscription, count)
    }
    return date
}

fun upcomingCharges(subscriptions: List<Subscription>, today: LocalDate): List<UpcomingCharge> = subscriptions
    .filter { !it.isCancelled && !it.isDeleted }
    .map { UpcomingCharge(it, nextChargeDate(it, today)) }
    .filter { ChronoUnit.DAYS.between(today, it.date) in 0..it.subscription.notifyDaysBefore }
    .sortedBy { it.date }

fun monthlyTotalMinor(subscriptions: List<Subscription>): Long = subscriptions
    .filter { !it.isCancelled && !it.isDeleted }
    .sumOf { monthlyCostMinor(it) }
    .roundToLong()

private fun monthlyCostMinor(subscription: Subscription): Double = when (subscription.periodType) {
    PeriodType.MONTHLY -> subscription.priceMinor.toDouble()
    PeriodType.YEARLY -> subscription.priceMinor.toDouble() / MONTHS_IN_YEAR
    PeriodType.CUSTOM_DAYS -> subscription.priceMinor * AVERAGE_MONTH_DAYS / periodDays(subscription)
}

private fun periodsBetween(subscription: Subscription, today: LocalDate): Long {
    val start = subscription.startedAt
    return when (subscription.periodType) {
        PeriodType.MONTHLY -> ChronoUnit.MONTHS.between(start, today)
        PeriodType.YEARLY -> ChronoUnit.YEARS.between(start, today)
        PeriodType.CUSTOM_DAYS -> ChronoUnit.DAYS.between(start, today) / periodDays(subscription)
    }
}

private fun chargeDate(subscription: Subscription, count: Long): LocalDate {
    val start = subscription.startedAt
    return when (subscription.periodType) {
        PeriodType.MONTHLY -> start.plusMonths(count)
        PeriodType.YEARLY -> start.plusYears(count)
        PeriodType.CUSTOM_DAYS -> start.plusDays(count * periodDays(subscription))
    }
}

private fun periodDays(subscription: Subscription): Int = (subscription.periodDays ?: 1).coerceAtLeast(1)
