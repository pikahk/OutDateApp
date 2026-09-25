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
import ru.pikahk.outdateapp.domain.daysLeft
import ru.pikahk.outdateapp.domain.urgency

class ItemsViewModel(private val repository: ItemRepository) : ViewModel() {

    val items: StateFlow<List<ItemUi>> =
        repository.observeAll()
            .map { list -> list.toUiModels(LocalDate.now()) }
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
            urgency = urgency(left)
        )
    }.sortedBy { it.daysLeft }

    fun addItem(name: String, expiresAt: LocalDate) {
        viewModelScope.launch {
            repository.save(
                Item(
                    name = name,
                    categoryId = null,
                    barcode = null,
                    expiresAt = expiresAt,
                    daysAfterOpening = null,
                    openedAt = null
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
