package com.ewastetriage.app.triage

import com.ewastetriage.app.detection.Detection

data class TriageResult(
    val className: String,
    val displayName: String,
    val confidence: Float,
    val tier: String,   // "possible" or "confident"
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
)

class TriageEngine(
    private val config: TriageConfig,
    private val smoother: ClassSmoother = ClassSmoother(config.smoothWindow, config.smoothNeeded),
    private val displayNames: Map<String, String> = emptyMap()
) {
    /**
     * Live video stream triage with 3-of-5 frame smoothing.
     */
    fun process(detections: List<Detection>, offset: Float = 0f): List<TriageResult> {
        // Step 1: filter by per-class minimum confidence, keep best per class
        val frameDets = mutableListOf<Detection>()
        val bestByClass = mutableMapOf<String, Float>()
        for (det in detections) {
            if (config.tier(det.className, det.confidence, offset) == null) continue
            frameDets.add(det)
            bestByClass[det.className] = maxOf(det.confidence, bestByClass.getOrDefault(det.className, 0f))
        }

        // Step 2: smooth over recent frames
        smoother.update(bestByClass)
        val confirmed = smoother.confirmed()

        // Step 3: map detections to triage results
        val results = mutableListOf<TriageResult>()
        for (det in frameDets) {
            val smoothedConf = confirmed[det.className] ?: continue
            val tier = config.tier(det.className, smoothedConf, offset) ?: continue
            results.add(
                TriageResult(
                    className = det.className,
                    displayName = displayNames.getOrDefault(det.className, det.className),
                    confidence = smoothedConf,
                    tier = tier,
                    x1 = det.x1, y1 = det.y1, x2 = det.x2, y2 = det.y2
                )
            )
        }
        return results
    }

    /**
     * Single still image triage (e.g. Gallery photo testing).
     * Bypasses temporal smoothing and evaluates per-class tier directly.
     */
    fun processSingle(detections: List<Detection>, offset: Float = 0f): List<TriageResult> {
        val results = mutableListOf<TriageResult>()
        for (det in detections) {
            val tier = config.tier(det.className, det.confidence, offset) ?: continue
            results.add(
                TriageResult(
                    className = det.className,
                    displayName = displayNames.getOrDefault(det.className, det.className),
                    confidence = det.confidence,
                    tier = tier,
                    x1 = det.x1, y1 = det.y1, x2 = det.x2, y2 = det.y2
                )
            )
        }
        return results
    }

    fun reset() = smoother.reset()
}
