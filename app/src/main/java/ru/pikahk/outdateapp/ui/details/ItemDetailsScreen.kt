package ru.pikahk.outdateapp.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.DefaultCategory
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.ui.CategoryUi
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsScreen(viewModel: ItemDetailsViewModel, onBack: () -> Unit, onGone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var askOpening by rememberSaveable { mutableStateOf(false) }

    if (state is ItemDetailsState.Gone) {
        LaunchedEffect(Unit) { onGone() }
    }

    val current = state
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (current is ItemDetailsState.Loaded) {
                DetailsActions(
                    item = current.item,
                    onOpen = {
                        if (current.item.daysAfterOpening == null) askOpening = true else viewModel.markOpened(null)
                    },
                    onUndoOpen = viewModel::markSealed
                )
            }
        }
    ) { padding ->
        if (current is ItemDetailsState.Loaded) {
            ItemDetailsContent(item = current.item, modifier = Modifier.padding(padding))
        }
    }

    if (askOpening) {
        OpeningDialog(
            onConfirm = { days ->
                askOpening = false
                viewModel.markOpened(days)
            },
            onDismiss = { askOpening = false }
        )
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
private fun ItemDetailsContent(item: ItemDetailsUi, modifier: Modifier = Modifier) {
    val longDate = rememberDateFormatter("d MMMM yyyy")
    val shortDate = rememberDateFormatter("d MMMM")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (item.category != null) {
                CategoryTag(item.category)
            }
            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        StatusHero(item = item, dateText = item.effectiveExpiresAt.format(longDate))

        DetailRows(item = item, longDate = longDate, shortDate = shortDate)
    }
}

@Composable
private fun DetailsActions(item: ItemDetailsUi, onOpen: () -> Unit, onUndoOpen: () -> Unit) {
    val shortDate = rememberDateFormatter("d MMMM")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (item.openedAt == null) {
            if (item.expiresIfOpenedToday != null) {
                Text(
                    text = stringResource(R.string.details_open_hint, item.expiresIfOpenedToday.format(shortDate)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = onOpen,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.open_today), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            OutlinedButton(
                onClick = onUndoOpen,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.undo_open), style = MaterialTheme.typography.titleMedium)
            }
        }
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
                category = CategoryUi("food", "food", DefaultCategory.FOOD),
                expiresAt = LocalDate.of(2026, 9, 29),
                openedAt = null,
                daysAfterOpening = 3,
                effectiveExpiresAt = LocalDate.of(2026, 9, 29),
                daysLeft = 4,
                urgency = Urgency.SOON,
                limitedByOpening = false,
                expiresIfOpenedToday = LocalDate.of(2026, 9, 28),
                createdAt = LocalDate.of(2026, 9, 20),
                notifyDaysBefore = 3
            )
        )
    }
}
