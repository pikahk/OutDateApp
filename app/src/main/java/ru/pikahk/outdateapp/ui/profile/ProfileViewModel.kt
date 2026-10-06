package ru.pikahk.outdateapp.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.pikahk.outdateapp.data.DatabaseProvider
import ru.pikahk.outdateapp.data.ItemRepository
import ru.pikahk.outdateapp.domain.ItemStats
import ru.pikahk.outdateapp.domain.itemStats

class ProfileViewModel(items: ItemRepository) : ViewModel() {

    private val today = LocalDate.now()
    private val zone = ZoneId.systemDefault()

    val month: YearMonth = YearMonth.from(today)

    val stats: StateFlow<ItemStats?> =
        combine(
            items.observeAll(),
            items.observeDeletedSince(month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli())
        ) { active, deleted ->
            itemStats(active, deleted, today, zone)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer {
                ProfileViewModel(ItemRepository(DatabaseProvider.get(context).itemDao()))
            }
        }
    }
}
