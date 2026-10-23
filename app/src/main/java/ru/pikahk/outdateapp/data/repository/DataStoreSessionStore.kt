package ru.pikahk.outdateapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import ru.pikahk.outdateapp.data.model.Family
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

    override val syncVersion: Flow<Long> = store.data.map { it[SYNC_VERSION] ?: 0L }

    override suspend fun save(session: Session?) {
        store.edit { prefs ->
            if (session == null) {
                prefs.clear()
            } else {
                if (prefs[TOKEN] != session.token) {
                    prefs.remove(SYNC_VERSION)
                    prefs.removeFamily()
                }
                prefs[TOKEN] = session.token
                prefs[EMAIL] = session.email
                prefs[EMAIL_VERIFIED] = session.emailVerified
            }
        }
    }

    override suspend fun saveSyncVersion(version: Long) {
        store.edit { prefs ->
            if (prefs[TOKEN] != null) prefs[SYNC_VERSION] = version
        }
    }

    override val family: Flow<Family?> = store.data.map { prefs ->
        prefs[FAMILY]?.let { saved ->
            try {
                Json.decodeFromString<Family>(saved)
            } catch (e: SerializationException) {
                null
            }
        }
    }

    override val familyVersion: Flow<Long> = store.data.map { it[FAMILY_VERSION] ?: 0L }

    override suspend fun saveFamily(family: Family?) {
        store.edit { prefs ->
            if (family == null || prefs[TOKEN] == null) {
                prefs.removeFamily()
            } else {
                if (prefs[FAMILY_ID] != family.id) prefs.remove(FAMILY_VERSION)
                prefs[FAMILY_ID] = family.id
                prefs[FAMILY] = Json.encodeToString(family)
            }
        }
    }

    override suspend fun saveFamilyVersion(version: Long) {
        store.edit { prefs ->
            if (prefs[FAMILY] != null) prefs[FAMILY_VERSION] = version
        }
    }

    private fun MutablePreferences.removeFamily() {
        remove(FAMILY_ID)
        remove(FAMILY)
        remove(FAMILY_VERSION)
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val EMAIL = stringPreferencesKey("email")
        val EMAIL_VERIFIED = booleanPreferencesKey("email_verified")
        val SYNC_VERSION = longPreferencesKey("sync_version")
        val FAMILY_ID = stringPreferencesKey("family_id")
        val FAMILY = stringPreferencesKey("family")
        val FAMILY_VERSION = longPreferencesKey("family_version")
    }
}
