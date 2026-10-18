package ru.pikahk.outdateapp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.pikahk.outdateapp.data.AuthProvider
import ru.pikahk.outdateapp.data.repository.AuthRepository
import ru.pikahk.outdateapp.data.repository.SettingsRepository
import ru.pikahk.outdateapp.data.repository.ThemeMode

enum class StartScreen { SIGN_IN, VERIFY_EMAIL, MAIN }

class MainViewModel(private val settings: SettingsRepository, auth: AuthRepository) : ViewModel() {

    val theme: StateFlow<ThemeMode?> = settings.settings
        .map { it.theme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val start: StateFlow<StartScreen?> = flow {
        val session = auth.session.first()
        val screen = when {
            session != null -> if (session.emailVerified) StartScreen.MAIN else StartScreen.VERIFY_EMAIL
            settings.authPromptDone.first() -> StartScreen.MAIN
            else -> StartScreen.SIGN_IN
        }
        emit(screen)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch { auth.refresh() }
    }

    fun finishAuthPrompt() {
        viewModelScope.launch { settings.markAuthPromptDone() }
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer {
                MainViewModel(SettingsRepository(context), AuthProvider.get(context))
            }
        }
    }
}
