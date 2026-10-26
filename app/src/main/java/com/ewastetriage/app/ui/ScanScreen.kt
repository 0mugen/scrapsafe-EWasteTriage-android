package com.ewastetriage.app.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ewastetriage.app.guidance.Severity
import com.ewastetriage.app.triage.TriageResult
import com.ewastetriage.app.ui.components.GuidanceDetail
import com.ewastetriage.app.ui.theme.CameraOverlayDefaults
import com.ewastetriage.app.ui.theme.semantic
import com.ewastetriage.app.ui.theme.severityColorFor
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    vm: ScanViewModel,
    onOpenGallery: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val state by vm.state.collectAsState()
    val isFrozen by vm.isFrozen.collectAsState()
    val strictnessOffset by vm.strictnessOffset.collectAsState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedResult by remember { mutableStateOf<TriageResult?>(null) }
    var showSensitivitySheet by remember { mutableStateOf(false) }

    // Haptic feedback trigger for high-severity items
    var hasTriggeredHighHazardHaptic by remember { mutableStateOf(false) }
    val detectedList = (state as? ScanState.Detected)?.results ?: emptyList()

    LaunchedEffect(detectedList) {
        val hasHighHazard = detectedList.any { r ->
            val g = vm.getGuidance(r.className)
            g?.overallSeverity == Severity.HIGH
        }
        if (hasHighHazard && !hasTriggeredHighHazardHaptic) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            hasTriggeredHighHazardHaptic = true
        } else if (!hasHighHazard) {
            hasTriggeredHighHazardHaptic = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Camera viewfinder (FIT_CENTER for exact aspect-ratio alignment)
        CameraPreview(vm)

        // 2. Reticle corner brackets overlay
        ScanningReticle(isScanning = state is ScanState.Scanning && !isFrozen)

        // 3. Bounding box overlay with filled text tags
        if (detectedList.isNotEmpty()) {
            DetectionOverlay(
                results = detectedList,
                onBoxSelected = { selectedResult = it }
            )
        }

        // 4. Top section: Stacked Column (Toolbar + Verify Banner)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            // Frosted pill group with 48dp IconButtons
            Surface(
                color = CameraOverlayDefaults.cardBackground,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CameraOverlayDefaults.border),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    // Freeze / Resume
                    IconButton(
                        onClick = { vm.toggleFreeze() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isFrozen) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isFrozen) "Resume scanning" else "Freeze frame",
                            tint = if (isFrozen) MaterialTheme.colorScheme.primary else CameraOverlayDefaults.textPrimary
                        )
                    }

                    // Save (only if detections present)
                    AnimatedVisibility(visible = detectedList.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                vm.saveCurrentDetections(detectedList, source = "Live Camera")
                                coroutineScope.launch {
                                    val action = snackbarHostState.showSnackbar(
                                        message = "Saved scan to history",
                                        actionLabel = "View",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (action == SnackbarResult.ActionPerformed) {
                                        onNavigateToHistory()
                                    }
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SaveAlt,
                                contentDescription = "Save scan to history",
                                tint = CameraOverlayDefaults.textPrimary
                            )
                        }
                    }

                    // Gallery photo analysis
                    IconButton(
                        onClick = onOpenGallery,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Test photo from gallery",
                            tint = CameraOverlayDefaults.textPrimary
                        )
                    }

                    // Sensitivity / Tuning
                    IconButton(
                        onClick = { showSensitivitySheet = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Adjust sensitivity",
                            tint = if (strictnessOffset != 0f) MaterialTheme.colorScheme.primary else CameraOverlayDefaults.textPrimary
                        )
                    }
                }
            }

            // Verify banner (if applicable) stacked below toolbar
            val verifyHint = detectedList.firstNotNullOfOrNull { r ->
                val g = vm.getGuidance(r.className)
                if (g?.alwaysVerify == true || r.tier == "possible") g?.verifyHint else null
            }

            AnimatedVisibility(visible = verifyHint != null) {
                Surface(
                    color = CameraOverlayDefaults.cardBackgroundElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.semantic.verifyWarning.color.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = null,
                            tint = MaterialTheme.semantic.verifyWarning.color,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = verifyHint ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = CameraOverlayDefaults.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 5. Bottom Panel: Rounded card with status and detection chips
        Surface(
            color = CameraOverlayDefaults.cardBackground,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CameraOverlayDefaults.border),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Status line with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (state) {
                        is ScanState.Initializing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Loading model...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CameraOverlayDefaults.textSecondary
                            )
                        }
                        is ScanState.Scanning -> {
                            Icon(
                                imageVector = if (isFrozen) Icons.Default.Pause else Icons.Default.Sensors,
                                contentDescription = null,
                                tint = if (isFrozen) MaterialTheme.colorScheme.primary else CameraOverlayDefaults.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFrozen) "Frozen - tap box or chip for safety details" else "Scanning for electronic waste...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CameraOverlayDefaults.textPrimary
                            )
                        }
                        is ScanState.NothingFound -> {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.semantic.verifyWarning.color,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No supported hazard detected - this does not mean the item is safe",
                                style = MaterialTheme.typography.bodySmall,
                                color = CameraOverlayDefaults.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        is ScanState.Detected -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${detectedList.size} item(s) detected - tap for safety guidance",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CameraOverlayDefaults.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        is ScanState.Error -> {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (state as ScanState.Error).message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Detection chips row
                if (detectedList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        detectedList.forEach { r ->
                            val boxColor = severityColorFor(r.className, r.tier)
                            val isPossible = r.tier == "possible"
                            val guidance = vm.getGuidance(r.className)
                            val severity = guidance?.overallSeverity ?: Severity.MEDIUM

                            AssistChip(
                                onClick = { selectedResult = r },
                                label = {
                                    Text(
                                        text = "${if (isPossible) "Possible " else ""}${r.displayName} ${(r.confidence * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                leadingIcon = {
                                    val icon = when (severity) {
                                        Severity.HIGH -> Icons.Default.Warning
                                        Severity.MEDIUM -> Icons.Default.ReportProblem
                                        Severity.LOW -> Icons.Default.Info
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = boxColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "View details",
                                        tint = CameraOverlayDefaults.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = CameraOverlayDefaults.cardBackgroundElevated,
                                    labelColor = CameraOverlayDefaults.textPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // SnackbarHost on top
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        )
    }

    // ModalBottomSheet for guidance detail
    selectedResult?.let { result ->
        val guidance = vm.getGuidance(result.className)
        ModalBottomSheet(
            onDismissRequest = { selectedResult = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            GuidanceDetail(
                result = result,
                guidance = guidance,
                categoryKey = result.className,
                onDismiss = { selectedResult = null }
            )
        }
    }

    // Sensitivity tuning bottom sheet
    if (showSensitivitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showSensitivitySheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sensitivity",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { vm.setStrictnessOffset(0.0f) }) {
                        Text("Reset")
                    }
                }

                Text(
                    text = "Lower strictness detects more items; higher strictness reduces false alarms.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Slider(
                    value = strictnessOffset,
                    onValueChange = { vm.setStrictnessOffset(it) },
                    valueRange = -0.15f..0.30f,
                    steps = 8
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "More sensitive",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Stricter",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { showSensitivitySheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
fun DetectionOverlay(
    results: List<TriageResult>,
    onBoxSelected: (TriageResult) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val boxColors = results.associateWith { severityColorFor(it.className, it.tier) }
    val defaultColor = MaterialTheme.semantic.severityMedium.color

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(results) {
                detectTapGestures { tapOffset ->
                    val tapX = tapOffset.x / size.width
                    val tapY = tapOffset.y / size.height
                    // Only open guidance when tap lands inside a box
                    val hit = results.firstOrNull { r ->
                        tapX in r.x1..r.x2 && tapY in r.y1..r.y2
                    }
                    hit?.let { onBoxSelected(it) }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            results.forEach { r ->
                val boxColor = boxColors[r] ?: defaultColor

                val strokeWidth = if (r.className == "crt" || r.className == "battery_swollen" || r.className == "battery_liion") 4.dp.toPx() else 2.5.dp.toPx()
                val isPossible = r.tier == "possible"
                val pathEffect = if (isPossible) PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f) else null

                val x1 = r.x1 * size.width
                val y1 = r.y1 * size.height
                val x2 = r.x2 * size.width
                val y2 = r.y2 * size.height
                val boxWidth = x2 - x1
                val boxHeight = y2 - y1

                // 1. Draw rounded bounding box
                drawRoundRect(
                    color = boxColor,
                    topLeft = Offset(x1, y1),
                    size = Size(boxWidth, boxHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(width = strokeWidth, pathEffect = pathEffect)
                )

                // 2. Draw filled tag at top-left
                val labelText = "${if (isPossible) "Possible " else ""}${r.displayName} ${(r.confidence * 100).toInt()}%"
                val textLayout = textMeasurer.measure(
                    text = labelText,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                val tagHeight = textLayout.size.height + 8f
                val tagWidth = textLayout.size.width + 16f
                val tagY = (y1 - tagHeight).coerceAtLeast(0f)

                drawRoundRect(
                    color = boxColor,
                    topLeft = Offset(x1, tagY),
                    size = Size(tagWidth, tagHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(x1 + 8f, tagY + 4f)
                )
            }
        }
    }
}

@Composable
fun ScanningReticle(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "reticle_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reticle_alpha"
    )

    if (!isScanning) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val reticleSize = size.width.coerceAtMost(size.height) * 0.65f
        val left = cx - reticleSize / 2f
        val right = cx + reticleSize / 2f
        val top = cy - reticleSize / 2f
        val bottom = cy + reticleSize / 2f
        val cornerLen = 28.dp.toPx()
        val stroke = 2.5.dp.toPx()
        val color = Color.White.copy(alpha = alpha)

        // Top-left corner
        drawLine(color, Offset(left, top), Offset(left + cornerLen, top), stroke)
        drawLine(color, Offset(left, top), Offset(left, top + cornerLen), stroke)

        // Top-right corner
        drawLine(color, Offset(right, top), Offset(right - cornerLen, top), stroke)
        drawLine(color, Offset(right, top), Offset(right, top + cornerLen), stroke)

        // Bottom-left corner
        drawLine(color, Offset(left, bottom), Offset(left + cornerLen, bottom), stroke)
        drawLine(color, Offset(left, bottom), Offset(left, bottom - cornerLen), stroke)

        // Bottom-right corner
        drawLine(color, Offset(right, bottom), Offset(right - cornerLen, bottom), stroke)
        drawLine(color, Offset(right, bottom), Offset(right, bottom - cornerLen), stroke)
    }
}

@Composable
fun CameraPreview(vm: ScanViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                // FIT_CENTER prevents cropping so bounding box normalized coords map 1:1
                scaleType = PreviewView.ScaleType.FIT_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    try {
                        val bitmap = imageProxy.toBitmap()
                        val rotated = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
                        vm.onFrame(rotated)
                    } catch (e: Exception) {
                        Log.e("ScanScreen", "Frame analysis error", e)
                    } finally {
                        imageProxy.close()
                    }
                }

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("ScanScreen", "Camera bind failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    if (degrees == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
