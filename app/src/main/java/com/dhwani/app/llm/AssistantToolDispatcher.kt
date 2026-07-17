package com.dhwani.app.llm

import com.dhwani.app.data.CallSummary
import com.dhwani.app.data.RecentCallSummarySource
import com.dhwani.app.data.UserContext

data class AssistantToolCall(
    val name: String,
    val args: Map<String, String>,
)

data class AssistantToolResult(
    val name: String,
    val result: String,
)

class AssistantToolDispatcher(
    private val context: UserContext,
    private val recentCalls: RecentCallSummarySource,
) {
    fun availableContext(): String = buildList {
        add("addresses=${context.addresses.size}")
        add("contacts=${context.contacts.size}")
        add("medical=${if (context.medicalNotes.isBlank()) "not configured" else "configured"}")
        add("payment_hints=${context.paymentHints.size}")
        add("recent_call_summaries=${recentCalls.loadRecent(3).size}")
    }.joinToString("; ")

    fun dispatch(call: AssistantToolCall): AssistantToolResult {
        val result = when (call.name) {
            "get_address" -> getAddress(call.args["label"])
            "get_contact_info" -> getContactInfo(call.args["name_or_role"])
            "get_medical_info" -> getMedicalInfo(call.args["field"])
            "get_payment_hint" -> getPaymentHint(call.args["label"])
            "get_recent_call_summary" -> getRecentCallSummary(call.args["contact_name"])
            else -> "Unknown tool: ${call.name}"
        }
        return AssistantToolResult(name = call.name, result = result)
    }

    private fun getAddress(label: String?): String {
        val query = label.orEmpty().ifBlank { "home" }
        val address = context.addresses.firstOrNull { it.label.equals(query, ignoreCase = true) }
            ?: context.addresses.firstOrNull()
        return address?.let {
            buildString {
                append(it.label)
                append(": ")
                append(it.voiceFriendly.ifBlank { it.fullAddress })
                if (it.landmark.isNotBlank()) append(" Landmark: ${it.landmark}")
            }
        } ?: "No address saved."
    }

    private fun getContactInfo(query: String?): String {
        val normalized = query.orEmpty().trim()
        val contact = context.contacts.firstOrNull { contact ->
            normalized.isBlank() ||
                contact.name.contains(normalized, ignoreCase = true) ||
                contact.role.contains(normalized, ignoreCase = true) ||
                contact.notes.contains(normalized, ignoreCase = true)
        } ?: context.contacts.firstOrNull()

        return contact?.let {
            buildString {
                append("${it.name} (${it.role})")
                if (it.phoneNumbers.isNotEmpty()) append(" ${it.phoneNumbers.joinToString(", ")}")
                if (it.notes.isNotBlank()) append(" - ${it.notes}")
                if (it.lastCallSummary.isNotBlank()) append(" Last call: ${it.lastCallSummary}")
            }
        } ?: "No matching contact saved."
    }

    private fun getMedicalInfo(field: String?): String {
        return when (field.orEmpty().lowercase()) {
            "medications", "medicine", "meds" -> context.medical.medications.joinToString(", ")
            "allergies", "allergy" -> context.medical.allergies.joinToString(", ")
            "conditions", "condition" -> context.medical.conditions.joinToString(", ")
            "emergency", "emergency_contact" -> context.medical.emergencyContact
            else -> context.medicalNotes
        }.ifBlank { "No medical information saved for that field." }
    }

    private fun getPaymentHint(label: String?): String {
        val query = label.orEmpty().ifBlank { "default" }
        val hint = context.paymentHints.firstOrNull {
            it.label.equals(query, ignoreCase = true) ||
                it.label.contains(query, ignoreCase = true)
        } ?: context.paymentHints.firstOrNull()
        return hint?.let { "${it.label}: ${it.safeToShare}" } ?: "No safe payment hint saved."
    }

    private fun getRecentCallSummary(contactName: String?): String {
        val query = contactName.orEmpty().trim()
        val summaries = recentCalls.loadRecent(5)
            .filter { summary ->
                query.isBlank() ||
                    summary.contactName.contains(query, ignoreCase = true) ||
                    summary.summary.contains(query, ignoreCase = true)
            }
            .ifEmpty { recentCalls.loadRecent(3) }
        return summaries.joinToString("\n", transform = CallSummary::summary).ifBlank {
            "No recent call summaries saved."
        }
    }

    companion object {
        fun extractToolCalls(text: String): List<AssistantToolCall> {
            return extractToolPayloads(text).mapNotNull { payload ->
                parseToolPayload(payload)
            }.filter { it.name.isNotBlank() }.toList()
        }

        fun removeToolBlocks(text: String): String {
            var output = text
            while (true) {
                val start = output.indexOf("<tool_call", ignoreCase = true)
                if (start < 0) return output.trim()
                val openEnd = output.indexOf(">", start)
                val closeStart = output.indexOf("</tool_call>", start, ignoreCase = true)
                if (openEnd < 0 || closeStart < 0) return output.trim()
                val closeEnd = closeStart + "</tool_call>".length
                output = output.removeRange(start, closeEnd)
            }
        }

        fun formatToolResults(results: List<AssistantToolResult>): String {
            return results.joinToString("\n") { result ->
                "<tool_result name=\"${result.name}\">${escapeToolResult(result.result)}</tool_result>"
            }
        }

        private fun escapeToolResult(value: String): String {
            return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
        }

        private fun extractToolPayloads(text: String): List<String> {
            val payloads = mutableListOf<String>()
            var searchFrom = 0
            while (searchFrom < text.length) {
                val start = text.indexOf("<tool_call", startIndex = searchFrom, ignoreCase = true)
                if (start < 0) break
                val openEnd = text.indexOf(">", start)
                val closeStart = text.indexOf("</tool_call>", startIndex = start, ignoreCase = true)
                if (openEnd < 0 || closeStart < 0 || closeStart <= openEnd) break
                payloads += text.substring(openEnd + 1, closeStart)
                searchFrom = closeStart + "</tool_call>".length
            }
            return payloads
        }

        private fun parseToolPayload(payload: String): AssistantToolCall? {
            val name = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"")
                .find(payload)
                ?.groupValues
                ?.getOrNull(1)
                .orEmpty()
            if (name.isBlank()) return null

            val argsPayload = Regex("\"args\"\\s*:\\s*\\{(.*?)\\}", RegexOption.DOT_MATCHES_ALL)
                .find(payload)
                ?.groupValues
                ?.getOrNull(1)
                .orEmpty()
            val args = Regex("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"")
                .findAll(argsPayload)
                .associate { match ->
                    match.groupValues[1] to match.groupValues[2]
                }
            return AssistantToolCall(name = name, args = args)
        }
    }
}
