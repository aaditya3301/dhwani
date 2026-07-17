package com.dhwani.app.llm

import com.dhwani.app.data.Address
import com.dhwani.app.data.CallSummary
import com.dhwani.app.data.Contact
import com.dhwani.app.data.MedicalContext
import com.dhwani.app.data.PaymentHint
import com.dhwani.app.data.RecentCallSummarySource
import com.dhwani.app.data.UserContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantToolDispatcherTest {
    @Test
    fun parsesGemmaToolCallBlocks() {
        val calls = AssistantToolDispatcher.extractToolCalls(
            """
            <tool_call>{"name":"get_address","args":{"label":"home"}}</tool_call>
            """.trimIndent(),
        )

        assertEquals(1, calls.size)
        assertEquals("get_address", calls.single().name)
        assertEquals("home", calls.single().args["label"])
    }

    @Test
    fun dispatchesStructuredContext() {
        val dispatcher = AssistantToolDispatcher(
            context = UserContext(
                name = "Ravi",
                addresses = listOf(Address(label = "home", fullAddress = "MG Road", voiceFriendly = "near MG Road")),
                contacts = listOf(Contact(name = "Dr. Mehta", role = "Doctor", phoneNumbers = listOf("+911234567890"))),
                medical = MedicalContext(medications = listOf("Telmisartan 40mg")),
                paymentHints = listOf(PaymentHint(label = "Default UPI", safeToShare = "ravi@upi")),
            ),
            recentCalls = FakeRecentCalls,
        )

        assertTrue(dispatcher.dispatch(AssistantToolCall("get_address", mapOf("label" to "home"))).result.contains("MG Road"))
        assertTrue(dispatcher.dispatch(AssistantToolCall("get_contact_info", mapOf("name_or_role" to "doctor"))).result.contains("Dr. Mehta"))
        assertTrue(dispatcher.dispatch(AssistantToolCall("get_medical_info", mapOf("field" to "medications"))).result.contains("Telmisartan"))
        assertTrue(dispatcher.dispatch(AssistantToolCall("get_payment_hint", mapOf("label" to "upi"))).result.contains("ravi@upi"))
        assertTrue(dispatcher.dispatch(AssistantToolCall("get_recent_call_summary", emptyMap())).result.contains("appointment"))
    }

    @Test
    fun parsesSuggestionsWithoutToolMarkup() {
        val suggestions = CallAssistant.parseSuggestions(
            """
            <suggestions>
            1. Thursday 3pm works.
            2. Please confirm it.
            3. Thank you.
            </suggestions>
            """.trimIndent(),
        )

        assertEquals(listOf("Thursday 3pm works.", "Please confirm it.", "Thank you."), suggestions)
    }

    private object FakeRecentCalls : RecentCallSummarySource {
        override fun loadRecent(limit: Int): List<CallSummary> {
            return listOf(
                CallSummary(
                    id = "1",
                    timestamp = 1L,
                    direction = "manual",
                    phoneNumber = "",
                    contactName = "Dr. Mehta",
                    durationSec = 60,
                    transcript = "",
                    summary = "Discussed appointment timing.",
                    outcomes = listOf("appointment timing"),
                ),
            ).take(limit)
        }
    }
}
