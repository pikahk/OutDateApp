package ru.pikahk.outdateapp.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.model.PeriodType
import ru.pikahk.outdateapp.domain.parseDate
import ru.pikahk.outdateapp.ui.add.AmountField
import ru.pikahk.outdateapp.ui.add.DateField
import ru.pikahk.outdateapp.ui.add.FieldShape
import ru.pikahk.outdateapp.ui.add.LabeledField
import ru.pikahk.outdateapp.ui.add.Segmented
import ru.pikahk.outdateapp.ui.add.fieldColors
import ru.pikahk.outdateapp.ui.reminderOptions
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

private const val PRICE_MAX_LENGTH = 12

private val periodOptions = listOf(
    PeriodType.MONTHLY to R.string.period_option_month,
    PeriodType.YEARLY to R.string.period_option_year,
    PeriodType.CUSTOM_DAYS to R.string.period_option_days
)

@Composable
fun SubscriptionEditScreen(viewModel: SubscriptionEditViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    LaunchedEffect(viewModel.finished) {
        if (viewModel.finished) onDone()
    }
    SubscriptionEditScreen(
        form = viewModel.form,
        isNew = viewModel.isNew,
        isCancelled = viewModel.isCancelled,
        onChange = viewModel::update,
        onSave = viewModel::save,
        onToggleCancelled = viewModel::toggleCancelled,
        onDelete = viewModel::delete,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionEditScreen(
    form: SubscriptionForm?,
    isNew: Boolean,
    isCancelled: Boolean,
    onChange: (SubscriptionForm) -> Unit,
    onSave: () -> Unit,
    onToggleCancelled: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val today = remember { LocalDate.now() }
    var showMissing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(if (isNew) R.string.subscription_new else R.string.subscription_title),
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
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (form != null) {
                SaveBar(
                    nextCharge = form.nextCharge(today),
                    canSave = form.toSubscription(null, today) != null,
                    onSave = onSave,
                    onBlockedSave = { showMissing = true }
                )
            }
        }
    ) { padding ->
        if (form != null) {
            SubscriptionFields(
                form = form,
                today = today,
                showMissing = showMissing,
                onChange = onChange,
                modifier = Modifier.padding(padding)
            ) {
                if (!isNew) {
                    val label = if (isCancelled) R.string.subscription_resume else R.string.subscription_cancel
                    OutlinedButton(
                        onClick = onToggleCancelled,
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(stringResource(label))
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.subscription_delete_title)) },
            text = { Text(stringResource(R.string.delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
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
private fun SubscriptionFields(
    form: SubscriptionForm,
    today: LocalDate,
    showMissing: Boolean,
    onChange: (SubscriptionForm) -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit
) {
    val nameMissing = showMissing && form.name.isBlank()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LabeledField(label = stringResource(R.string.field_name)) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { onChange(form.copy(name = it)) },
                isError = nameMissing,
                supportingText = if (nameMissing) {
                    { Text(stringResource(R.string.field_required)) }
                } else {
                    null
                },
                singleLine = true,
                shape = FieldShape,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        PriceField(
            value = form.price,
            onValueChange = { onChange(form.copy(price = it)) },
            invalid = form.price.isNotBlank() && form.priceMinor == null,
            missing = showMissing && form.price.isBlank()
        )

        DateField(
            value = form.startedAt,
            onValueChange = { onChange(form.copy(startedAt = it)) },
            label = stringResource(R.string.field_started),
            hint = stringResource(R.string.hint_date_format),
            parse = ::parseDate,
            missing = showMissing && form.startDate == null,
            error = if (form.startsInFuture(today)) stringResource(R.string.error_future_date) else null
        )

        LabeledField(label = stringResource(R.string.field_period)) {
            Segmented(
                options = periodOptions,
                selected = form.periodType,
                onSelect = { onChange(form.copy(periodType = it)) }
            )
        }
        if (form.periodType == PeriodType.CUSTOM_DAYS) {
            AmountField(
                value = form.periodDays,
                onValueChange = { onChange(form.copy(periodDays = it)) },
                label = stringResource(R.string.field_every_days),
                missing = showMissing && form.days == null
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        LabeledField(label = stringResource(R.string.field_charge_reminder)) {
            Segmented(
                options = reminderOptions,
                selected = form.notifyDaysBefore,
                onSelect = { onChange(form.copy(notifyDaysBefore = it)) }
            )
        }

        footer()
    }
}

@Composable
private fun PriceField(value: String, onValueChange: (String) -> Unit, invalid: Boolean, missing: Boolean) {
    val supporting = when {
        invalid -> stringResource(R.string.error_not_number)
        missing -> stringResource(R.string.field_required)
        else -> null
    }
    LabeledField(label = stringResource(R.string.field_price)) {
        OutlinedTextField(
            value = value,
            onValueChange = { text ->
                onValueChange(text.filter { it.isPriceChar() }.take(PRICE_MAX_LENGTH))
            },
            isError = supporting != null,
            supportingText = if (supporting != null) {
                { Text(supporting) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = FieldShape,
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun Char.isPriceChar(): Boolean = isDigit() || this == '.' || this == ',' || this == ' '

@Composable
private fun SaveBar(nextCharge: LocalDate?, canSave: Boolean, onSave: () -> Unit, onBlockedSave: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding()
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (nextCharge != null) {
                Text(
                    text = stringResource(R.string.summary_next_charge, rememberShortDate(nextCharge)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(11.dp))
                        .padding(horizontal = 13.dp, vertical = 11.dp)
                )
            }
            Box {
                Button(
                    onClick = onSave,
                    enabled = canSave,
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium)
                }
                if (!canSave) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(interactionSource = null, indication = null, onClick = onBlockedSave)
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberShortDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(date, locale) { date.format(DateTimeFormatter.ofPattern("d MMMM", locale)) }
}

@Preview(showBackground = true)
@Composable
private fun SubscriptionEditScreenPreview() {
    OutDateAppTheme {
        SubscriptionEditScreen(
            form = SubscriptionForm(name = "Яндекс Плюс", price = "399", startedAt = "07.03.2025"),
            isNew = false,
            isCancelled = false,
            onChange = {},
            onSave = {},
            onToggleCancelled = {},
            onDelete = {},
            onBack = {}
        )
    }
}
