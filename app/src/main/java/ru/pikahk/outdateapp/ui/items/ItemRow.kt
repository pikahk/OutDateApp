package ru.pikahk.outdateapp.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.ui.color
import ru.pikahk.outdateapp.ui.containerColor
import ru.pikahk.outdateapp.ui.iconRes
import ru.pikahk.outdateapp.ui.label

@Composable
internal fun ItemRow(item: ItemUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ItemBadge(item)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle(item),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DaysBadge(daysLeft = item.daysLeft, urgency = item.urgency)
    }
}

@Composable
private fun ItemBadge(item: ItemUi) {
    val expired = item.urgency == Urgency.EXPIRED
    val content = if (expired) item.urgency.color() else MaterialTheme.colorScheme.onSurfaceVariant
    val background = if (expired) item.urgency.containerColor() else MaterialTheme.colorScheme.surfaceVariant
    val builtIn = item.category?.builtIn
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = background, shape = RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (builtIn != null) {
            Icon(
                painter = painterResource(builtIn.iconRes()),
                contentDescription = item.category?.label(),
                tint = content,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = item.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = content
            )
        }
    }
}

@Composable
private fun DaysBadge(daysLeft: Long, urgency: Urgency) {
    Text(
        text = badgeText(daysLeft),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = urgency.color(),
        modifier = Modifier
            .background(color = urgency.containerColor(), shape = RoundedCornerShape(9.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun subtitle(item: ItemUi): String {
    val openedAt = item.openedAt ?: return stringResource(R.string.list_sealed)
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
    val date = openedAt.format(formatter)
    return if (item.limitedByOpening) {
        stringResource(R.string.list_opened_limited, date)
    } else {
        stringResource(R.string.list_opened, date)
    }
}

@Composable
private fun badgeText(daysLeft: Long): String {
    val days = abs(daysLeft).toInt()
    return when {
        daysLeft < 0 -> pluralStringResource(R.plurals.days_ago, days, days)
        daysLeft == 0L -> stringResource(R.string.badge_today)
        daysLeft == 1L -> stringResource(R.string.badge_tomorrow)
        else -> pluralStringResource(R.plurals.days, days, days)
    }
}
