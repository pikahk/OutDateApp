package ru.pikahk.outdateapp.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.LocalTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.pikahk.outdateapp.data.AppSettings
import ru.pikahk.outdateapp.data.SettingsRepository
import ru.pikahk.outdateapp.data.ThemeMode
import ru.pikahk.outdateapp.notifications.ExpiryWorker

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val rescheduleReminders: (LocalTime) -> Unit
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun setReminderTime(time: LocalTime) {
        viewModelScope.launch {
            repository.setReminderTime(time)
            rescheduleReminders(time)
        }
    }

    fun setDefaultNotifyDaysBefore(days: Int) {
        viewModelScope.launch { repository.setDefaultNotifyDaysBefore(days) }
    }

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch { repository.setTheme(theme) }
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
                SettingsViewModel(
                    repository = SettingsRepository(appContext),
                    rescheduleReminders = { time -> ExpiryWorker.reschedule(appContext, time) }
                )
            }
        }
    }
}
