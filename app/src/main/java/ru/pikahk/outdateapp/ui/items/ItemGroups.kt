package ru.pikahk.outdateapp.ui.items

import java.time.LocalDate
import ru.pikahk.outdateapp.data.model.Item
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.domain.daysLeft
import ru.pikahk.outdateapp.domain.effectiveExpiryDate
import ru.pikahk.outdateapp.domain.urgency
import ru.pikahk.outdateapp.ui.CategoryUi

fun List<Item>.toItemGroups(today: LocalDate, categories: List<CategoryUi>): List<ItemsGroup> {
    val categoriesById = categories.associateBy { it.id }
    return map { item ->
        val left = daysLeft(item, today)
        ItemUi(
            id = item.id,
            name = item.name,
            daysLeft = left,
            urgency = urgency(left),
            openedAt = item.openedAt,
            limitedByOpening = effectiveExpiryDate(item) < item.expiresAt,
            category = item.categoryId?.let { categoriesById[it] },
            addedBy = item.addedBy
        )
    }
        .groupBy { it.urgency.toSection() }
        .map { (section, sectionItems) -> ItemsGroup(section, sectionItems) }
        .sortedBy { it.section }
}

private fun Urgency.toSection(): ItemsSection = when (this) {
    Urgency.EXPIRED -> ItemsSection.EXPIRED
    Urgency.CRITICAL, Urgency.SOON -> ItemsSection.SOON
    Urgency.OK -> ItemsSection.LATER
}
