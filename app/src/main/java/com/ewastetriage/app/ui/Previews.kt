package com.ewastetriage.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ewastetriage.app.guidance.GuidanceEntry
import com.ewastetriage.app.guidance.HarmfulComponent
import com.ewastetriage.app.guidance.HazardType
import com.ewastetriage.app.guidance.ReusableComponent
import com.ewastetriage.app.guidance.Severity
import com.ewastetriage.app.history.HistoryEntry
import com.ewastetriage.app.history.HistoryItem
import com.ewastetriage.app.ui.components.GuidanceDetail
import com.ewastetriage.app.ui.theme.CameraOverlayDefaults
import com.ewastetriage.app.ui.theme.EWasteTriageTheme
import com.ewastetriage.app.ui.theme.severityColorFor

private val sampleGuidance = GuidanceEntry(
    displayName = "CRT Monitor / TV",
    danger = true,
    composition = "Evacuated glass tube, phosphor coating, electron gun, high-voltage circuitry.",
    verifyHint = "Check for a deep, bulky back and curved glass.",
    alwaysVerify = true,
    reusable = listOf(
        ReusableComponent("Copper yoke", "Recoverable copper."),
        ReusableComponent("Steel frame", "Recyclable steel once tube is removed.")
    ),
    harmful = listOf(
        HarmfulComponent(
            name = "Vacuum glass tube",
            risk = "A cracked tube can implode violently.",
            handling = "Never strike or drop. Wear eye shield and gloves.",
            severity = Severity.HIGH,
            type = HazardType.GLASS
        ),
        HarmfulComponent(
            name = "High-voltage stored charge",
            risk = "Capacitors can retain dangerous electrical charge.",
            handling = "Do not open unless trained to discharge safely.",
            severity = Severity.HIGH,
            type = HazardType.ELECTRICAL
        )
    )
)

private val sampleHistoryEntry = HistoryEntry(
    id = "1",
    timestamp = System.currentTimeMillis(),
    dateString = "2024-10-26 19:30:00",
    source = "Live Camera",
    items = listOf(
        HistoryItem("crt", "CRT Monitor", 0.88f, "confident"),
        HistoryItem("battery_liion", "Li-ion Battery", 0.74f, "possible")
    )
)

// --- 1. Category Card Previews ---

@Preview(name = "Category Card - Light", showBackground = true)
@Composable
fun PreviewCategoryCardLight() {
    EWasteTriageTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            CategoryCard(
                categoryKey = "crt",
                entry = sampleGuidance,
                onClick = {}
            )
        }
    }
}

@Preview(name = "Category Card - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewCategoryCardDark() {
    EWasteTriageTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            CategoryCard(
                categoryKey = "crt",
                entry = sampleGuidance,
                onClick = {}
            )
        }
    }
}

// --- 2. Guidance Detail Previews ---

@Preview(name = "Guidance Detail - Light", showBackground = true)
@Composable
fun PreviewGuidanceDetailLight() {
    EWasteTriageTheme {
        Surface {
            GuidanceDetail(
                guidance = sampleGuidance,
                categoryKey = "crt",
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "Guidance Detail - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewGuidanceDetailDark() {
    EWasteTriageTheme {
        Surface {
            GuidanceDetail(
                guidance = sampleGuidance,
                categoryKey = "crt",
                onDismiss = {}
            )
        }
    }
}

// --- 3. Detection Chip Previews ---

@Preview(name = "Detection Chip - Light", showBackground = true)
@Composable
fun PreviewDetectionChipLight() {
    EWasteTriageTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AssistChip(
                onClick = {},
                label = { Text("CRT Monitor 88%") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = severityColorFor("crt", "confident")
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Preview(name = "Detection Chip - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewDetectionChipDark() {
    EWasteTriageTheme {
        Box(modifier = Modifier.background(CameraOverlayDefaults.cardBackground).padding(16.dp)) {
            AssistChip(
                onClick = {},
                label = { Text("CRT Monitor 88%") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = severityColorFor("crt", "confident")
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = CameraOverlayDefaults.cardBackgroundElevated,
                    labelColor = CameraOverlayDefaults.textPrimary
                )
            )
        }
    }
}

// --- 4. History Entry Previews ---

@Preview(name = "History Entry - Light", showBackground = true)
@Composable
fun PreviewHistoryEntryLight() {
    EWasteTriageTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            // Standalone mock of HistoryEntry card
            Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = sampleHistoryEntry.source + " - " + sampleHistoryEntry.dateString,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    for (item in sampleHistoryEntry.items) {
                        Text(
                            text = item.displayName + " (${(item.confidence * 100).toInt()}%)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "History Entry - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewHistoryEntryDark() {
    EWasteTriageTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = sampleHistoryEntry.source + " - " + sampleHistoryEntry.dateString,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    for (item in sampleHistoryEntry.items) {
                        Text(
                            text = item.displayName + " (${(item.confidence * 100).toInt()}%)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
