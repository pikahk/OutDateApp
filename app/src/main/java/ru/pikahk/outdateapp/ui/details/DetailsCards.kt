package ru.pikahk.outdateapp.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.ui.CategoryUi
import ru.pikahk.outdateapp.ui.color
import ru.pikahk.outdateapp.ui.containerColor
import ru.pikahk.outdateapp.ui.iconRes
import ru.pikahk.outdateapp.ui.label

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
internal fun StatusCard(item: ItemDetailsUi, dateText: String) {
    val days = abs(item.daysLeft).toInt()
    val (label, value, subtitle) = when {
        item.daysLeft < 0 -> Triple(
            stringResource(R.string.details_status_expired),
            pluralStringResource(R.plurals.days, days, days),
            stringResource(R.string.details_was_due, dateText)
        )

        item.daysLeft == 0L -> Triple(
            stringResource(R.string.details_status_expires),
            stringResource(R.string.badge_today),
            dateText
        )

        else -> Triple(
            stringResource(R.string.details_status_left),
            pluralStringResource(R.plurals.days, days, days),
            stringResource(R.string.details_until, dateText)
        )
    }
    val color = item.urgency.color()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = item.urgency.containerColor(), shape = RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = color
        )
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = color
        )
    }
}

@Composable
internal fun InfoCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = shape)
    ) {
        content()
    }
}

@Composable
internal fun InfoRow(title: String, subtitle: String, active: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (active) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (active) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = Urgency.OK.color(),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun openingText(item: ItemDetailsUi, formatter: DateTimeFormatter): String {
    val days = item.daysAfterOpening
    val openedAt = item.openedAt
    return when {
        days == null && openedAt == null -> stringResource(R.string.details_not_set)

        days == null && openedAt != null ->
            stringResource(R.string.details_opened_no_limit, openedAt.format(formatter))

        openedAt == null && days != null ->
            stringResource(R.string.details_opening_not_started, pluralStringResource(R.plurals.days, days, days))

        else -> stringResource(
            R.string.details_opening_running,
            openedAt?.format(formatter).orEmpty(),
            item.openingExpiresAt?.format(formatter).orEmpty()
        )
    }
}
