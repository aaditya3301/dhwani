package com.dhwani.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CallSummary(
    val id: Long,
    val timestamp: Long,
    val summary: String,
)

class CallLogStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadRecent(limit: Int = 5): List<CallSummary> {
        val raw = prefs.getString(KEY_SUMMARIES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        CallSummary(
                            id = item.optLong("id"),
                            timestamp = item.optLong("timestamp"),
                            summary = item.optString("summary"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList()).take(limit)
    }

    fun add(summary: String) {
        val cleanSummary = summary.trim()
        if (cleanSummary.isBlank()) return

        val updated = listOf(
            CallSummary(
                id = System.currentTimeMillis(),
                timestamp = System.currentTimeMillis(),
                summary = cleanSummary,
            ),
        ) + loadRecent(MAX_ITEMS - 1)

        val array = JSONArray()
        updated.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("timestamp", item.timestamp)
                    .put("summary", item.summary),
            )
        }
        prefs.edit().putString(KEY_SUMMARIES, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "dhwani_call_log"
        private const val KEY_SUMMARIES = "summaries"
        private const val MAX_ITEMS = 10
    }
}
