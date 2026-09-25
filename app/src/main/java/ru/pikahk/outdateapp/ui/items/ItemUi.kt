package ru.pikahk.outdateapp.ui.items

import java.time.LocalDate
import ru.pikahk.outdateapp.domain.Urgency

data class ItemUi(
    val id: String,
    val name: String,
    val daysLeft: Long,
    val urgency: Urgency,
    val openedAt: LocalDate?,
    val limitedByOpening: Boolean
)

enum class ItemsSection { EXPIRED, SOON, LATER }

data class ItemsGroup(val section: ItemsSection, val items: List<ItemUi>)
