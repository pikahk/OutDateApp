package ru.pikahk.outdateapp.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.domain.ShelfLifeUnit
import ru.pikahk.outdateapp.domain.expiryFromProduction
import ru.pikahk.outdateapp.domain.formatDate
import ru.pikahk.outdateapp.domain.formatDateDigits
import ru.pikahk.outdateapp.domain.parseDate
import ru.pikahk.outdateapp.domain.parseExpiryDate
import ru.pikahk.outdateapp.domain.shelfLifeInDays
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

private enum class InputMode { EXPIRY_DATE, PRODUCTION }

private val modeOptions = listOf(
    InputMode.EXPIRY_DATE to "Годен до",
    InputMode.PRODUCTION to "Изготовлен"
)

private val unitOptions = listOf(
    ShelfLifeUnit.HOURS to "часы",
    ShelfLifeUnit.DAYS to "сутки",
    ShelfLifeUnit.MONTHS to "месяцы",
    ShelfLifeUnit.YEARS to "годы"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(onSave: (ItemDraft) -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    var name by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(InputMode.EXPIRY_DATE) }
    var expiryText by rememberSaveable { mutableStateOf("") }
    var producedText by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(ShelfLifeUnit.DAYS) }
    var openedAmountText by rememberSaveable { mutableStateOf("") }
    var openedUnit by rememberSaveable { mutableStateOf(ShelfLifeUnit.DAYS) }

    val expiresAt = when (mode) {
        InputMode.EXPIRY_DATE -> parseExpiryDate(expiryText)
        InputMode.PRODUCTION -> calculateExpiry(producedText, amountText, unit)
    }
    val daysAfterOpening = parseAmount(openedAmountText)?.let { shelfLifeInDays(it, openedUnit) }
    val canSave = name.isNotBlank() && expiresAt != null

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Новый продукт") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ChoiceChips(options = modeOptions, selected = mode, onSelect = { mode = it })

            when (mode) {
                InputMode.EXPIRY_DATE -> DateField(
                    value = expiryText,
                    onValueChange = { expiryText = it },
                    label = "Годен до",
                    hint = "ДД.ММ.ГГГГ или ММ.ГГГГ",
                    parse = ::parseExpiryDate
                )

                InputMode.PRODUCTION -> {
                    DateField(
                        value = producedText,
                        onValueChange = { producedText = it },
                        label = "Дата изготовления",
                        hint = "ДД.ММ.ГГГГ",
                        parse = ::parseDate
                    )
                    AmountField(value = amountText, onValueChange = { amountText = it }, label = "Срок годности")
                    ChoiceChips(options = unitOptions, selected = unit, onSelect = { unit = it })
                    if (expiresAt != null) {
                        Text(
                            text = "Годен до ${formatDate(expiresAt)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            HorizontalDivider()
            AmountField(
                value = openedAmountText,
                onValueChange = { openedAmountText = it },
                label = "Годен после вскрытия",
                hint = "Необязательно. Например, «хранить 5 суток после вскрытия»"
            )
            ChoiceChips(options = unitOptions, selected = openedUnit, onSelect = { openedUnit = it })

            Button(
                onClick = {
                    if (expiresAt != null) onSave(ItemDraft(name.trim(), expiresAt, daysAfterOpening))
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Отмена")
            }
        }
    }
}

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

    OutlinedTextField(
        value = TextFieldValue(value, TextRange(value.length)),
        onValueChange = { onValueChange(formatDateDigits(it.text)) },
        label = { Text(label) },
        placeholder = { Text("15.10.2026") },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(painter = painterResource(R.drawable.ic_calendar), contentDescription = "Выбрать дату")
            }
        },
        isError = showError,
        supportingText = { Text(if (showError) "Нет такой даты" else hint) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )

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
                Text("Готово")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun AmountField(value: String, onValueChange: (String) -> Unit, label: String, hint: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(4)) },
        label = { Text(label) },
        supportingText = if (hint != null) {
            { Text(hint) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun <T> ChoiceChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
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
