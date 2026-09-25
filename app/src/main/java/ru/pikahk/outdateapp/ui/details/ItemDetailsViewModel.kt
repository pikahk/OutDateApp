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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.pikahk.outdateapp.data.DatabaseProvider
import ru.pikahk.outdateapp.data.Item
import ru.pikahk.outdateapp.data.ItemRepository
import ru.pikahk.outdateapp.domain.daysLeft
import ru.pikahk.outdateapp.domain.effectiveExpiryDate
import ru.pikahk.outdateapp.domain.urgency

class ItemDetailsViewModel(private val repository: ItemRepository, private val itemId: String) : ViewModel() {

    val state: StateFlow<ItemDetailsState> =
        repository.observeById(itemId)
            .map { item ->
                if (item == null) ItemDetailsState.Gone else ItemDetailsState.Loaded(item.toDetailsUi(LocalDate.now()))
            }
            .stateIn(
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

    private fun Item.toDetailsUi(today: LocalDate): ItemDetailsUi {
        val left = daysLeft(this, today)
        val effective = effectiveExpiryDate(this)
        return ItemDetailsUi(
            name = name,
            expiresAt = expiresAt,
            openedAt = openedAt,
            daysAfterOpening = daysAfterOpening,
            effectiveExpiresAt = effective,
            daysLeft = left,
            urgency = urgency(left),
            limitedByOpening = effective < expiresAt,
            openingExpiresAt = daysAfterOpening?.let { days -> openedAt?.plusDays(days.toLong()) },
            expiresIfOpenedToday = daysAfterOpening?.let { effectiveExpiryDate(copy(openedAt = today)) },
            createdAt = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        )
    }

    companion object {
        fun factory(context: Context, itemId: String) = viewModelFactory {
            initializer {
                val dao = DatabaseProvider.get(context).itemDao()
                ItemDetailsViewModel(ItemRepository(dao), itemId)
            }
        }
    }
}
