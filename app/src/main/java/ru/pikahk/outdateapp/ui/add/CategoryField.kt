package ru.pikahk.outdateapp.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.ui.CategoryUi
import ru.pikahk.outdateapp.ui.label

private const val CATEGORY_NAME_LIMIT = 30

@Composable
internal fun CategoryField(
    categories: List<CategoryUi>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onCreate: (String) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    var creating by rememberSaveable { mutableStateOf(false) }
    val labels = categories.associate { it.id to it.label() }
    val selected = categories.firstOrNull { it.id == selectedId }
    val textColor = if (selected != null) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    LabeledField(label = stringResource(R.string.field_category)) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(FieldShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = FieldShape)
                    .clickable(role = Role.DropdownList) { expanded = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected?.let { labels[it.id] } ?: stringResource(R.string.category_none),
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.category_none)) },
                    onClick = {
                        onSelect(null)
                        expanded = false
                    }
                )
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(labels.getValue(category.id)) },
                        onClick = {
                            onSelect(category.id)
                            expanded = false
                        }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.category_create),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        expanded = false
                        creating = true
                    }
                )
            }
        }
    }

    if (creating) {
        NewCategoryDialog(
            onConfirm = { name ->
                val existing = categories.firstOrNull { labels[it.id].equals(name, ignoreCase = true) }
                onSelect(existing?.id ?: onCreate(name))
                creating = false
            },
            onDismiss = { creating = false }
        )
    }
}

@Composable
private fun NewCategoryDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_new_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(CATEGORY_NAME_LIMIT) },
                placeholder = { Text(stringResource(R.string.category_name_hint)) },
                singleLine = true,
                shape = FieldShape,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
