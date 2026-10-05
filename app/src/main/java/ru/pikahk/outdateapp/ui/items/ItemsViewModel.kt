package ru.pikahk.outdateapp.ui.items

import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.text.Collator
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pikahk.outdateapp.data.Category
import ru.pikahk.outdateapp.data.CategoryRepository
import ru.pikahk.outdateapp.data.DatabaseProvider
import ru.pikahk.outdateapp.data.Item
import ru.pikahk.outdateapp.data.ItemRepository
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.domain.daysLeft
import ru.pikahk.outdateapp.domain.effectiveExpiryDate
import ru.pikahk.outdateapp.domain.urgency
import ru.pikahk.outdateapp.ui.CategoryUi
import ru.pikahk.outdateapp.ui.add.ItemDraft
import ru.pikahk.outdateapp.ui.toUi

class ItemsViewModel(private val items: ItemRepository, private val categories: CategoryRepository) : ViewModel() {

    val query = TextFieldState()

    private val filter = MutableStateFlow(ItemsFilter())

    private val collator = Collator.getInstance()

    val state: StateFlow<ItemsUiState> =
        combine(
            items.observeAll(),
            categories.observeAll(),
            filter,
            snapshotFlow { query.text.toString().trim() }
        ) { itemList, categoryList, current, search ->
            val categoryUis = categoryList.map { it.toUi() }
            val selected = current.categoryId?.takeIf { id -> categoryUis.any { it.id == id } }
            val visible = itemList
                .filter { item ->
                    (selected == null || item.categoryId == selected) &&
                        current.opened.matches(item) &&
                        item.name.contains(search, ignoreCase = true)
                }
                .sortedWith(current.sort.comparator())
            ItemsUiState(
                isLoading = false,
                categories = categoryUis,
                selectedCategoryId = selected,
                sort = current.sort,
                opened = current.opened,
                isSearching = search.isNotEmpty(),
                groups = visible.toUiModels(LocalDate.now(), categoryUis).toGroups(),
                hasItems = itemList.isNotEmpty()
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ItemsUiState()
        )

    init {
        viewModelScope.launch { categories.addDefaults() }
    }

    fun selectCategory(id: String?) {
        filter.update { it.copy(categoryId = id) }
    }

    fun selectSort(sort: ItemsSort) {
        filter.update { it.copy(sort = sort) }
    }

    fun selectOpened(opened: OpenedFilter) {
        filter.update { it.copy(opened = opened) }
    }

    fun createCategory(name: String): String {
        val category = Category(name = name.trim(), isDefault = false)
        viewModelScope.launch { categories.save(category) }
        return category.id
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch { categories.delete(id) }
    }

    fun addItem(draft: ItemDraft) {
        viewModelScope.launch {
            items.save(
                Item(
                    name = draft.name,
                    categoryId = draft.categoryId,
                    barcode = null,
                    expiresAt = draft.expiresAt,
                    daysAfterOpening = draft.daysAfterOpening,
                    openedAt = draft.openedAt,
                    notifyDaysBefore = draft.notifyDaysBefore
                )
            )
        }
    }

    private fun OpenedFilter.matches(item: Item): Boolean = when (this) {
        OpenedFilter.ALL -> true
        OpenedFilter.OPENED -> item.openedAt != null
        OpenedFilter.SEALED -> item.openedAt == null
    }

    private fun ItemsSort.comparator(): Comparator<Item> = when (this) {
        ItemsSort.EXPIRY -> compareBy { effectiveExpiryDate(it) }
        ItemsSort.NAME -> compareBy(collator) { it.name }
        ItemsSort.ADDED -> compareByDescending { it.createdAt }
    }

    private fun List<Item>.toUiModels(today: LocalDate, categoryUis: List<CategoryUi>): List<ItemUi> {
        val categoriesById = categoryUis.associateBy { it.id }
        return map { item ->
            val left = daysLeft(item, today)
            ItemUi(
                id = item.id,
                name = item.name,
                daysLeft = left,
                urgency = urgency(left),
                openedAt = item.openedAt,
                limitedByOpening = effectiveExpiryDate(item) < item.expiresAt,
                category = item.categoryId?.let { categoriesById[it] }
            )
        }
    }

    private fun List<ItemUi>.toGroups(): List<ItemsGroup> = groupBy { it.urgency.toSection() }
        .map { (section, sectionItems) -> ItemsGroup(section, sectionItems) }
        .sortedBy { it.section }

    private fun Urgency.toSection(): ItemsSection = when (this) {
        Urgency.EXPIRED -> ItemsSection.EXPIRED
        Urgency.CRITICAL, Urgency.SOON -> ItemsSection.SOON
        Urgency.OK -> ItemsSection.LATER
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer {
                val database = DatabaseProvider.get(context)
                ItemsViewModel(ItemRepository(database.itemDao()), CategoryRepository(database.categoryDao()))
            }
        }
    }
}

private data class ItemsFilter(
    val categoryId: String? = null,
    val sort: ItemsSort = ItemsSort.EXPIRY,
    val opened: OpenedFilter = OpenedFilter.ALL
)
