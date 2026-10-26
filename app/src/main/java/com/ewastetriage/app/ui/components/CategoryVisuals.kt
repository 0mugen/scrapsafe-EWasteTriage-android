package com.ewastetriage.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ewastetriage.app.guidance.HazardType
import com.ewastetriage.app.guidance.Severity
import com.ewastetriage.app.ui.theme.semanticColorForSeverity

fun iconForCategory(className: String): ImageVector {
    return when (className) {
        "smartphone" -> Icons.Default.PhoneAndroid
        "laptop" -> Icons.Default.Laptop
        "crt" -> Icons.Default.Tv
        "battery_liion", "battery_swollen" -> Icons.Default.BatteryChargingFull
        "battery_alkaline" -> Icons.Default.BatteryStd
        "flat_panel_monitor" -> Icons.Default.Monitor
        else -> Icons.Default.Category
    }
}

fun iconForHazardType(type: HazardType): ImageVector {
    return when (type) {
        HazardType.FIRE -> Icons.Default.LocalFireDepartment
        HazardType.TOXIC -> Icons.Default.Science
        HazardType.ELECTRICAL -> Icons.Default.Bolt
        HazardType.GLASS -> Icons.Default.BrokenImage
        HazardType.CORROSIVE -> Icons.Default.WaterDrop
        HazardType.UNKNOWN -> Icons.Default.Warning
    }
}

@Composable
fun SeverityBadge(
    severity: Severity,
    modifier: Modifier = Modifier
) {
    val sem = semanticColorForSeverity(severity)
    Surface(
        color = sem.container,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val icon = when (severity) {
                Severity.HIGH -> Icons.Default.Warning
                Severity.MEDIUM -> Icons.Default.ReportProblem
                Severity.LOW -> Icons.Default.Info
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = sem.onContainer,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = severity.label,
                style = MaterialTheme.typography.labelSmall,
                color = sem.onContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
