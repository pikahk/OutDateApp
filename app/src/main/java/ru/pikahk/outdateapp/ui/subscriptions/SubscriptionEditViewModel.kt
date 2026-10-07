package ru.pikahk.outdateapp.ui.subscriptions

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.pikahk.outdateapp.data.DatabaseProvider
import ru.pikahk.outdateapp.data.model.Subscription
import ru.pikahk.outdateapp.data.repository.SettingsRepository
import ru.pikahk.outdateapp.data.repository.SubscriptionRepository

class SubscriptionEditViewModel(
    private val repository: SubscriptionRepository,
    settings: SettingsRepository,
    private val id: String?
) : ViewModel() {

    val isNew = id == null

    var form by mutableStateOf<SubscriptionForm?>(null)
        private set

    var isCancelled by mutableStateOf(false)
        private set

    var finished by mutableStateOf(false)
        private set

    private var existing: Subscription? = null

    init {
        viewModelScope.launch {
            val subscription = id?.let { repository.find(it) }
            existing = subscription
            isCancelled = subscription?.isCancelled == true
            form = subscription?.toForm()
                ?: SubscriptionForm(notifyDaysBefore = settings.settings.first().defaultNotifyDaysBefore)
        }
    }

    fun update(form: SubscriptionForm) {
        this.form = form
    }

    fun save() {
        val subscription = form?.toSubscription(existing, LocalDate.now()) ?: return
        viewModelScope.launch {
            repository.save(subscription)
            finished = true
        }
    }

    fun toggleCancelled() {
        val subscriptionId = id ?: return
        viewModelScope.launch {
            repository.setCancelled(subscriptionId, !isCancelled)
            finished = true
        }
    }

    fun delete() {
        val subscriptionId = id ?: return
        viewModelScope.launch {
            repository.markDeleted(subscriptionId)
            finished = true
        }
    }

    companion object {
        fun factory(context: Context, id: String?) = viewModelFactory {
            initializer {
                SubscriptionEditViewModel(
                    repository = SubscriptionRepository(DatabaseProvider.get(context).subscriptionDao()),
                    settings = SettingsRepository(context),
                    id = id
                )
            }
        }
    }
}
