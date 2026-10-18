package ru.pikahk.outdateapp.ui.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ru.pikahk.outdateapp.data.AuthProvider
import ru.pikahk.outdateapp.data.repository.AuthRepository

class ResetRequestViewModel(private val auth: AuthRepository, email: String) : AuthFormViewModel<String>() {

    var email by mutableStateOf(email)
        private set

    fun updateEmail(value: String) {
        email = value
        error = null
    }

    fun submit() {
        if (email.isBlank()) return
        emailError(email)?.let {
            error = it
            return
        }
        val address = email.trim()
        request {
            auth.requestPasswordReset(address)
            outcome = address
        }
    }

    companion object {
        fun factory(context: Context, email: String) = viewModelFactory {
            initializer { ResetRequestViewModel(AuthProvider.get(context), email) }
        }
    }
}

class ResetPasswordViewModel(private val auth: AuthRepository, val email: String) : CodeFormViewModel<Unit>() {

    var password by mutableStateOf("")
        private set

    val canSubmit: Boolean get() = code.length == CODE_LENGTH && password.isNotEmpty()

    init {
        startTimer()
    }

    fun updatePassword(value: String) {
        password = value
        error = null
    }

    fun submit() {
        if (!canSubmit) return
        passwordError(password)?.let {
            error = it
            return
        }
        request {
            auth.resetPassword(email, code, password)
            outcome = Unit
        }
    }

    fun resend() {
        if (resendIn > 0) return
        request {
            auth.requestPasswordReset(email)
            clearCode()
            startTimer()
        }
    }

    companion object {
        fun factory(context: Context, email: String) = viewModelFactory {
            initializer { ResetPasswordViewModel(AuthProvider.get(context), email) }
        }
    }
}
