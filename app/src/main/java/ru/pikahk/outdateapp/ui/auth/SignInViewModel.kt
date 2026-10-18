package ru.pikahk.outdateapp.ui.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ru.pikahk.outdateapp.data.AuthProvider
import ru.pikahk.outdateapp.data.model.Session
import ru.pikahk.outdateapp.data.repository.AuthRepository

class SignInViewModel(private val auth: AuthRepository, email: String) : AuthFormViewModel<Session>() {

    var email by mutableStateOf(email)
        private set

    var password by mutableStateOf("")
        private set

    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotEmpty()

    fun updateEmail(value: String) {
        email = value
        error = null
    }

    fun updatePassword(value: String) {
        password = value
        error = null
    }

    fun submit() {
        if (!canSubmit) return
        emailError(email)?.let {
            error = it
            return
        }
        request { outcome = auth.login(email.trim(), password) }
    }

    companion object {
        fun factory(context: Context, email: String) = viewModelFactory {
            initializer { SignInViewModel(AuthProvider.get(context), email) }
        }
    }
}
