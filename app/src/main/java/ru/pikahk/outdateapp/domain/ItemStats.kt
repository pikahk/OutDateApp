package ru.pikahk.outdateapp.domain

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import ru.pikahk.outdateapp.data.Item

private const val WEEK_DAYS = 7L

data class ItemStats(
    val tracked: Int,
    val expiringThisWeek: Int,
    val expired: Int,
    val usedThisMonth: Int,
    val wastedThisMonth: Int
)

fun itemStats(active: List<Item>, deleted: List<Item>, today: LocalDate, zone: ZoneId): ItemStats {
    val month = YearMonth.from(today)
    val removedThisMonth = deleted.filter { it.isDeleted && YearMonth.from(it.deletedOn(zone)) == month }
    val used = removedThisMonth.count { !it.deletedOn(zone).isAfter(effectiveExpiryDate(it)) }
    return ItemStats(
        tracked = active.size,
        expiringThisWeek = active.count { daysLeft(it, today) in 0..WEEK_DAYS },
        expired = active.count { daysLeft(it, today) < 0 },
        usedThisMonth = used,
        wastedThisMonth = removedThisMonth.size - used
    )
}

private fun Item.deletedOn(zone: ZoneId): LocalDate = Instant.ofEpochMilli(updatedAt).atZone(zone).toLocalDate()
