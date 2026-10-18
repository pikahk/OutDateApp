package ru.pikahk.outdateapp.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.pikahk.outdateapp.data.remote.ApiException

class AuthErrorsTest {

    @Test
    fun `правильная почта проходит проверку`() {
        assertNull(emailError("misha@mail.ru"))
        assertNull(emailError("  misha@mail.ru  "))
    }

    @Test
    fun `почта без собаки или домена не проходит проверку`() {
        for (wrong in listOf("misha", "misha@", "misha@mail", "mi sha@mail.ru", "")) {
            assertEquals(wrong, AuthError.INVALID_EMAIL, emailError(wrong))
        }
    }

    @Test
    fun `пароль должен быть от 8 до 64 символов`() {
        assertEquals(AuthError.INVALID_PASSWORD, passwordError("a".repeat(7)))
        assertNull(passwordError("a".repeat(8)))
        assertNull(passwordError("a".repeat(64)))
        assertEquals(AuthError.INVALID_PASSWORD, passwordError("a".repeat(65)))
    }

    @Test
    fun `ответы сервера превращаются в ошибки у нужных полей`() {
        assertEquals(AuthField.EMAIL, authError("email_taken").field)
        assertEquals(AuthField.PASSWORD, authError("invalid_credentials").field)
        assertEquals(AuthField.CODE, authError("code_expired").field)
        assertEquals(AuthError.NETWORK, authError(ApiException.NETWORK))
        assertEquals(AuthError.SERVER, authError("something_new"))
    }
}
