package ru.pikahk.outdateapp.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.domain.ShelfLifeUnit
import ru.pikahk.outdateapp.domain.effectiveExpiryDate
import ru.pikahk.outdateapp.domain.expiryFromProduction
import ru.pikahk.outdateapp.domain.formatDate
import ru.pikahk.outdateapp.domain.formatDateDigits
import ru.pikahk.outdateapp.domain.parseDate
import ru.pikahk.outdateapp.domain.parseExpiryDate
import ru.pikahk.outdateapp.domain.shelfLifeInDays
import ru.pikahk.outdateapp.domain.urgency
import ru.pikahk.outdateapp.ui.color
import ru.pikahk.outdateapp.ui.containerColor
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

private enum class InputMode { EXPIRY_DATE, PRODUCTION }

private val modeOptions = listOf(
    InputMode.EXPIRY_DATE to R.string.mode_expiry,
    InputMode.PRODUCTION to R.string.mode_production
)

private val unitOptions = listOf(
    ShelfLifeUnit.HOURS to R.string.unit_hours,
    ShelfLifeUnit.DAYS to R.string.unit_days,
    ShelfLifeUnit.MONTHS to R.string.unit_months,
    ShelfLifeUnit.YEARS to R.string.unit_years
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(onSave: (ItemDraft) -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    var name by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(InputMode.EXPIRY_DATE) }
    var expiryText by rememberSaveable { mutableStateOf("") }
    var producedText by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(ShelfLifeUnit.DAYS) }
    var openedAmountText by rememberSaveable { mutableStateOf("") }
    var openedUnit by rememberSaveable { mutableStateOf(ShelfLifeUnit.DAYS) }
    var alreadyOpened by rememberSaveable { mutableStateOf(false) }
    var openedText by rememberSaveable { mutableStateOf("") }

    val expiresAt = when (mode) {
        InputMode.EXPIRY_DATE -> parseExpiryDate(expiryText)
        InputMode.PRODUCTION -> calculateExpiry(producedText, amountText, unit)
    }
    val daysAfterOpening = parseAmount(openedAmountText)?.let { shelfLifeInDays(it, openedUnit) }
    val openedAt = if (alreadyOpened) parseDate(openedText) else null
    val canSave = name.isNotBlank() && expiresAt != null && (!alreadyOpened || openedAt != null)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.add_title), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
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
                    if (expiresAt != null) {
                        ExpirySummary(
                            expiresAt = expiresAt,
                            effective = effectiveExpiryDate(expiresAt, openedAt, daysAfterOpening),
                            today = today
                        )
                    }
                    Button(
                        onClick = {
                            if (expiresAt != null) {
                                onSave(ItemDraft(name.trim(), expiresAt, daysAfterOpening, openedAt))
                            }
                        },
                        enabled = canSave,
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LabeledField(label = stringResource(R.string.field_name)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    shape = FieldShape,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Segmented(options = modeOptions, selected = mode, onSelect = { mode = it })

            when (mode) {
                InputMode.EXPIRY_DATE -> DateField(
                    value = expiryText,
                    onValueChange = { expiryText = it },
                    label = stringResource(R.string.field_expires),
                    hint = stringResource(R.string.hint_expiry_format),
                    parse = ::parseExpiryDate
                )

                InputMode.PRODUCTION -> {
                    DateField(
                        value = producedText,
                        onValueChange = { producedText = it },
                        label = stringResource(R.string.field_produced),
                        hint = stringResource(R.string.hint_date_format),
                        parse = ::parseDate
                    )
                    AmountField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = stringResource(R.string.field_shelf_life)
                    )
                    Segmented(options = unitOptions, selected = unit, onSelect = { unit = it })
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            AmountField(
                value = openedAmountText,
                onValueChange = { openedAmountText = it },
                label = stringResource(R.string.field_after_opening),
                hint = stringResource(R.string.hint_after_opening)
            )
            Segmented(options = unitOptions, selected = openedUnit, onSelect = { openedUnit = it })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.already_opened),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.already_opened_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = alreadyOpened,
                    onCheckedChange = { checked ->
                        alreadyOpened = checked
                        if (checked && openedText.isEmpty()) openedText = formatDate(today)
                    }
                )
            }
            if (alreadyOpened) {
                DateField(
                    value = openedText,
                    onValueChange = { openedText = it },
                    label = stringResource(R.string.field_opened),
                    hint = stringResource(R.string.hint_date_format),
                    parse = ::parseDate
                )
            }
        }
    }
}

@Composable
private fun ExpirySummary(expiresAt: LocalDate, effective: LocalDate, today: LocalDate) {
    val left = ChronoUnit.DAYS.between(today, effective)
    val level = urgency(left)
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
    val date = effective.format(formatter)
    val text = when {
        left < 0 -> stringResource(R.string.summary_expired, date)
        effective < expiresAt -> stringResource(R.string.summary_expires_opening, date)
        else -> stringResource(R.string.summary_expires, date)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = level.color(),
        modifier = Modifier
            .fillMaxWidth()
            .background(color = level.containerColor(), shape = RoundedCornerShape(11.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp)
    )
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

private val FieldShape = RoundedCornerShape(12.dp)

@Composable
private fun fieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedBorderColor = MaterialTheme.colorScheme.primary
)

@Composable
private fun DateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    parse: (String) -> LocalDate?
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val date = parse(value)
    val showError = date == null && isYearTyped(value)

    LabeledField(label = label) {
        OutlinedTextField(
            value = TextFieldValue(value, TextRange(value.length)),
            onValueChange = { onValueChange(formatDateDigits(it.text)) },
            placeholder = { Text("15.10.2026") },
            trailingIcon = {
                IconButton(onClick = { showPicker = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = stringResource(R.string.pick_date)
                    )
                }
            },
            isError = showError,
            supportingText = { Text(if (showError) stringResource(R.string.error_no_such_date) else hint) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = FieldShape,
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showPicker) {
        DatePickerModal(
            initial = date,
            onPick = { onValueChange(formatDate(it)) },
            onDismiss = { showPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(initial: LocalDate?, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial?.toUtcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onPick(it.toUtcDate()) }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun AmountField(value: String, onValueChange: (String) -> Unit, label: String, hint: String? = null) {
    LabeledField(label = label) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.filter(Char::isDigit).take(4)) },
            supportingText = if (hint != null) {
                { Text(hint) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = FieldShape,
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun <T> Segmented(options: List<Pair<T, Int>>, selected: T, onSelect: (T) -> Unit) {
    val selectedText = MaterialTheme.colorScheme.primary
    val selectedBackground = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(13.dp))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, labelRes) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) selectedBackground else Color.Transparent)
                    .selectable(selected = isSelected, onClick = { onSelect(value) }, role = Role.Tab),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) selectedText else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

private fun calculateExpiry(producedText: String, amountText: String, unit: ShelfLifeUnit): LocalDate? {
    val producedAt = parseDate(producedText) ?: return null
    val amount = parseAmount(amountText) ?: return null
    return expiryFromProduction(producedAt, amount, unit)
}

private fun parseAmount(text: String): Int? = text.toIntOrNull()?.takeIf { it > 0 }

private fun isYearTyped(text: String): Boolean = text.trim().takeLastWhile { it.isDigit() }.length >= 4

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Preview(showBackground = true)
@Composable
private fun AddItemScreenPreview() {
    OutDateAppTheme {
        AddItemScreen(onSave = {}, onCancel = {})
    }
}
