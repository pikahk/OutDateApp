package ru.pikahk.outdateapp.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import ru.pikahk.outdateapp.data.Item

enum class Urgency { EXPIRED, CRITICAL, SOON, OK }

fun effectiveExpiryDate(item: Item): LocalDate =
    effectiveExpiryDate(item.expiresAt, item.openedAt, item.daysAfterOpening)

fun effectiveExpiryDate(expiresAt: LocalDate, openedAt: LocalDate?, daysAfterOpening: Int?): LocalDate {
    if (daysAfterOpening == null || openedAt == null) return expiresAt
    return minOf(openedAt.plusDays(daysAfterOpening.toLong()), expiresAt)
}

fun daysLeft(item: Item, today: LocalDate): Long = ChronoUnit.DAYS.between(today, effectiveExpiryDate(item))

fun urgency(daysLeft: Long): Urgency = when {
    daysLeft < 0 -> Urgency.EXPIRED
    daysLeft <= 1 -> Urgency.CRITICAL
    daysLeft <= 7 -> Urgency.SOON
    else -> Urgency.OK
}
