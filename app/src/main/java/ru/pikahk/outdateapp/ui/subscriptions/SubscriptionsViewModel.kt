package ru.pikahk.outdateapp.ui.subscriptions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.text.Collator
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.pikahk.outdateapp.data.DatabaseProvider
import ru.pikahk.outdateapp.data.model.Subscription
import ru.pikahk.outdateapp.data.repository.SubscriptionRepository
import ru.pikahk.outdateapp.domain.monthlyTotalMinor

class SubscriptionsViewModel(repository: SubscriptionRepository) : ViewModel() {

    private val collator = Collator.getInstance()

    val state: StateFlow<SubscriptionsUiState?> = repository.observeAll()
        .map { it.toUiState(LocalDate.now()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private fun List<Subscription>.toUiState(today: LocalDate): SubscriptionsUiState {
        val (cancelled, active) = partition { it.isCancelled }
        return SubscriptionsUiState(
            active = active.map { it.toUi(today) }.sortedBy { it.daysUntilCharge },
            cancelled = cancelled.map { it.toUi(today) }.sortedWith(compareBy(collator) { it.name }),
            monthlyTotalMinor = monthlyTotalMinor(this)
        )
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer {
                SubscriptionsViewModel(SubscriptionRepository(DatabaseProvider.get(context).subscriptionDao()))
            }
        }
    }
}
