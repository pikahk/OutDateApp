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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import ru.pikahk.outdateapp.domain.ShelfLifeUnit
import ru.pikahk.outdateapp.domain.expiryFromProduction
import ru.pikahk.outdateapp.domain.formatDate
import ru.pikahk.outdateapp.domain.parseDate
import ru.pikahk.outdateapp.domain.parseExpiryDate
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
fun AddItemScreen(
    onSave: (name: String, expiresAt: LocalDate) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(InputMode.EXPIRY_DATE) }
    var expiryText by rememberSaveable { mutableStateOf("") }
    var producedText by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(ShelfLifeUnit.DAYS) }

    val expiresAt = when (mode) {
        InputMode.EXPIRY_DATE -> parseExpiryDate(expiryText)
        InputMode.PRODUCTION -> calculateExpiry(producedText, amountText, unit)
    }
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
                    isValid = expiresAt != null
                )

                InputMode.PRODUCTION -> {
                    DateField(
                        value = producedText,
                        onValueChange = { producedText = it },
                        label = "Дата изготовления",
                        hint = "ДД.ММ.ГГГГ",
                        isValid = parseDate(producedText) != null
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter(Char::isDigit).take(4) },
                        label = { Text("Срок годности") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ChoiceChips(options = unitOptions, selected = unit, onSelect = { unit = it })
                    if (expiresAt != null) {
                        Text(
                            text = "Годен до ${formatDate(expiresAt)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Button(
                onClick = { if (expiresAt != null) onSave(name.trim(), expiresAt) },
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
private fun DateField(value: String, onValueChange: (String) -> Unit, label: String, hint: String, isValid: Boolean) {
    val showError = !isValid && isYearTyped(value)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("15.10.2026") },
        isError = showError,
        supportingText = { Text(if (showError) "Нет такой даты" else hint) },
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
    val amount = amountText.toIntOrNull()?.takeIf { it > 0 } ?: return null
    return expiryFromProduction(producedAt, amount, unit)
}

private fun isYearTyped(text: String): Boolean = text.trim().takeLastWhile { it.isDigit() }.length >= 4

@Preview(showBackground = true)
@Composable
private fun AddItemScreenPreview() {
    OutDateAppTheme {
        AddItemScreen(onSave = { _, _ -> }, onCancel = {})
    }
}
