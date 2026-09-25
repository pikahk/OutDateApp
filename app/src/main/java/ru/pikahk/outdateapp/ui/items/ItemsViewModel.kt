package ru.pikahk.outdateapp.ui.items

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.pikahk.outdateapp.data.DatabaseProvider
import ru.pikahk.outdateapp.data.Item
import ru.pikahk.outdateapp.data.ItemRepository
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.domain.daysLeft
import ru.pikahk.outdateapp.domain.effectiveExpiryDate
import ru.pikahk.outdateapp.domain.urgency
import ru.pikahk.outdateapp.ui.add.ItemDraft

class ItemsViewModel(private val repository: ItemRepository) : ViewModel() {

    val groups: StateFlow<List<ItemsGroup>> =
        repository.observeAll()
            .map { list -> list.toUiModels(LocalDate.now()).toGroups() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private fun List<Item>.toUiModels(today: LocalDate): List<ItemUi> = map { item ->
        val left = daysLeft(item, today)
        ItemUi(
            id = item.id,
            name = item.name,
            daysLeft = left,
            urgency = urgency(left),
            openedAt = item.openedAt,
            limitedByOpening = effectiveExpiryDate(item) < item.expiresAt
        )
    }.sortedBy { it.daysLeft }

    private fun List<ItemUi>.toGroups(): List<ItemsGroup> = groupBy { it.urgency.toSection() }
        .map { (section, items) -> ItemsGroup(section, items) }
        .sortedBy { it.section }

    private fun Urgency.toSection(): ItemsSection = when (this) {
        Urgency.EXPIRED -> ItemsSection.EXPIRED
        Urgency.CRITICAL, Urgency.SOON -> ItemsSection.SOON
        Urgency.OK -> ItemsSection.LATER
    }

    fun addItem(draft: ItemDraft) {
        viewModelScope.launch {
            repository.save(
                Item(
                    name = draft.name,
                    categoryId = null,
                    barcode = null,
                    expiresAt = draft.expiresAt,
                    daysAfterOpening = draft.daysAfterOpening,
                    openedAt = draft.openedAt
                )
            )
        }
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer {
                val dao = DatabaseProvider.get(context).itemDao()
                ItemsViewModel(ItemRepository(dao))
            }
        }
    }
}
