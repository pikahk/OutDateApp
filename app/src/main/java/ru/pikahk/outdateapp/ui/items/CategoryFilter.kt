package ru.pikahk.outdateapp.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.ui.CategoryUi
import ru.pikahk.outdateapp.ui.label

@Composable
internal fun CategoryFilter(
    categories: List<CategoryUi>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDelete: (String) -> Unit
) {
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all") {
            CategoryChip(
                text = stringResource(R.string.category_all),
                selected = selectedId == null,
                onClick = { onSelect(null) }
            )
        }
        items(items = categories, key = { it.id }) { category ->
            val onLongClick: (() -> Unit)? = if (category.builtIn == null) {
                { deletingId = category.id }
            } else {
                null
            }
            CategoryChip(
                text = category.label(),
                selected = category.id == selectedId,
                onClick = { onSelect(category.id) },
                onLongClick = onLongClick
            )
        }
    }

    val deleting = categories.firstOrNull { it.id == deletingId }
    if (deleting != null) {
        DeleteCategoryDialog(
            name = deleting.label(),
            onConfirm = {
                onDelete(deleting.id)
                deletingId = null
            },
            onDismiss = { deletingId = null }
        )
    }
}

@Composable
private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val longClickLabel = if (onLongClick != null) stringResource(R.string.delete) else null
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .semantics { this.selected = selected }
            .combinedClickable(
                role = Role.Tab,
                onLongClickLabel = longClickLabel,
                onLongClick = onLongClick,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = content
        )
    }
}

@Composable
private fun DeleteCategoryDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_delete_title, name)) },
        text = { Text(stringResource(R.string.category_delete_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
