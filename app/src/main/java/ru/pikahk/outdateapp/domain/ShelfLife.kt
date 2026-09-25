package ru.pikahk.outdateapp.domain

import java.time.LocalDate

enum class ShelfLifeUnit { HOURS, DAYS, MONTHS, YEARS }

fun expiryFromProduction(producedAt: LocalDate, amount: Int, unit: ShelfLifeUnit): LocalDate = when (unit) {
    ShelfLifeUnit.HOURS -> producedAt.plusDays((amount / 24).toLong())
    ShelfLifeUnit.DAYS -> producedAt.plusDays(amount.toLong())
    ShelfLifeUnit.MONTHS -> producedAt.plusMonths(amount.toLong())
    ShelfLifeUnit.YEARS -> producedAt.plusYears(amount.toLong())
}
