package ru.pikahk.outdateapp.ui.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.time.LocalDate
import kotlinx.coroutines.delay
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.domain.DateReading
import ru.pikahk.outdateapp.domain.formatDate
import ru.pikahk.outdateapp.domain.readPackageDates

private const val FRAME_RATIO = 3.2f
private const val HINT_DELAY_MS = 10_000L

@Composable
fun DateScanner(onConfirm: (DateReading) -> Unit, onClose: () -> Unit) {
    val today = remember { LocalDate.now() }
    val haptic = LocalHapticFeedback.current
    var candidate by remember { mutableStateOf<DateReading?>(null) }
    var found by remember { mutableStateOf<DateReading?>(null) }
    var showHint by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(found, attempt) {
        if (found != null) {
            showHint = false
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            delay(HINT_DELAY_MS)
            showHint = true
        }
    }

    CameraScanner(
        frameRatio = FRAME_RATIO,
        hint = stringResource(R.string.date_hint),
        highlighted = found != null,
        onClose = onClose,
        attachAnalyzer = { controller, executor ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val analyzer = MlKitAnalyzer(
                listOf(recognizer),
                ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                executor
            ) { result ->
                if (found == null) {
                    val reading = result.getValue(recognizer)?.text?.let { readPackageDates(it, today) }
                    if (reading != null && reading == candidate) found = reading else candidate = reading
                }
            }
            controller.setImageAnalysisAnalyzer(executor, analyzer)
            recognizer
        }
    ) {
        val sheetModifier = Modifier.align(Alignment.BottomCenter)
        when (val reading = found) {
            is DateReading.Expiry -> ResultSheet(
                rows = listOfNotNull(
                    R.string.mode_expiry to reading.expiresAt,
                    reading.producedAt?.let { R.string.mode_production to it }
                ),
                onRetry = {
                    found = null
                    candidate = null
                },
                onConfirm = { onConfirm(reading) },
                modifier = sheetModifier
            )

            is DateReading.Production -> ResultSheet(
                rows = listOf(R.string.mode_production to reading.producedAt),
                onRetry = {
                    found = null
                    candidate = null
                },
                onConfirm = { onConfirm(reading) },
                modifier = sheetModifier
            )

            is DateReading.Unclear -> QuestionSheet(
                date = reading.date,
                onProduction = { onConfirm(DateReading.Production(reading.date)) },
                onExpiry = { onConfirm(DateReading.Expiry(reading.date, null)) },
                modifier = sheetModifier
            )

            null -> if (showHint) {
                HintSheet(
                    onManual = onClose,
                    onContinue = {
                        showHint = false
                        attempt++
                    },
                    modifier = sheetModifier
                )
            }
        }
    }
}

@Composable
private fun ResultSheet(
    rows: List<Pair<Int, LocalDate>>,
    onRetry: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScannerSheet(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { (label, date) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = stringResource(label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(date),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            SheetButtons(
                secondary = R.string.date_again,
                onSecondary = onRetry,
                primary = R.string.date_confirm,
                onPrimary = onConfirm
            )
        }
    }
}

@Composable
private fun QuestionSheet(
    date: LocalDate,
    onProduction: () -> Unit,
    onExpiry: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScannerSheet(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.date_question, formatDate(date)),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            SheetButtons(
                secondary = R.string.mode_production,
                onSecondary = onProduction,
                primary = R.string.mode_expiry,
                onPrimary = onExpiry
            )
        }
    }
}

@Composable
private fun HintSheet(onManual: () -> Unit, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    ScannerSheet(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.date_failed_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.date_failed_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SheetButtons(
                secondary = R.string.date_manual,
                onSecondary = onManual,
                primary = R.string.date_continue,
                onPrimary = onContinue
            )
        }
    }
}

@Composable
private fun SheetButtons(secondary: Int, onSecondary: () -> Unit, primary: Int, onPrimary: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onSecondary, shape = RoundedCornerShape(11.dp), modifier = Modifier.weight(1f)) {
            Text(stringResource(secondary))
        }
        Button(onClick = onPrimary, shape = RoundedCornerShape(11.dp), modifier = Modifier.weight(1f)) {
            Text(stringResource(primary))
        }
    }
}
