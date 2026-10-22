package ru.pikahk.outdateapp.ui.scan

import androidx.activity.compose.BackHandler
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.repository.ProductLookup
import ru.pikahk.outdateapp.domain.normalizeBarcode
import ru.pikahk.outdateapp.ui.theme.GreenDark

private const val FRAME_WIDTH = 0.78f
private const val FRAME_RATIO = 1.6f
private val FRAME_CORNER = 16.dp

@Composable
fun BarcodeScanner(
    onLookup: suspend (String) -> ProductLookup,
    onResult: (String, ProductLookup) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    var code by remember { mutableStateOf<String?>(null) }
    var torch by remember { mutableStateOf(false) }
    val controller = remember {
        LifecycleCameraController(context).apply { setEnabledUseCases(CameraController.IMAGE_ANALYSIS) }
    }

    DisposableEffect(controller, lifecycleOwner) {
        val executor = ContextCompat.getMainExecutor(context)
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
        val analyzer = MlKitAnalyzer(listOf(scanner), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, executor) { result ->
            if (code == null) {
                code = result.getValue(scanner)?.firstNotNullOfOrNull { barcode ->
                    barcode.rawValue?.let(::normalizeBarcode)
                }
            }
        }
        controller.setImageAnalysisAnalyzer(executor, analyzer)
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.unbind()
            scanner.close()
        }
    }

    LaunchedEffect(code) {
        val detected = code ?: return@LaunchedEffect
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onResult(detected, onLookup(detected))
    }

    LaunchedEffect(torch) {
        controller.enableTorch(torch)
    }

    BackHandler(onBack = onClose)

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val frameWidth = maxWidth * FRAME_WIDTH
        val frameHeight = frameWidth / FRAME_RATIO

        AndroidView(
            factory = { viewContext ->
                PreviewView(viewContext).apply {
                    this.controller = controller
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            val hole = Size(frameWidth.toPx(), frameHeight.toPx())
            drawRect(Color.Black.copy(alpha = 0.45f))
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset((size.width - hole.width) / 2, (size.height - hole.height) / 2),
                size = hole,
                cornerRadius = CornerRadius(FRAME_CORNER.toPx()),
                blendMode = BlendMode.Clear
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(frameWidth, frameHeight)
                .border(2.dp, if (code == null) Color.White else GreenDark, RoundedCornerShape(FRAME_CORNER))
        )
        Text(
            text = stringResource(R.string.scan_hint),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = frameHeight / 2 + 36.dp)
                .padding(horizontal = 32.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RoundButton(icon = R.drawable.ic_close, label = R.string.scan_close, onClick = onClose)
            RoundButton(icon = R.drawable.ic_flash, label = R.string.scan_torch, onClick = { torch = !torch })
        }
        val detected = code
        if (detected != null) {
            SearchingSheet(
                code = detected,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun RoundButton(icon: Int, label: Int, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), CircleShape)
    ) {
        Icon(painter = painterResource(icon), contentDescription = stringResource(label), tint = Color.White)
    }
}

@Composable
private fun SearchingSheet(code: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 3.dp)
        Column {
            Text(text = stringResource(R.string.scan_searching), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
