package com.ewastetriage.app.guidance

import android.content.Context
import org.json.JSONObject

class GuidanceRepository(context: Context, fileName: String = "guidance_en.json") {
    private val entries: Map<String, GuidanceEntry>

    init {
        val json = JSONObject(context.assets.open(fileName).bufferedReader().readText())
        val map = mutableMapOf<String, GuidanceEntry>()
        json.keys().forEach { key ->
            val obj = json.getJSONObject(key)
            val reusable = mutableListOf<ReusableComponent>()
            val harmful = mutableListOf<HarmfulComponent>()
            val reusableArr = obj.getJSONArray("reusable")
            for (i in 0 until reusableArr.length()) {
                val r = reusableArr.getJSONObject(i)
                reusable.add(ReusableComponent(r.getString("name"), r.getString("note")))
            }
            val harmfulArr = obj.getJSONArray("harmful")
            for (i in 0 until harmfulArr.length()) {
                val h = harmfulArr.getJSONObject(i)
                val severityStr = h.optString("severity", "MEDIUM")
                val severity = try {
                    Severity.valueOf(severityStr.uppercase())
                } catch (_: Exception) {
                    Severity.MEDIUM
                }
                val typeStr = h.optString("type", "UNKNOWN")
                val type = try {
                    HazardType.valueOf(typeStr.uppercase())
                } catch (_: Exception) {
                    HazardType.UNKNOWN
                }
                harmful.add(
                    HarmfulComponent(
                        name = h.getString("name"),
                        risk = h.getString("risk"),
                        handling = h.getString("handling"),
                        severity = severity,
                        type = type
                    )
                )
            }
            map[key] = GuidanceEntry(
                displayName = obj.getString("display_name"),
                danger = obj.getBoolean("danger"),
                composition = obj.getString("composition"),
                verifyHint = obj.optString("verify_hint").takeIf { it.isNotEmpty() },
                alwaysVerify = obj.optBoolean("always_verify", false),
                reusable = reusable,
                harmful = harmful
            )
        }
        entries = map
    }

    fun get(className: String): GuidanceEntry? = entries[className]
    fun displayNames(): Map<String, String> = entries.mapValues { it.value.displayName }
    fun getAllEntries(): Map<String, GuidanceEntry> = entries
}
