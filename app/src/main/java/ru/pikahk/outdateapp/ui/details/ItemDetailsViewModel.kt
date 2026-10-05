package ru.pikahk.outdateapp.ui.details

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.pikahk.outdateapp.data.CategoryRepository
import ru.pikahk.outdateapp.data.DatabaseProvider
import ru.pikahk.outdateapp.data.Item
import ru.pikahk.outdateapp.data.ItemRepository
import ru.pikahk.outdateapp.domain.daysLeft
import ru.pikahk.outdateapp.domain.effectiveExpiryDate
import ru.pikahk.outdateapp.domain.urgency
import ru.pikahk.outdateapp.ui.CategoryUi
import ru.pikahk.outdateapp.ui.toUi

class ItemDetailsViewModel(
    private val repository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val itemId: String
) : ViewModel() {

    val state: StateFlow<ItemDetailsState> =
        combine(repository.observeById(itemId), categoryRepository.observeAll()) { item, categories ->
            if (item == null) {
                ItemDetailsState.Gone
            } else {
                val category = categories.firstOrNull { it.id == item.categoryId }?.toUi()
                ItemDetailsState.Loaded(item.toDetailsUi(LocalDate.now(), category))
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ItemDetailsState.Loading
        )

    fun markOpened() {
        viewModelScope.launch { repository.setOpenedAt(itemId, LocalDate.now()) }
    }

    fun markSealed() {
        viewModelScope.launch { repository.setOpenedAt(itemId, null) }
    }

    fun delete() {
        viewModelScope.launch { repository.markDeleted(itemId) }
    }

    private fun Item.toDetailsUi(today: LocalDate, category: CategoryUi?): ItemDetailsUi {
        val left = daysLeft(this, today)
        val effective = effectiveExpiryDate(this)
        return ItemDetailsUi(
            name = name,
            category = category,
            expiresAt = expiresAt,
            openedAt = openedAt,
            daysAfterOpening = daysAfterOpening,
            effectiveExpiresAt = effective,
            daysLeft = left,
            urgency = urgency(left),
            limitedByOpening = effective < expiresAt,
            expiresIfOpenedToday = daysAfterOpening?.let { effectiveExpiryDate(copy(openedAt = today)) },
            createdAt = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate(),
            notifyDaysBefore = notifyDaysBefore
        )
    }

    companion object {
        fun factory(context: Context, itemId: String) = viewModelFactory {
            initializer {
                val database = DatabaseProvider.get(context)
                ItemDetailsViewModel(
                    ItemRepository(database.itemDao()),
                    CategoryRepository(database.categoryDao()),
                    itemId
                )
            }
        }
    }
}
