package com.ewastetriage.app.history

import android.content.Context
import com.ewastetriage.app.triage.TriageResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class HistoryItem(
    val className: String,
    val displayName: String,
    val confidence: Float,
    val tier: String
)

data class HistoryEntry(
    val id: String,
    val timestamp: Long,
    val dateString: String,
    val source: String,
    val items: List<HistoryItem>
)

class HistoryManager(private val context: Context) {
    private val file = File(context.filesDir, "scan_history.json")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    @Synchronized
    fun getHistory(): List<HistoryEntry> {
        if (!file.exists()) return emptyList()
        return try {
            val text = file.readText()
            val jsonArray = JSONArray(text)
            val list = mutableListOf<HistoryEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val itemsArr = obj.getJSONArray("items")
                val items = mutableListOf<HistoryItem>()
                for (j in 0 until itemsArr.length()) {
                    val it = itemsArr.getJSONObject(j)
                    items.add(
                        HistoryItem(
                            className = it.getString("className"),
                            displayName = it.getString("displayName"),
                            confidence = it.getDouble("confidence").toFloat(),
                            tier = it.getString("tier")
                        )
                    )
                }
                list.add(
                    HistoryEntry(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("timestamp"),
                        dateString = obj.getString("dateString"),
                        source = obj.optString("source", "Camera"),
                        items = items
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveScan(results: List<TriageResult>, source: String = "Camera") {
        if (results.isEmpty()) return
        val current = getHistory().toMutableList()
        val now = System.currentTimeMillis()
        val entry = HistoryEntry(
            id = UUID.randomUUID().toString(),
            timestamp = now,
            dateString = dateFormat.format(Date(now)),
            source = source,
            items = results.map {
                HistoryItem(
                    className = it.className,
                    displayName = it.displayName,
                    confidence = it.confidence,
                    tier = it.tier
                )
            }
        )
        current.add(0, entry)
        // Keep up to 50 entries
        val trimmed = if (current.size > 50) current.subList(0, 50) else current
        persist(trimmed)
    }

    @Synchronized
    fun deleteEntry(id: String) {
        val current = getHistory().toMutableList()
        current.removeAll { it.id == id }
        persist(current)
    }

    @Synchronized
    fun restoreEntry(entry: HistoryEntry, index: Int = 0) {
        val current = getHistory().toMutableList()
        val safeIndex = index.coerceIn(0, current.size)
        current.add(safeIndex, entry)
        persist(current)
    }

    @Synchronized
    fun clearHistory() {
        if (file.exists()) file.delete()
    }

    private fun persist(list: List<HistoryEntry>) {
        try {
            val array = JSONArray()
            for (e in list) {
                val obj = JSONObject()
                obj.put("id", e.id)
                obj.put("timestamp", e.timestamp)
                obj.put("dateString", e.dateString)
                obj.put("source", e.source)
                val itemsArr = JSONArray()
                for (it in e.items) {
                    val itObj = JSONObject()
                    itObj.put("className", it.className)
                    itObj.put("displayName", it.displayName)
                    itObj.put("confidence", it.confidence.toDouble())
                    itObj.put("tier", it.tier)
                    itemsArr.put(itObj)
                }
                obj.put("items", itemsArr)
                array.put(obj)
            }
            file.writeText(array.toString(2))
        } catch (_: Exception) {}
    }
}
