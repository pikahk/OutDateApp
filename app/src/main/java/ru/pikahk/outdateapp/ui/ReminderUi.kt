package ru.pikahk.outdateapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.domain.REMINDER_OFF

val reminderOptions = listOf(
    REMINDER_OFF to R.string.reminder_off,
    1 to R.string.reminder_1,
    3 to R.string.reminder_3,
    7 to R.string.reminder_7
)

@Composable
fun reminderLabel(days: Int): String {
    val option = reminderOptions.firstOrNull { it.first == days }
    return if (option != null) stringResource(option.second) else pluralStringResource(R.plurals.days, days, days)
}
