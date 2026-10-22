package ru.pikahk.outdateapp.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.ui.containerColor

@Composable
fun ScanNoteCard(note: ScanNote, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val background = if (note.success) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        Urgency.SOON.containerColor()
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = stringResource(note.title),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        if (note.text != null) {
            Text(
                text = stringResource(note.text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (note == ScanNote.NO_CAMERA) {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.scan_open_settings))
            }
        }
    }
}
