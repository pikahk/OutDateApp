package ru.pikahk.outdateapp.ui.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.pikahk.outdateapp.data.AuthProvider
import ru.pikahk.outdateapp.data.repository.AuthRepository

class AccountViewModel(private val auth: AuthRepository) : ViewModel() {

    val email: StateFlow<String?> = auth.session
        .map { session -> session?.takeIf { it.emailVerified }?.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun logout() {
        viewModelScope.launch { auth.logout() }
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer { AccountViewModel(AuthProvider.get(context)) }
        }
    }
}
