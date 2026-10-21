package ru.pikahk.outdateapp.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class AppLanguage(val tag: String?) { SYSTEM(null), RUSSIAN("ru"), ENGLISH("en") }

fun currentAppLanguage(): AppLanguage {
    val tag = AppCompatDelegate.getApplicationLocales()[0]?.language
    return AppLanguage.entries.firstOrNull { it.tag == tag } ?: AppLanguage.SYSTEM
}

fun setAppLanguage(language: AppLanguage) {
    val locales = language.tag?.let(LocaleListCompat::forLanguageTags) ?: LocaleListCompat.getEmptyLocaleList()
    AppCompatDelegate.setApplicationLocales(locales)
}
