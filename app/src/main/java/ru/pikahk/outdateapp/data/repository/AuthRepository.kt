package ru.pikahk.outdateapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import ru.pikahk.outdateapp.data.model.Session
import ru.pikahk.outdateapp.data.remote.ApiException
import ru.pikahk.outdateapp.data.remote.OutDateApi

class AuthRepository(private val api: OutDateApi, private val store: SessionStore) {

    val session: Flow<Session?> = store.session

    suspend fun register(email: String, password: String) {
        val response = api.register(email, password)
        store.save(Session(response.token, response.email, emailVerified = false))
    }

    suspend fun login(email: String, password: String): Session {
        val response = api.login(email, password)
        val me = api.me(response.token)
        val session = Session(response.token, me.email, me.emailVerified)
        store.save(session)
        return session
    }

    suspend fun verifyEmail(code: String) {
        val session = requireSession()
        api.verifyEmail(session.token, code)
        store.save(session.copy(emailVerified = true))
    }

    suspend fun resendCode() {
        api.resendCode(requireSession().token)
    }

    suspend fun requestPasswordReset(email: String) {
        api.forgotPassword(email)
    }

    suspend fun resetPassword(email: String, code: String, password: String) {
        val response = api.resetPassword(email, code, password)
        store.save(Session(response.token, response.email, emailVerified = true))
    }

    suspend fun refresh() {
        val session = store.session.first() ?: return
        try {
            val me = api.me(session.token)
            store.save(session.copy(email = me.email, emailVerified = me.emailVerified))
        } catch (e: ApiException) {
            if (e.code == ApiException.UNAUTHORIZED) store.save(null)
        }
    }

    suspend fun logout() {
        val session = store.session.first() ?: return
        store.save(null)
        runCatching { api.logout(session.token) }
    }

    private suspend fun requireSession(): Session =
        store.session.first() ?: throw ApiException(ApiException.UNAUTHORIZED)
}
