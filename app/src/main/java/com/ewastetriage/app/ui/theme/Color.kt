package com.ewastetriage.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.ewastetriage.app.guidance.Severity

// --- Material 3 Palette ---

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF003300),
    secondary = Color(0xFF388E3C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF00390A),
    tertiary = Color(0xFF00796B),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F8F5),
    onBackground = Color(0xFF1B1F1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1F1B),
    surfaceVariant = Color(0xFFE7ECE5),
    onSurfaceVariant = Color(0xFF434942),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFF490000),
    outline = Color(0xFF737971),
    outlineVariant = Color(0xFFC3C8C0)
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF66BB6A),
    onPrimary = Color(0xFF003A03),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF00390A),
    secondaryContainer = Color(0xFF2E5B32),
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = Color(0xFF4DB6AC),
    onTertiary = Color(0xFF003731),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEEEEEE),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFEEEEEE),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFC3C8C0),
    error = Color(0xFFEF5350),
    onError = Color(0xFF490000),
    errorContainer = Color(0xFF8C0009),
    onErrorContainer = Color(0xFFFFCDD2),
    outline = Color(0xFF8D938B),
    outlineVariant = Color(0xFF434942)
)

// --- Semantic Color Definitions ---

data class SemanticColor(
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color
)

data class SemanticColors(
    val severityHigh: SemanticColor,
    val severityMedium: SemanticColor,
    val severityLow: SemanticColor,
    val safe: SemanticColor,
    val info: SemanticColor,
    val verifyWarning: SemanticColor
) {
    val reusable: SemanticColor get() = safe
}

val LightSemanticColors = SemanticColors(
    severityHigh = SemanticColor(
        color = Color(0xFFC62828),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFFFEBEE),
        onContainer = Color(0xFFB71C1C)
    ),
    severityMedium = SemanticColor(
        color = Color(0xFFE65100),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFFFF3E0),
        onContainer = Color(0xFFBF360C)
    ),
    severityLow = SemanticColor(
        color = Color(0xFF455A64),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFECEFF1),
        onContainer = Color(0xFF263238)
    ),
    safe = SemanticColor(
        color = Color(0xFF2E7D32),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFE8F5E9),
        onContainer = Color(0xFF1B5E20)
    ),
    info = SemanticColor(
        color = Color(0xFF1565C0),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFE3F2FD),
        onContainer = Color(0xFF0D47A1)
    ),
    verifyWarning = SemanticColor(
        color = Color(0xFFF57F17),
        onColor = Color(0xFF000000),
        container = Color(0xFFFFFDE7),
        onContainer = Color(0xFFE65100)
    )
)

val DarkSemanticColors = SemanticColors(
    severityHigh = SemanticColor(
        color = Color(0xFFEF5350),
        onColor = Color(0xFF490000),
        container = Color(0xFF3B1E1E),
        onContainer = Color(0xFFFFCDD2)
    ),
    severityMedium = SemanticColor(
        color = Color(0xFFFF9800),
        onColor = Color(0xFF3E2723),
        container = Color(0xFF3E271E),
        onContainer = Color(0xFFFFE0B2)
    ),
    severityLow = SemanticColor(
        color = Color(0xFF90A4AE),
        onColor = Color(0xFF102027),
        container = Color(0xFF263238),
        onContainer = Color(0xFFCFD8DC)
    ),
    safe = SemanticColor(
        color = Color(0xFF81C784),
        onColor = Color(0xFF003300),
        container = Color(0xFF1B331B),
        onContainer = Color(0xFFC8E6C9)
    ),
    info = SemanticColor(
        color = Color(0xFF64B5F6),
        onColor = Color(0xFF002171),
        container = Color(0xFF102A45),
        onContainer = Color(0xFFBBDEFB)
    ),
    verifyWarning = SemanticColor(
        color = Color(0xFFFFD54F),
        onColor = Color(0xFF211500),
        container = Color(0xFF332B10),
        onContainer = Color(0xFFFFF59D)
    )
)

val LocalSemanticColors = staticCompositionLocalOf<SemanticColors> {
    LightSemanticColors
}

val MaterialTheme.semantic: SemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSemanticColors.current

// Camera feed overlay scrim defaults (always dark with light content)
object CameraOverlayDefaults {
    val scrim = Color(0xCC000000)
    val scrimHeavy = Color(0xE6000000)
    val cardBackground = Color(0xD91E1E1E)
    val cardBackgroundElevated = Color(0xF02A2A2A)
    val textPrimary = Color(0xFFEEEEEE)
    val textSecondary = Color(0xFFB0BEC5)
    val border = Color(0x40FFFFFF)
}

/**
 * Shared helper for bounding box and detection badge colors based on class and confidence tier.
 */
@Composable
@ReadOnlyComposable
fun severityColorFor(className: String, tier: String): Color {
    val sem = MaterialTheme.semantic
    if (tier == "possible") {
        return sem.severityLow.color
    }
    return when (className) {
        "crt", "battery_swollen", "battery_liion" -> sem.severityHigh.color
        "battery_alkaline", "laptop", "flat_panel_monitor" -> sem.severityMedium.color
        "smartphone" -> sem.safe.color
        else -> sem.severityMedium.color
    }
}

/**
 * SemanticColor for a given Severity level.
 */
@Composable
@ReadOnlyComposable
fun semanticColorForSeverity(severity: Severity): SemanticColor {
    val sem = MaterialTheme.semantic
    return when (severity) {
        Severity.HIGH -> sem.severityHigh
        Severity.MEDIUM -> sem.severityMedium
        Severity.LOW -> sem.severityLow
    }
}
