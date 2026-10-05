package ru.pikahk.outdateapp.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.ui.CategoryUi
import ru.pikahk.outdateapp.ui.color
import ru.pikahk.outdateapp.ui.containerColor
import ru.pikahk.outdateapp.ui.iconRes
import ru.pikahk.outdateapp.ui.label
import ru.pikahk.outdateapp.ui.reminderLabel

@Composable
internal fun CategoryTag(category: CategoryUi) {
    val icon = category.builtIn?.iconRes()
    Row(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = category.label(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun StatusHero(item: ItemDetailsUi, dateText: String) {
    val color = item.urgency.color()
    val days = abs(item.daysLeft).toInt()
    val subtitle = when {
        item.daysLeft < 0 -> stringResource(R.string.details_was_due, dateText)
        item.daysLeft == 0L -> dateText
        else -> stringResource(R.string.details_until, dateText)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = item.urgency.containerColor(), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (item.daysLeft == 0L) {
            Text(
                text = stringResource(R.string.badge_today),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        } else {
            val unit = if (item.daysLeft < 0) R.plurals.days_ago_unit else R.plurals.days_unit
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = days.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.alignByBaseline()
                )
                Text(
                    text = pluralStringResource(unit, days),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = color,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun DetailRows(item: ItemDetailsUi, longDate: DateTimeFormatter, shortDate: DateTimeFormatter) {
    val accent = item.urgency.color()
    val afterOpening = item.daysAfterOpening
    Column {
        DetailRow(
            label = stringResource(R.string.details_package_date),
            value = item.expiresAt.format(longDate),
            accent = if (item.limitedByOpening) null else accent
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DetailRow(
            label = stringResource(R.string.details_after_opening),
            value = if (afterOpening != null) {
                pluralStringResource(R.plurals.days, afterOpening, afterOpening)
            } else {
                stringResource(R.string.details_not_set)
            },
            accent = if (item.limitedByOpening) accent else null
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DetailRow(
            label = stringResource(R.string.details_opened),
            value = item.openedAt?.format(shortDate) ?: stringResource(R.string.details_not_opened)
        )
        DetailRow(
            label = stringResource(R.string.details_reminder),
            value = reminderLabel(item.notifyDaysBefore)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DetailRow(
            label = stringResource(R.string.details_added),
            value = item.createdAt.format(shortDate)
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, accent: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (accent != null) FontWeight.Bold else FontWeight.Medium,
            color = accent ?: MaterialTheme.colorScheme.onSurface
        )
    }
}
