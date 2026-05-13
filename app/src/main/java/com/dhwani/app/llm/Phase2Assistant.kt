package com.dhwani.app.llm

import com.dhwani.app.data.UserContext

object Phase2Assistant {
    fun smartReplyPrompt(
        context: UserContext,
        rollingTranscript: String,
        latestUtterance: String,
    ): String = """
        You are Dhwani, a phone-call assistant for a deaf user.

        Personal context:
        ${context.forPrompt()}

        Conversation so far:
        ${rollingTranscript.ifBlank { "No prior transcript." }}

        Latest caller utterance:
        "$latestUtterance"

        Generate exactly 3 short natural replies the user could say.
        Each reply must be 12 words or fewer.
        Match the caller's language when obvious. Use Devanagari for Hindi.

        Output only:
        1. first reply
        2. second reply
        3. third reply
    """.trimIndent()

    fun briefingPrompt(context: UserContext, goal: String): String = """
        You are Dhwani, preparing a deaf user for an outgoing phone call.

        Personal context:
        ${context.forPrompt()}

        Call goal:
        "$goal"

        Generate a concise call briefing with:
        Opening line:
        Likely questions:
        Recommended responses:
        Information to keep ready:

        Keep it practical and short.
    """.trimIndent()

    fun parseSuggestions(output: String): List<String> {
        val numbered = output
            .lineSequence()
            .map { it.trim() }
            .map { it.replace(Regex("^[-*]\\s*"), "") }
            .map { it.replace(Regex("^\\d+[.)]\\s*"), "") }
            .filter { it.isNotBlank() }
            .filterNot { it.contains("suggestion", ignoreCase = true) }
            .take(3)
            .toList()

        return if (numbered.size >= 3) numbered else fallbackSuggestions()
    }

    fun fallbackSuggestions(): List<String> = listOf(
        "Yes, please continue.",
        "Can you repeat that?",
        "I will get back to you soon.",
    )
}
