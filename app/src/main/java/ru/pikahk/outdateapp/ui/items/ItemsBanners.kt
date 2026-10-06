package ru.pikahk.outdateapp.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.ui.containerColor

private const val MAX_NAMES = 3

@Composable
internal fun ItemsBanners(
    missedExpiries: List<String>,
    showNotificationsOff: Boolean,
    onDismissMissed: () -> Unit,
    onEnableNotifications: () -> Unit,
    onDismissNotifications: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (missedExpiries.isNotEmpty()) {
            Banner(
                title = stringResource(R.string.away_title),
                text = stringResource(R.string.away_expired, namesText(missedExpiries)),
                containerColor = Urgency.EXPIRED.containerColor()
            ) {
                TextButton(onClick = onDismissMissed) {
                    Text(stringResource(R.string.away_ok))
                }
            }
        }
        if (showNotificationsOff) {
            Banner(
                title = stringResource(R.string.settings_notifications_off),
                text = stringResource(R.string.settings_notifications_off_hint),
                containerColor = Urgency.SOON.containerColor()
            ) {
                TextButton(onClick = onDismissNotifications) {
                    Text(stringResource(R.string.not_now))
                }
                Button(
                    onClick = onEnableNotifications,
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
    }
}

@Composable
private fun Banner(title: String, text: String, containerColor: Color, actions: @Composable RowScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(containerColor, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, end = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

@Composable
private fun namesText(names: List<String>): String {
    val shown = names.take(MAX_NAMES).joinToString(", ")
    val rest = names.size - MAX_NAMES
    return if (rest > 0) stringResource(R.string.away_and_more, shown, rest) else shown
}
