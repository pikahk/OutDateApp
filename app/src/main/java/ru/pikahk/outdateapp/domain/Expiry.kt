package ru.pikahk.outdateapp.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import ru.pikahk.outdateapp.data.Item

enum class Urgency { EXPIRED, CRITICAL, SOON, OK }

fun effectiveExpiryDate(item: Item): LocalDate {
    if (item.daysAfterOpening == null || item.openedAt == null) return item.expiresAt
    return minOf(item.openedAt.plusDays(item.daysAfterOpening.toLong()), item.expiresAt)
}

fun daysLeft(item: Item, today: LocalDate): Long = ChronoUnit.DAYS.between(today, effectiveExpiryDate(item))

fun urgency(daysLeft: Long): Urgency = when {
    daysLeft < 0 -> Urgency.EXPIRED
    daysLeft <= 1 -> Urgency.CRITICAL
    daysLeft <= 7 -> Urgency.SOON
    else -> Urgency.OK
}
