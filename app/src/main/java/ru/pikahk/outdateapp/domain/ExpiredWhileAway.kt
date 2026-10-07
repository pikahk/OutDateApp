package ru.pikahk.outdateapp.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import ru.pikahk.outdateapp.data.model.Item

const val AWAY_MIN_DAYS = 2L

fun expiredWhileAway(items: List<Item>, lastOpened: LocalDate, today: LocalDate): List<Item> {
    if (ChronoUnit.DAYS.between(lastOpened, today) < AWAY_MIN_DAYS) return emptyList()
    return items
        .filter { item -> !item.isDeleted && effectiveExpiryDate(item).let { it >= lastOpened && it < today } }
        .sortedBy { effectiveExpiryDate(it) }
}
