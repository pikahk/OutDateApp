package ru.pikahk.outdateapp.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.ui.color
import ru.pikahk.outdateapp.ui.containerColor
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsScreen(viewModel: ItemDetailsViewModel, onBack: () -> Unit, onGone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    if (state is ItemDetailsState.Gone) {
        LaunchedEffect(Unit) { onGone() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        val current = state
        if (current is ItemDetailsState.Loaded) {
            ItemDetailsContent(
                item = current.item,
                onOpen = viewModel::markOpened,
                onUndoOpen = viewModel::markSealed,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete()
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ItemDetailsContent(
    item: ItemDetailsUi,
    onOpen: () -> Unit,
    onUndoOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val longDate = rememberDateFormatter("d MMMM yyyy")
    val shortDate = rememberDateFormatter("d MMMM")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        StatusCard(item = item, dateText = item.effectiveExpiresAt.format(longDate))

        InfoCard {
            InfoRow(
                title = stringResource(R.string.details_package_date),
                subtitle = item.expiresAt.format(longDate),
                active = !item.limitedByOpening
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow(
                title = stringResource(R.string.details_after_opening),
                subtitle = openingText(item, shortDate),
                active = item.limitedByOpening
            )
        }

        if (item.openedAt == null) {
            Button(
                onClick = onOpen,
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.open_today), style = MaterialTheme.typography.titleMedium)
            }
            if (item.expiresIfOpenedToday != null) {
                Text(
                    text = stringResource(R.string.details_open_hint, item.expiresIfOpenedToday.format(shortDate)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
            }
        } else {
            OutlinedButton(
                onClick = onUndoOpen,
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.undo_open), style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.details_added),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = item.createdAt.format(shortDate),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StatusCard(item: ItemDetailsUi, dateText: String) {
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
private fun InfoCard(content: @Composable () -> Unit) {
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
private fun InfoRow(title: String, subtitle: String, active: Boolean) {
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
                text = if (active) stringResource(R.string.details_used_now, subtitle) else subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun openingText(item: ItemDetailsUi, formatter: DateTimeFormatter): String {
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

@Composable
private fun rememberDateFormatter(pattern: String): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, pattern) { DateTimeFormatter.ofPattern(pattern, locale) }
}

@Preview(showBackground = true)
@Composable
private fun ItemDetailsContentPreview() {
    OutDateAppTheme {
        ItemDetailsContent(
            item = ItemDetailsUi(
                name = "Творог 5%",
                expiresAt = LocalDate.of(2026, 9, 29),
                openedAt = null,
                daysAfterOpening = 3,
                effectiveExpiresAt = LocalDate.of(2026, 9, 29),
                daysLeft = 4,
                urgency = Urgency.SOON,
                limitedByOpening = false,
                openingExpiresAt = null,
                expiresIfOpenedToday = LocalDate.of(2026, 9, 28),
                createdAt = LocalDate.of(2026, 9, 20)
            ),
            onOpen = {},
            onUndoOpen = {}
        )
    }
}
