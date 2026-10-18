package ru.pikahk.outdateapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pikahk.outdateapp.data.model.Session

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class DataStoreSessionStore(context: Context) : SessionStore {

    private val store = context.applicationContext.sessionStore

    override val session: Flow<Session?> = store.data.map { prefs ->
        val token = prefs[TOKEN] ?: return@map null
        Session(
            token = token,
            email = prefs[EMAIL].orEmpty(),
            emailVerified = prefs[EMAIL_VERIFIED] ?: false
        )
    }

    override suspend fun save(session: Session?) {
        store.edit { prefs ->
            if (session == null) {
                prefs.clear()
            } else {
                prefs[TOKEN] = session.token
                prefs[EMAIL] = session.email
                prefs[EMAIL_VERIFIED] = session.emailVerified
            }
        }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val EMAIL = stringPreferencesKey("email")
        val EMAIL_VERIFIED = booleanPreferencesKey("email_verified")
    }
}
