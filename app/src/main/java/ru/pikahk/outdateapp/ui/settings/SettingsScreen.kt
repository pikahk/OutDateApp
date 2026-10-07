package ru.pikahk.outdateapp.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.repository.AppSettings
import ru.pikahk.outdateapp.data.repository.ThemeMode
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.notifications.areNotificationsEnabled
import ru.pikahk.outdateapp.notifications.openNotificationSettings
import ru.pikahk.outdateapp.ui.LinkRow
import ru.pikahk.outdateapp.ui.SectionTitle
import ru.pikahk.outdateapp.ui.add.LabeledField
import ru.pikahk.outdateapp.ui.add.Segmented
import ru.pikahk.outdateapp.ui.containerColor
import ru.pikahk.outdateapp.ui.reminderOptions
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

private val themeOptions = listOf(
    ThemeMode.SYSTEM to R.string.theme_system,
    ThemeMode.LIGHT to R.string.theme_light,
    ThemeMode.DARK to R.string.theme_dark
)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsEnabled by remember { mutableStateOf(areNotificationsEnabled(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsEnabled = areNotificationsEnabled(context)
    }

    SettingsScreen(
        settings = settings,
        notificationsEnabled = notificationsEnabled,
        onBack = onBack,
        onNotificationsClick = { openNotificationSettings(context) },
        onReminderTimeChange = viewModel::setReminderTime,
        onDefaultReminderChange = viewModel::setDefaultNotifyDaysBefore,
        onThemeChange = viewModel::setTheme,
        showLanguage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
        onLanguageClick = { openLanguageSettings(context) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    settings: AppSettings?,
    notificationsEnabled: Boolean,
    onBack: () -> Unit,
    onNotificationsClick: () -> Unit,
    onReminderTimeChange: (LocalTime) -> Unit,
    onDefaultReminderChange: (Int) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    showLanguage: Boolean,
    onLanguageClick: () -> Unit
) {
    var pickingTime by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (settings == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            SectionTitle(stringResource(R.string.settings_notifications))
            if (notificationsEnabled) {
                LinkRow(
                    title = stringResource(R.string.settings_notifications),
                    value = stringResource(R.string.settings_notifications_on),
                    onClick = onNotificationsClick
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            } else {
                NotificationsOffCard(onEnable = onNotificationsClick)
            }
            LinkRow(
                title = stringResource(R.string.settings_time),
                value = rememberTimeText(settings.reminderTime),
                onClick = { pickingTime = true }
            )
            Column(modifier = Modifier.padding(top = 12.dp)) {
                LabeledField(label = stringResource(R.string.settings_default_reminder)) {
                    Segmented(
                        options = reminderOptions,
                        selected = settings.defaultNotifyDaysBefore,
                        onSelect = onDefaultReminderChange
                    )
                }
            }

            SectionTitle(stringResource(R.string.settings_appearance))
            LabeledField(label = stringResource(R.string.settings_theme)) {
                Segmented(options = themeOptions, selected = settings.theme, onSelect = onThemeChange)
            }
            if (showLanguage) {
                LinkRow(
                    title = stringResource(R.string.settings_language),
                    value = rememberLanguageName(),
                    onClick = onLanguageClick,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        if (pickingTime) {
            ReminderTimeDialog(
                initial = settings.reminderTime,
                onPick = onReminderTimeChange,
                onDismiss = { pickingTime = false }
            )
        }
    }
}

@Composable
private fun NotificationsOffCard(onEnable: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .background(Urgency.SOON.containerColor(), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.settings_notifications_off),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.settings_notifications_off_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = onEnable,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface
            ),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Text(stringResource(R.string.settings_enable))
        }
    }
}

@Composable
private fun rememberTimeText(time: LocalTime): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(time, locale) {
        val skeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
        time.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
    }
}

@Composable
private fun rememberLanguageName(): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) } }
}

private fun openLanguageSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
    context.startActivity(intent)
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    OutDateAppTheme {
        SettingsScreen(
            settings = AppSettings(),
            notificationsEnabled = false,
            onBack = {},
            onNotificationsClick = {},
            onReminderTimeChange = {},
            onDefaultReminderChange = {},
            onThemeChange = {},
            showLanguage = true,
            onLanguageClick = {}
        )
    }
}
