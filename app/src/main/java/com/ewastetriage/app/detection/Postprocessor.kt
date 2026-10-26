package com.ewastetriage.app.detection

/**
 * Decodes the raw YOLOv8 output tensor of shape (1, 10, 8400) and applies class-agnostic NMS.
 *
 * Output layout per anchor (dim 1 = 10):
 *   indices 0-3: cx, cy, w, h  (values in 0-640 range based on model export)
 *   indices 4-9: class scores (6 classes)
 *
 * The tensor is stored as a flat FloatArray in row-major order:
 *   output[0][feature][anchor] => flat index = feature * 8400 + anchor
 */
class Postprocessor(
    private val classNames: List<String>,
    private val confFloor: Float = 0.20f,
    private val iouThreshold: Float = 0.45f
) {
    private val numAnchors = 8400
    private val numClasses get() = classNames.size

    fun process(rawOutput: FloatArray): List<Detection> {
        val candidates = mutableListOf<Detection>()

        for (a in 0 until numAnchors) {
            // Box coords (cx, cy, w, h) in 0-640 space
            val cx = rawOutput[0 * numAnchors + a]
            val cy = rawOutput[1 * numAnchors + a]
            val w  = rawOutput[2 * numAnchors + a]
            val h  = rawOutput[3 * numAnchors + a]

            // Class scores
            var bestScore = -1f
            var bestClass = -1
            for (c in 0 until numClasses) {
                val score = rawOutput[(4 + c) * numAnchors + a]
                if (score > bestScore) {
                    bestScore = score
                    bestClass = c
                }
            }

            if (bestScore < confFloor || bestClass < 0) continue

            // Convert cx,cy,w,h -> x1,y1,x2,y2 normalized to 0-1
            // Handle both pixel space (0-640) and normalized space (0-1)
            val factor = if (cx > 2f || w > 2f) 640f else 1f
            val x1 = (cx - w / 2f) / factor
            val y1 = (cy - h / 2f) / factor
            val x2 = (cx + w / 2f) / factor
            val y2 = (cy + h / 2f) / factor

            if (x2 <= x1 || y2 <= y1) continue

            candidates.add(
                Detection(bestClass, classNames[bestClass], bestScore, x1.coerceIn(0f,1f), y1.coerceIn(0f,1f), x2.coerceIn(0f,1f), y2.coerceIn(0f,1f))
            )
        }

        return nms(candidates)
    }

    private fun iou(a: Detection, b: Detection): Float {
        val ix1 = maxOf(a.x1, b.x1)
        val iy1 = maxOf(a.y1, b.y1)
        val ix2 = minOf(a.x2, b.x2)
        val iy2 = minOf(a.y2, b.y2)
        val inter = maxOf(0f, ix2 - ix1) * maxOf(0f, iy2 - iy1)
        val areaA = (a.x2 - a.x1) * (a.y2 - a.y1)
        val areaB = (b.x2 - b.x1) * (b.y2 - b.y1)
        val union = areaA + areaB - inter
        return if (union <= 0f) 0f else inter / union
    }

    // Class-agnostic NMS (matching desktop agnostic_nms=True)
    private fun nms(dets: List<Detection>): List<Detection> {
        val sorted = dets.sortedByDescending { it.confidence }
        val kept = mutableListOf<Detection>()
        val suppressed = BooleanArray(sorted.size)
        for (i in sorted.indices) {
            if (suppressed[i]) continue
            kept.add(sorted[i])
            for (j in i + 1 until sorted.size) {
                if (!suppressed[j] && iou(sorted[i], sorted[j]) > iouThreshold) {
                    suppressed[j] = true
                }
            }
        }
        return kept
    }
}
