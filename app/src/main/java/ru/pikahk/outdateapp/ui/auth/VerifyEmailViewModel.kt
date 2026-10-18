package ru.pikahk.outdateapp.ui.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.pikahk.outdateapp.data.AuthProvider
import ru.pikahk.outdateapp.data.repository.AuthRepository

class VerifyEmailViewModel(private val auth: AuthRepository, codeSent: Boolean) : CodeFormViewModel<Unit>() {

    var email by mutableStateOf("")
        private set

    var signedOut by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch { email = auth.session.first()?.email.orEmpty() }
        if (codeSent) startTimer()
    }

    override fun updateCode(value: String) {
        super.updateCode(value)
        if (code.length == CODE_LENGTH) submit()
    }

    fun submit() {
        if (code.length != CODE_LENGTH) return
        request {
            auth.verifyEmail(code)
            outcome = Unit
        }
    }

    fun resend() {
        if (resendIn > 0) return
        request(onError = { if (it == AuthError.TOO_SOON) startTimer() }) {
            auth.resendCode()
            clearCode()
            startTimer()
        }
    }

    fun changeEmail() {
        viewModelScope.launch {
            auth.logout()
            signedOut = true
        }
    }

    companion object {
        fun factory(context: Context, codeSent: Boolean) = viewModelFactory {
            initializer { VerifyEmailViewModel(AuthProvider.get(context), codeSent) }
        }
    }
}
