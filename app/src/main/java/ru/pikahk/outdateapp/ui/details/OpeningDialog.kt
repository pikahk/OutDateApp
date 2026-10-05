package ru.pikahk.outdateapp.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.domain.ShelfLifeUnit
import ru.pikahk.outdateapp.domain.shelfLifeInDays
import ru.pikahk.outdateapp.ui.add.FieldShape
import ru.pikahk.outdateapp.ui.add.Segmented
import ru.pikahk.outdateapp.ui.add.fieldColors
import ru.pikahk.outdateapp.ui.add.unitOptions

@Composable
internal fun OpeningDialog(onConfirm: (daysAfterOpening: Int?) -> Unit, onDismiss: () -> Unit) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(ShelfLifeUnit.DAYS) }
    val amount = amountText.toIntOrNull()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.field_after_opening)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit).take(4) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = FieldShape,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Segmented(options = unitOptions, selected = unit, onSelect = { unit = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount?.let { shelfLifeInDays(it, unit) }) },
                enabled = amount != null
            ) {
                Text(stringResource(R.string.open))
            }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(null) }) {
                Text(stringResource(R.string.opening_no_limit))
            }
        }
    )
}
