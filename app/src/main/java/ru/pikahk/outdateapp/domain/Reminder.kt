package ru.pikahk.outdateapp.domain

import java.time.LocalDate
import ru.pikahk.outdateapp.data.Item

const val REMINDER_OFF = -1

fun itemsToRemind(items: List<Item>, today: LocalDate): List<Item> = items
    .filter { !it.isDeleted && daysLeft(it, today) in 0..it.notifyDaysBefore }
    .sortedBy { effectiveExpiryDate(it) }
