package com.ewastetriage.app.guidance

enum class Severity(val label: String) {
    HIGH("High risk"),
    MEDIUM("Moderate"),
    LOW("Lower risk")
}

enum class HazardType(val label: String) {
    FIRE("Fire"),
    TOXIC("Toxic"),
    ELECTRICAL("Electrical"),
    GLASS("Glass"),
    CORROSIVE("Corrosive"),
    UNKNOWN("Hazard")
}

data class HarmfulComponent(
    val name: String,
    val risk: String,
    val handling: String,
    val severity: Severity = Severity.MEDIUM,
    val type: HazardType = HazardType.UNKNOWN
)

data class ReusableComponent(
    val name: String,
    val note: String
)

data class GuidanceEntry(
    val displayName: String,
    val danger: Boolean,
    val composition: String,
    val verifyHint: String?,
    val alwaysVerify: Boolean,
    val reusable: List<ReusableComponent>,
    val harmful: List<HarmfulComponent>
) {
    /**
     * Overall severity derived dynamically from the most severe harmful component.
     */
    val overallSeverity: Severity
        get() {
            if (harmful.any { it.severity == Severity.HIGH }) return Severity.HIGH
            if (harmful.any { it.severity == Severity.MEDIUM }) return Severity.MEDIUM
            if (harmful.any { it.severity == Severity.LOW }) return Severity.LOW
            return if (danger) Severity.HIGH else Severity.LOW
        }
}
