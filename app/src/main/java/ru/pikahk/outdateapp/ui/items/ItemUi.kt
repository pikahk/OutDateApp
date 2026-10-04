package ru.pikahk.outdateapp.ui.items

import java.time.LocalDate
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.ui.CategoryUi

data class ItemUi(
    val id: String,
    val name: String,
    val daysLeft: Long,
    val urgency: Urgency,
    val openedAt: LocalDate?,
    val limitedByOpening: Boolean,
    val category: CategoryUi?
)

enum class ItemsSection { EXPIRED, SOON, LATER }

data class ItemsGroup(val section: ItemsSection, val items: List<ItemUi>)

enum class ItemsSort { EXPIRY, NAME, ADDED }

enum class OpenedFilter { ALL, OPENED, SEALED }

data class ItemsUiState(
    val isLoading: Boolean = true,
    val categories: List<CategoryUi> = emptyList(),
    val selectedCategoryId: String? = null,
    val sort: ItemsSort = ItemsSort.EXPIRY,
    val opened: OpenedFilter = OpenedFilter.ALL,
    val isSearching: Boolean = false,
    val groups: List<ItemsGroup> = emptyList(),
    val hasItems: Boolean = false
)
