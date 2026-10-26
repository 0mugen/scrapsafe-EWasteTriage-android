package com.ewastetriage.app.triage

import android.content.Context
import org.json.JSONObject

data class TriageConfig(
    val detectorFloor: Float,
    val defaultMin: Float,
    val defaultConfident: Float,
    val classMin: Map<String, Float>,
    val classConfident: Map<String, Float>,
    val smoothWindow: Int,
    val smoothNeeded: Int
) {
    fun minConf(name: String, offset: Float = 0f): Float {
        val base = classMin.getOrDefault(name, defaultMin) + offset
        return base.coerceIn(detectorFloor, 0.95f)
    }

    fun confidentConf(name: String, offset: Float = 0f): Float {
        val base = classConfident.getOrDefault(name, defaultConfident) + offset
        return maxOf(minConf(name, offset), base.coerceIn(detectorFloor, 0.95f))
    }

    fun tier(name: String, conf: Float, offset: Float = 0f): String? {
        if (conf < minConf(name, offset)) return null
        return if (conf >= confidentConf(name, offset)) "confident" else "possible"
    }

    companion object {
        fun load(context: Context, fileName: String = "thresholds.json"): TriageConfig {
            val json = JSONObject(context.assets.open(fileName).bufferedReader().readText())
            val classMinMap = mutableMapOf<String, Float>()
            val classConfMap = mutableMapOf<String, Float>()
            json.optJSONObject("class_min")?.let { obj ->
                obj.keys().forEach { key -> classMinMap[key] = obj.getDouble(key).toFloat() }
            }
            json.optJSONObject("class_confident")?.let { obj ->
                obj.keys().forEach { key -> classConfMap[key] = obj.getDouble(key).toFloat() }
            }
            return TriageConfig(
                detectorFloor = json.getDouble("detector_floor").toFloat(),
                defaultMin = json.getDouble("default_min").toFloat(),
                defaultConfident = json.getDouble("default_confident").toFloat(),
                classMin = classMinMap,
                classConfident = classConfMap,
                smoothWindow = json.getInt("smooth_window"),
                smoothNeeded = json.getInt("smooth_needed")
            )
        }
    }
}
