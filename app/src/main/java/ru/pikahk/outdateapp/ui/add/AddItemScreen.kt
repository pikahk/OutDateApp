package ru.pikahk.outdateapp.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import ru.pikahk.outdateapp.domain.parseExpiryDate
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    onSave: (name: String, expiresAt: LocalDate) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf("") }

    val expiresAt = parseExpiryDate(dateText)
    // Ошибку показываем, только когда дата набрана целиком, а не на каждом символе.
    val showDateError = dateText.length >= 10 && expiresAt == null
    val canSave = name.isNotBlank() && expiresAt != null

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Новый продукт") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text("Годен до") },
                placeholder = { Text("15.10.2026") },
                isError = showDateError,
                supportingText = { if (showDateError) Text("Нет такой даты. Формат: ДД.ММ.ГГГГ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
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

@Preview(showBackground = true)
@Composable
private fun AddItemScreenPreview() {
    OutDateAppTheme {
        AddItemScreen(onSave = { _, _ -> }, onCancel = {})
    }
}
