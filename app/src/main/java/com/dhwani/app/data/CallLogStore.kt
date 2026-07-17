package com.dhwani.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CallSummary(
    val id: String,
    val timestamp: Long,
    val direction: String,
    val phoneNumber: String,
    val contactName: String,
    val durationSec: Int,
    val transcript: String,
    val summary: String,
    val outcomes: List<String>,
)

interface RecentCallSummarySource {
    fun loadRecent(limit: Int = 5): List<CallSummary>
}

class CallLogStore(context: Context) : RecentCallSummarySource {
    private val secureStore = SecureJsonStore(
        context = context.applicationContext,
        prefsName = PREFS_NAME,
        keyAlias = KEY_ALIAS,
    )

    override fun loadRecent(limit: Int): List<CallSummary> {
        val array = secureStore.readArray(KEY_SUMMARIES) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { add(it.toCallSummary()) }
            }
        }.sortedByDescending { it.timestamp }.take(limit)
    }

    fun add(
        summary: String,
        transcript: String = "",
        direction: String = "manual",
        phoneNumber: String = "",
        contactName: String = "",
        durationSec: Int = 0,
    ) {
        val cleanSummary = summary.trim()
        if (cleanSummary.isBlank()) return

        val updated = listOf(
            CallSummary(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                direction = direction,
                phoneNumber = phoneNumber,
                contactName = contactName,
                durationSec = durationSec,
                transcript = transcript.trim(),
                summary = cleanSummary,
                outcomes = extractOutcomes(cleanSummary),
            ),
        ) + loadRecent(MAX_ITEMS - 1)

        secureStore.writeArray(
            KEY_SUMMARIES,
            JSONArray().apply { updated.forEach { put(it.toJson()) } },
        )
    }

    fun clear() {
        secureStore.remove(KEY_SUMMARIES)
    }

    companion object {
        private const val PREFS_NAME = "dhwani_call_log"
        private const val KEY_SUMMARIES = "summaries"
        private const val KEY_ALIAS = "dhwani_call_log_key"
        private const val MAX_ITEMS = 25
    }
}

private fun CallSummary.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("timestamp", timestamp)
    .put("direction", direction)
    .put("phoneNumber", phoneNumber)
    .put("contactName", contactName)
    .put("durationSec", durationSec)
    .put("transcript", transcript)
    .put("summary", summary)
    .put("outcomes", JSONArray(outcomes))

private fun JSONObject.toCallSummary(): CallSummary {
    return CallSummary(
        id = optString("id").ifBlank { UUID.randomUUID().toString() },
        timestamp = optLong("timestamp"),
        direction = optString("direction", "manual"),
        phoneNumber = optString("phoneNumber"),
        contactName = optString("contactName"),
        durationSec = optInt("durationSec"),
        transcript = optString("transcript"),
        summary = optString("summary"),
        outcomes = optJSONArray("outcomes")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }.orEmpty(),
    )
}

private fun extractOutcomes(summary: String): List<String> {
    return summary
        .lineSequence()
        .map { it.trim().trimStart('-', '*') }
        .filter { line ->
            line.contains("confirm", ignoreCase = true) ||
                line.contains("scheduled", ignoreCase = true) ||
                line.contains("rescheduled", ignoreCase = true) ||
                line.contains("appointment", ignoreCase = true) ||
                line.contains("follow", ignoreCase = true)
        }
        .take(3)
        .toList()
}
