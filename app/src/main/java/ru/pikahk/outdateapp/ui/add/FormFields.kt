package ru.pikahk.outdateapp.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.domain.formatDate
import ru.pikahk.outdateapp.domain.formatDateDigits

@Composable
internal fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

internal val FieldShape = RoundedCornerShape(12.dp)

@Composable
internal fun fieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedBorderColor = MaterialTheme.colorScheme.primary
)

@Composable
internal fun DateField(
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
internal fun AmountField(value: String, onValueChange: (String) -> Unit, label: String, hint: String? = null) {
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
internal fun <T> Segmented(options: List<Pair<T, Int>>, selected: T, onSelect: (T) -> Unit) {
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

private fun isYearTyped(text: String): Boolean = text.trim().takeLastWhile { it.isDigit() }.length >= 4

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
