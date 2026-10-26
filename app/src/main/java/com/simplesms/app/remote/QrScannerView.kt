package com.simplesms.app.remote

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.simplesms.app.ui.theme.EmeraldTick
import java.util.concurrent.Executors

@Composable
fun QrScannerDialog(
    onQrScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasScanned by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val executor = Executors.newSingleThreadExecutor()
                    val scanner = BarcodeScanning.getClient()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        processImageProxy(scanner, imageProxy) { rawValue ->
                            if (!hasScanned) {
                                hasScanned = true
                                ContextCompat.getMainExecutor(ctx).execute {
                                    onQrScanned(rawValue)
                                }
                            }
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("QrScanner", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay with scanner viewfinder
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = EmeraldTick,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Scan Web Portal QR",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            // Viewfinder Target Frame
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(3.dp, EmeraldTick, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Center crosshair
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                )
            }

            // Bottom Instructions
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Point camera at your laptop or PC screen",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Open the Simple SMS Web Portal on your browser to show the pairing QR code.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        onSuccess(rawValue)
                        break
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

/**
 * An embedded, in-card QR scanner matching the exact design in Screen 2:
 * 4 rounded corner brackets ⌜ ⌝ ⌞ ⌟, animated laser beam, center QR icon,
 * and live CameraX barcode feed.
 */
@Composable
fun EmbeddedQrScannerBox(
    modifier: Modifier = Modifier,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasScannedLocal by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "laserSweep")
    val laserFraction by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserFraction"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(255.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F172A))
            .clickable(enabled = !hasCameraPermission) { onRequestPermission() },
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val executor = Executors.newSingleThreadExecutor()
                            val scanner = BarcodeScanning.getClient()

                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                processImageProxy(scanner, imageProxy) { rawValue ->
                                    if (!hasScannedLocal) {
                                        hasScannedLocal = true
                                        ContextCompat.getMainExecutor(ctx).execute {
                                            onQrScanned(rawValue)
                                        }
                                    }
                                }
                            }

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("EmbeddedQrScanner", "Binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission placeholder inside box
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Text(
                    text = "Tap to enable camera",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Camera access is needed to scan the portal QR code",
                    color = Color.LightGray,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Overlay 1: 4 Corner White Brackets ⌜ ⌝ ⌞ ⌟
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.5.dp.toPx()
            val cornerLength = 30.dp.toPx()
            val cornerRadius = 12.dp.toPx()
            val pad = 16.dp.toPx()
            val strokeColor = Color.White

            // Top-Left corner: ⌜
            val tlPath = Path().apply {
                moveTo(pad, pad + cornerLength)
                lineTo(pad, pad + cornerRadius)
                quadraticBezierTo(pad, pad, pad + cornerRadius, pad)
                lineTo(pad + cornerLength, pad)
            }
            drawPath(tlPath, strokeColor, style = Stroke(strokeWidth, cap = StrokeCap.Round))

            // Top-Right corner: ⌝
            val trPath = Path().apply {
                moveTo(size.width - pad - cornerLength, pad)
                lineTo(size.width - pad - cornerRadius, pad)
                quadraticBezierTo(size.width - pad, pad, size.width - pad, pad + cornerRadius)
                lineTo(size.width - pad, pad + cornerLength)
            }
            drawPath(trPath, strokeColor, style = Stroke(strokeWidth, cap = StrokeCap.Round))

            // Bottom-Left corner: ⌞
            val blPath = Path().apply {
                moveTo(pad, size.height - pad - cornerLength)
                lineTo(pad, size.height - pad - cornerRadius)
                quadraticBezierTo(pad, size.height - pad, pad + cornerRadius, size.height - pad)
                lineTo(pad + cornerLength, size.height - pad)
            }
            drawPath(blPath, strokeColor, style = Stroke(strokeWidth, cap = StrokeCap.Round))

            // Bottom-Right corner: ⌟
            val brPath = Path().apply {
                moveTo(size.width - pad - cornerLength, size.height - pad)
                lineTo(size.width - pad - cornerRadius, size.height - pad)
                quadraticBezierTo(size.width - pad, size.height - pad, size.width - pad, size.height - pad - cornerRadius)
                lineTo(size.width - pad, size.height - pad - cornerLength)
            }
            drawPath(brPath, strokeColor, style = Stroke(strokeWidth, cap = StrokeCap.Round))
        }

        // Overlay 2: Animated glowing blue scanning laser beam
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-100.dp + (200.dp * laserFraction)))
                .height(2.5.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFF0057FF).copy(alpha = 0.6f),
                            Color(0xFF38BDF8),
                            Color(0xFF0057FF).copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Overlay 3: Center translucent box with QR icon and helper text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                text = "Tap to scan QR code",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            )
            Text(
                text = "Position the QR code within the frame",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 11.5.sp
            )
        }
    }
}
