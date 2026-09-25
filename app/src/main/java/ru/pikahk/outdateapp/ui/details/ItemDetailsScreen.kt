package ru.pikahk.outdateapp.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import kotlin.math.abs
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.domain.formatDate
import ru.pikahk.outdateapp.ui.color
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
                title = { Text((state as? ItemDetailsState.Loaded)?.item?.name.orEmpty()) },
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
        val current = state
        if (current is ItemDetailsState.Loaded) {
            ItemDetailsContent(
                item = current.item,
                onOpen = viewModel::markOpened,
                onUndoOpen = viewModel::markSealed,
                onDelete = { confirmDelete = true },
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
                    Text(stringResource(R.string.delete))
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
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = statusText(item.daysLeft),
            style = MaterialTheme.typography.headlineSmall,
            color = item.urgency.color()
        )

        InfoRow(stringResource(R.string.details_expires), formatDate(item.effectiveExpiresAt))
        if (item.effectiveExpiresAt != item.expiresAt) {
            InfoRow(stringResource(R.string.details_package_date), formatDate(item.expiresAt))
        }
        InfoRow(
            label = stringResource(R.string.details_after_opening),
            value = item.daysAfterOpening?.let { pluralStringResource(R.plurals.days, it, it) }
                ?: stringResource(R.string.details_not_set)
        )
        InfoRow(
            label = stringResource(R.string.details_opened),
            value = item.openedAt?.let(::formatDate) ?: stringResource(R.string.details_sealed)
        )

        if (item.openedAt == null) {
            Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.open_today))
            }
        } else {
            OutlinedButton(onClick = onUndoOpen, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.undo_open))
            }
        }
        TextButton(
            onClick = onDelete,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.delete))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun statusText(daysLeft: Long): String {
    val days = abs(daysLeft).toInt()
    return when {
        daysLeft < 0 -> stringResource(R.string.status_expired, pluralStringResource(R.plurals.days, days, days))
        daysLeft == 0L -> stringResource(R.string.status_today)
        else -> stringResource(R.string.status_left, pluralStringResource(R.plurals.days, days, days))
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemDetailsContentPreview() {
    OutDateAppTheme {
        ItemDetailsContent(
            item = ItemDetailsUi(
                name = "Молоко",
                expiresAt = LocalDate.of(2026, 10, 15),
                openedAt = LocalDate.of(2026, 9, 20),
                daysAfterOpening = 5,
                effectiveExpiresAt = LocalDate.of(2026, 9, 25),
                daysLeft = 2,
                urgency = Urgency.SOON
            ),
            onOpen = {},
            onUndoOpen = {},
            onDelete = {}
        )
    }
}
