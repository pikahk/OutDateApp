package ru.pikahk.outdateapp.domain

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.pikahk.outdateapp.data.model.Item

class ItemStatsTest {

    private val zone = ZoneOffset.UTC
    private val today: LocalDate = LocalDate.parse("2026-10-15")

    private fun item(expiresAt: String, deletedOn: String? = null) = Item(
        name = "Тестовый расходник",
        categoryId = null,
        barcode = null,
        expiresAt = LocalDate.parse(expiresAt),
        daysAfterOpening = null,
        openedAt = null,
        updatedAt = deletedOn?.let { LocalDate.parse(it).atStartOfDay(zone).toInstant().toEpochMilli() } ?: 0,
        isDeleted = deletedOn != null
    )

    @Test
    fun `считает отслеживаемые, истекающие за неделю и просроченные`() {
        val active = listOf(
            item("2026-10-15"),
            item("2026-10-22"),
            item("2026-10-23"),
            item("2026-10-14")
        )
        val stats = itemStats(active, emptyList(), today, zone)
        assertEquals(4, stats.tracked)
        assertEquals(2, stats.expiringThisWeek)
        assertEquals(1, stats.expired)
    }

    @Test
    fun `удалённое до конца срока считается использованным, после — выброшенным`() {
        val deleted = listOf(
            item("2026-10-20", deletedOn = "2026-10-10"),
            item("2026-10-05", deletedOn = "2026-10-08")
        )
        val stats = itemStats(emptyList(), deleted, today, zone)
        assertEquals(1, stats.usedThisMonth)
        assertEquals(1, stats.wastedThisMonth)
    }

    @Test
    fun `удалённое в последний день срока считается использованным`() {
        val stats = itemStats(emptyList(), listOf(item("2026-10-10", deletedOn = "2026-10-10")), today, zone)
        assertEquals(1, stats.usedThisMonth)
        assertEquals(0, stats.wastedThisMonth)
    }

    @Test
    fun `удалённое в прошлом месяце не учитывается`() {
        val stats = itemStats(emptyList(), listOf(item("2026-09-20", deletedOn = "2026-09-25")), today, zone)
        assertEquals(0, stats.usedThisMonth)
        assertEquals(0, stats.wastedThisMonth)
    }
}
