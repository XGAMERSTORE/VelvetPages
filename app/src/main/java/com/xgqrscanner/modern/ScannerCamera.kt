package com.xgqrscanner.modern

import android.content.Context
import android.net.Uri
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

@Composable
fun ScannerCamera(
    modifier: Modifier = Modifier,
    onResult: (String, Int) -> Unit,
    onError: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val scanner = remember { BarcodeScanning.getClient() }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val lastDetection = remember { AtomicLong(0L) }
    val latestResult by rememberUpdatedState(onResult)
    val latestError by rememberUpdatedState(onError)
    var camera by remember { mutableStateOf<Camera?>(null) }
    var hasFlash by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(analysisExecutor) { proxy ->
                    val mediaImage = proxy.image
                    if (mediaImage == null) {
                        proxy.close()
                        return@setAnalyzer
                    }

                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        proxy.imageInfo.rotationDegrees
                    )

                    scanner.process(image)
                        .addOnSuccessListener { codes ->
                            val now = System.currentTimeMillis()
                            val code = codes.firstOrNull { !it.rawValue.isNullOrBlank() }
                            if (code != null && now - lastDetection.get() > 1600L) {
                                lastDetection.set(now)
                                latestResult(code.rawValue.orEmpty(), code.format)
                            }
                        }
                        .addOnFailureListener { latestError() }
                        .addOnCompleteListener { proxy.close() }
                }

                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
            } catch (_: Throwable) {
                latestError()
            }
        }, mainExecutor)

        onDispose {
            runCatching {
                if (future.isDone) future.get().unbindAll()
            }
            scanner.close()
            analysisExecutor.shutdown()
        }
    }

    val accent = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "scan")
    val scanPosition by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewView }
        )

        Canvas(Modifier.fillMaxSize()) {
            val frame = size.width * 0.66f
            val left = (size.width - frame) / 2f
            val top = (size.height - frame) / 2f
            drawRoundRect(
                color = accent.copy(alpha = 0.95f),
                topLeft = Offset(left, top),
                size = Size(frame, frame),
                cornerRadius = CornerRadius(38f, 38f),
                style = Stroke(width = 6f)
            )
            val y = top + frame * scanPosition
            drawLine(
                color = accent.copy(alpha = 0.9f),
                start = Offset(left + 18f, y),
                end = Offset(left + frame - 18f, y),
                strokeWidth = 5f
            )
        }

        if (hasFlash) {
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp),
                onClick = {
                    torchOn = !torchOn
                    camera?.cameraControl?.enableTorch(torchOn)
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Icon(
                    imageVector = if (torchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                    contentDescription = null
                )
            }
        }
    }
}

fun scanImageUri(
    context: Context,
    uri: Uri,
    onResult: (String, Int) -> Unit,
    onEmpty: () -> Unit
) {
    val scanner = BarcodeScanning.getClient()
    runCatching { InputImage.fromFilePath(context, uri) }
        .onSuccess { image ->
            scanner.process(image)
                .addOnSuccessListener { codes ->
                    val code = codes.firstOrNull { !it.rawValue.isNullOrBlank() }
                    if (code == null) onEmpty()
                    else onResult(code.rawValue.orEmpty(), code.format)
                }
                .addOnFailureListener { onEmpty() }
                .addOnCompleteListener { scanner.close() }
        }
        .onFailure {
            scanner.close()
            onEmpty()
        }
}
