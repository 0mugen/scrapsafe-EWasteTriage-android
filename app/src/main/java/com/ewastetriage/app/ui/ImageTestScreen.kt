package com.ewastetriage.app.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ewastetriage.app.guidance.Severity
import com.ewastetriage.app.triage.TriageResult
import com.ewastetriage.app.ui.components.GuidanceDetail
import com.ewastetriage.app.ui.components.SeverityBadge
import com.ewastetriage.app.ui.components.iconForCategory
import com.ewastetriage.app.ui.theme.semantic
import com.ewastetriage.app.ui.theme.semanticColorForSeverity
import com.ewastetriage.app.ui.theme.severityColorFor
import com.ewastetriage.app.util.BitmapUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageTestScreen(
    vm: ScanViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detections by remember { mutableStateOf<List<TriageResult>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var selectedResultForGuidance by remember { mutableStateOf<TriageResult?>(null) }
    val textMeasurer = rememberTextMeasurer()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bmp = BitmapUtils.loadFromUri(context, uri)
            selectedBitmap = bmp
            if (bmp != null) {
                isAnalyzing = true
                coroutineScope.launch {
                    detections = vm.analyzeStillImage(bmp)
                    isAnalyzing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Photo Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to camera"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Test an e-waste image from your device gallery to identify supported hazards.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Button(
                onClick = { pickerLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choose photo", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isAnalyzing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Analysing photo...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            selectedBitmap?.let { bmp ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Selected photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        val boxColors = detections.associateWith { severityColorFor(it.className, it.tier) }
                        val defaultColor = MaterialTheme.semantic.severityMedium.color

                        // Bounding box overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            detections.forEach { r ->
                                val boxColor = boxColors[r] ?: defaultColor

                                val strokeWidth = if (r.className == "crt" || r.className == "battery_swollen" || r.className == "battery_liion") 4.dp.toPx() else 2.5.dp.toPx()
                                val isPossible = r.tier == "possible"
                                val pathEffect = if (isPossible) PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f) else null

                                val x1 = r.x1 * size.width
                                val y1 = r.y1 * size.height
                                val x2 = r.x2 * size.width
                                val y2 = r.y2 * size.height

                                drawRoundRect(
                                    color = boxColor,
                                    topLeft = Offset(x1, y1),
                                    size = Size(x2 - x1, y2 - y1),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                    style = Stroke(width = strokeWidth, pathEffect = pathEffect)
                                )

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

                Spacer(modifier = Modifier.height(16.dp))

                if (detections.isEmpty() && !isAnalyzing) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.semantic.verifyWarning.container
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.semantic.verifyWarning.onContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "No supported hazard detected",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.semantic.verifyWarning.onContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "This does not mean the item is safe. The model only detects supported e-waste classes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.semantic.verifyWarning.onContainer
                                )
                            }
                        }
                    }
                } else if (detections.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detected items (${detections.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        FilledTonalButton(
                            onClick = {
                                vm.saveCurrentDetections(detections, source = "Gallery Photo")
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Saved to scan history")
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to history", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    detections.forEach { r ->
                        val isPossible = r.tier == "possible"
                        val guidance = vm.getGuidance(r.className)
                        val severity = guidance?.overallSeverity ?: Severity.MEDIUM
                        val sem = semanticColorForSeverity(severity)

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedResultForGuidance = r }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(sem.container),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconForCategory(r.className),
                                        contentDescription = null,
                                        tint = sem.onContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isPossible) "Possible: ${r.displayName}" else r.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        SeverityBadge(severity = severity)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Confidence: ${(r.confidence * 100).toInt()}% - Tap to view safety handling",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "View details",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ModalBottomSheet for guidance
    selectedResultForGuidance?.let { result ->
        val guidance = vm.getGuidance(result.className)
        ModalBottomSheet(
            onDismissRequest = { selectedResultForGuidance = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            GuidanceDetail(
                result = result,
                guidance = guidance,
                categoryKey = result.className,
                onDismiss = { selectedResultForGuidance = null }
            )
        }
    }
}
