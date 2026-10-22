package ru.pikahk.outdateapp.ui.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.repository.ProductLookup
import ru.pikahk.outdateapp.domain.normalizeBarcode

private const val FRAME_RATIO = 1.6f

@Composable
fun BarcodeScanner(
    onLookup: suspend (String) -> ProductLookup,
    onResult: (String, ProductLookup) -> Unit,
    onClose: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var code by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(code) {
        val detected = code ?: return@LaunchedEffect
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onResult(detected, onLookup(detected))
    }

    CameraScanner(
        frameRatio = FRAME_RATIO,
        hint = stringResource(R.string.scan_hint),
        highlighted = code != null,
        onClose = onClose,
        attachAnalyzer = { controller, executor ->
            val scanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_DATA_MATRIX
                    )
                    .build()
            )
            val analyzer = MlKitAnalyzer(
                listOf(scanner),
                ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                executor
            ) { result ->
                if (code == null) {
                    code = result.getValue(scanner)?.firstNotNullOfOrNull { barcode ->
                        barcode.rawValue?.let(::normalizeBarcode)
                    }
                }
            }
            controller.setImageAnalysisAnalyzer(executor, analyzer)
            scanner
        }
    ) {
        val detected = code
        if (detected != null) {
            ScannerSheet(modifier = Modifier.align(Alignment.BottomCenter)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 3.dp)
                    Column {
                        Text(text = stringResource(R.string.scan_searching), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = detected,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
