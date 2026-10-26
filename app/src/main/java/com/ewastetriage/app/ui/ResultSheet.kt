package com.ewastetriage.app.ui

import androidx.compose.runtime.Composable
import com.ewastetriage.app.guidance.GuidanceEntry
import com.ewastetriage.app.triage.TriageResult
import com.ewastetriage.app.ui.components.GuidanceDetail

@Composable
fun ResultSheet(
    result: TriageResult? = null,
    guidance: GuidanceEntry?,
    categoryKey: String? = result?.className,
    onDismiss: () -> Unit
) {
    GuidanceDetail(
        result = result,
        guidance = guidance,
        categoryKey = categoryKey,
        onDismiss = onDismiss
    )
}
