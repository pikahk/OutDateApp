package ru.pikahk.outdateapp.ui.scan

import androidx.activity.compose.BackHandler
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executor
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.ui.theme.GreenDark

private const val FRAME_WIDTH = 0.78f
private val FRAME_CORNER = 16.dp

@Composable
fun CameraScanner(
    frameRatio: Float,
    hint: String,
    highlighted: Boolean,
    onClose: () -> Unit,
    attachAnalyzer: (LifecycleCameraController, Executor) -> AutoCloseable,
    overlay: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var torch by remember { mutableStateOf(false) }
    val controller = remember {
        LifecycleCameraController(context).apply { setEnabledUseCases(CameraController.IMAGE_ANALYSIS) }
    }

    DisposableEffect(controller, lifecycleOwner) {
        val detector = attachAnalyzer(controller, ContextCompat.getMainExecutor(context))
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.unbind()
            detector.close()
        }
    }

    LaunchedEffect(torch) {
        controller.enableTorch(torch)
    }

    BackHandler(onBack = onClose)

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val frameWidth = maxWidth * FRAME_WIDTH
        val frameHeight = frameWidth / frameRatio

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
                .border(2.dp, if (highlighted) GreenDark else Color.White, RoundedCornerShape(FRAME_CORNER))
        )
        Text(
            text = hint,
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
        overlay()
    }
}

@Composable
fun ScannerSheet(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(12.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        content()
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
