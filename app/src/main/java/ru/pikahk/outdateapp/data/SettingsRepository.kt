package ru.pikahk.outdateapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pikahk.outdateapp.domain.DEFAULT_REMINDER_DAYS

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val reminderTime: LocalTime = LocalTime.of(9, 0),
    val defaultNotifyDaysBefore: Int = DEFAULT_REMINDER_DAYS,
    val theme: ThemeMode = ThemeMode.SYSTEM
)

data class ListPreferences(val categoryId: String? = null, val sort: String? = null, val opened: String? = null)

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsStore

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            reminderTime = prefs[REMINDER_MINUTES]?.let { LocalTime.of(it / 60, it % 60) } ?: DEFAULTS.reminderTime,
            defaultNotifyDaysBefore = prefs[DEFAULT_NOTIFY_DAYS] ?: DEFAULTS.defaultNotifyDaysBefore,
            theme = ThemeMode.entries.firstOrNull { it.name == prefs[THEME] } ?: DEFAULTS.theme
        )
    }

    val listPreferences: Flow<ListPreferences> = store.data.map { prefs ->
        ListPreferences(
            categoryId = prefs[LIST_CATEGORY],
            sort = prefs[LIST_SORT],
            opened = prefs[LIST_OPENED]
        )
    }

    suspend fun setReminderTime(time: LocalTime) {
        store.edit { it[REMINDER_MINUTES] = time.hour * 60 + time.minute }
    }

    suspend fun setDefaultNotifyDaysBefore(days: Int) {
        store.edit { it[DEFAULT_NOTIFY_DAYS] = days }
    }

    suspend fun setTheme(theme: ThemeMode) {
        store.edit { it[THEME] = theme.name }
    }

    suspend fun saveListPreferences(preferences: ListPreferences) {
        store.edit { prefs ->
            prefs.put(LIST_CATEGORY, preferences.categoryId)
            prefs.put(LIST_SORT, preferences.sort)
            prefs.put(LIST_OPENED, preferences.opened)
        }
    }

    suspend fun markOpened(today: LocalDate): LocalDate? {
        var previous: LocalDate? = null
        store.edit { prefs ->
            previous = prefs[LAST_OPENED_DAY]?.let(LocalDate::ofEpochDay)
            prefs[LAST_OPENED_DAY] = today.toEpochDay()
        }
        return previous
    }

    private fun MutablePreferences.put(key: Preferences.Key<String>, value: String?) {
        if (value == null) remove(key) else this[key] = value
    }

    private companion object {
        val DEFAULTS = AppSettings()
        val REMINDER_MINUTES = intPreferencesKey("reminder_minutes")
        val DEFAULT_NOTIFY_DAYS = intPreferencesKey("default_notify_days")
        val THEME = stringPreferencesKey("theme")
        val LIST_CATEGORY = stringPreferencesKey("list_category")
        val LIST_SORT = stringPreferencesKey("list_sort")
        val LIST_OPENED = stringPreferencesKey("list_opened")
        val LAST_OPENED_DAY = longPreferencesKey("last_opened_day")
    }
}
