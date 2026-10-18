package ru.pikahk.outdateapp.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.pikahk.outdateapp.data.remote.ApiException

private const val RESEND_SECONDS = 60
private const val SECOND_MS = 1_000L

abstract class AuthFormViewModel<T : Any> : ViewModel() {

    var error by mutableStateOf<AuthError?>(null)
        protected set

    var loading by mutableStateOf(false)
        private set

    var outcome by mutableStateOf<T?>(null)
        protected set

    fun consumeOutcome() {
        outcome = null
    }

    protected fun request(onError: (AuthError) -> Unit = {}, block: suspend () -> Unit) {
        if (loading) return
        error = null
        loading = true
        viewModelScope.launch {
            try {
                block()
            } catch (e: ApiException) {
                val failure = authError(e.code)
                error = failure
                onError(failure)
            } finally {
                loading = false
            }
        }
    }
}

abstract class CodeFormViewModel<T : Any> : AuthFormViewModel<T>() {

    var code by mutableStateOf("")
        private set

    var resendIn by mutableIntStateOf(0)
        private set

    private var timer: Job? = null

    open fun updateCode(value: String) {
        code = value.filter(Char::isDigit).take(CODE_LENGTH)
        error = null
    }

    protected fun clearCode() {
        code = ""
    }

    protected fun startTimer() {
        timer?.cancel()
        resendIn = RESEND_SECONDS
        timer = viewModelScope.launch {
            while (resendIn > 0) {
                delay(SECOND_MS)
                resendIn--
            }
        }
    }
}
