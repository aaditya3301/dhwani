package com.dhwani.app.llm

import com.dhwani.app.data.CallLogStore
import com.dhwani.app.data.UserContext
import org.json.JSONObject

data class Phase2ToolCall(
    val name: String,
    val args: Map<String, String>,
)

data class Phase2ToolResult(
    val name: String,
    val result: String,
)

class Phase2ToolDispatcher(
    private val context: UserContext,
    private val callLogStore: CallLogStore,
) {
    fun availableContext(): String = buildList {
        add("get_address(label): ${context.voiceFriendlyAddress.ifBlank { context.homeAddress }.ifBlank { "not configured" }}")
        add("get_contact_info(name_or_role): ${context.importantPeople.ifBlank { "not configured" }}")
        add("get_medical_info(field): ${context.medicalNotes.ifBlank { "not configured" }}")
        add("get_payment_hint(label): ${context.paymentHint.ifBlank { "not configured" }}")
        add(
            "get_recent_call_summary(contact_name): " + callLogStore.loadRecent(3)
                .joinToString(" | ") { it.summary }
                .ifBlank { "none" },
        )
    }.joinToString("\n")

    fun dispatch(call: Phase2ToolCall): Phase2ToolResult {
        val result = when (call.name) {
            "get_address" -> context.voiceFriendlyAddress.ifBlank { context.homeAddress }.ifBlank { "No address saved." }
            "get_contact_info" -> context.importantPeople.ifBlank { "No contacts or people saved." }
            "get_medical_info" -> context.medicalNotes.ifBlank { "No medical notes saved." }
            "get_payment_hint" -> context.paymentHint.ifBlank { "No safe payment hint saved." }
            "get_recent_call_summary" -> callLogStore.loadRecent(3)
                .joinToString("\n") { it.summary }
                .ifBlank { "No recent call summaries saved." }
            else -> "Unknown tool: ${call.name}"
        }
        return Phase2ToolResult(name = call.name, result = result)
    }

    companion object {
        private val TOOL_CALL_RE = Regex("<tool_call>\\s*([\\s\\S]+?)\\s*</tool_call>")

        fun extractToolCalls(text: String): List<Phase2ToolCall> {
            return TOOL_CALL_RE.findAll(text).mapNotNull { match ->
                runCatching {
                    val json = JSONObject(match.groupValues[1])
                    val argsJson = json.optJSONObject("args") ?: JSONObject()
                    val args = buildMap {
                        argsJson.keys().forEach { key ->
                            put(key, argsJson.optString(key))
                        }
                    }
                    Phase2ToolCall(
                        name = json.optString("name"),
                        args = args,
                    )
                }.getOrNull()
            }.filter { it.name.isNotBlank() }.toList()
        }

        fun formatToolResults(results: List<Phase2ToolResult>): String {
            return results.joinToString("\n") { result ->
                "<tool_result name=\"${result.name}\">${result.result}</tool_result>"
            }
        }
    }
}
