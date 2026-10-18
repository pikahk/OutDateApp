package ru.pikahk.outdateapp.ui.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ru.pikahk.outdateapp.data.AuthProvider
import ru.pikahk.outdateapp.data.repository.AuthRepository

class RegisterViewModel(private val auth: AuthRepository) : AuthFormViewModel<Unit>() {

    var email by mutableStateOf("")
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
        val invalid = emailError(email) ?: passwordError(password)
        if (invalid != null) {
            error = invalid
            return
        }
        request {
            auth.register(email.trim(), password)
            outcome = Unit
        }
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer { RegisterViewModel(AuthProvider.get(context)) }
        }
    }
}
