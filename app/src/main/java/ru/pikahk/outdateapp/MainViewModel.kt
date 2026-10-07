package ru.pikahk.outdateapp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.pikahk.outdateapp.data.repository.SettingsRepository
import ru.pikahk.outdateapp.data.repository.ThemeMode

class MainViewModel(settings: SettingsRepository) : ViewModel() {

    val theme: StateFlow<ThemeMode?> = settings.settings
        .map { it.theme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer {
                MainViewModel(SettingsRepository(context))
            }
        }
    }
}
