package com.dhwani.app.llm

import com.dhwani.app.data.UserContext

object Phase2Assistant {
    fun smartReplyPrompt(
        context: UserContext,
        rollingTranscript: String,
        latestUtterance: String,
        recentSummaries: String,
        toolContext: String,
        toolResults: String = "",
    ): String = """
        You write tap-to-speak replies for a deaf user in a live phone call.

        User:
        ${compactContext(context)}

        Local tools available:
        - get_address(label)
        - get_contact_info(name_or_role)
        - get_medical_info(field)
        - get_payment_hint(label)
        - get_recent_call_summary(contact_name)

        Current local context:
        ${toolContext.ifBlank { "None" }}

        Tool results:
        ${toolResults.ifBlank { "None" }}

        Recent saved call notes:
        ${recentSummaries.ifBlank { "None" }}

        Recent call:
        ${rollingTranscript.ifBlank { "None" }}

        Caller just said:
        "$latestUtterance"

        Write exactly 3 replies the user can say now.
        Rules:
        - Reply directly to the caller's latest message.
        - Keep each reply under 10 words.
        - Sound natural, not robotic.
        - Do not explain.
        - Do not mention captions, AI, or deafness.
        - If the caller asks a question, answer or ask for clarification.
        - If a local fact is needed and not present in Tool results, output one <tool_call> block only.
        - If Hindi is used, write Hindi in Devanagari.

        Tool call format:
        <tool_call>{"name":"get_address","args":{"label":"home"}}</tool_call>

        Otherwise output only:
        1. ...
        2. ...
        3. ...
    """.trimIndent()

    fun briefingPrompt(
        context: UserContext,
        goal: String,
        recentSummaries: String,
        toolContext: String,
        toolResults: String = "",
    ): String = """
        Prepare a short outgoing-call briefing.

        User:
        ${compactContext(context)}

        Local tools available:
        - get_address(label)
        - get_contact_info(name_or_role)
        - get_medical_info(field)
        - get_payment_hint(label)
        - get_recent_call_summary(contact_name)

        Current local context:
        ${toolContext.ifBlank { "None" }}

        Tool results:
        ${toolResults.ifBlank { "None" }}

        Recent saved call notes:
        ${recentSummaries.ifBlank { "None" }}

        Goal:
        "$goal"

        If a local fact is needed and not present in Tool results, output one <tool_call> block only:
        <tool_call>{"name":"get_contact_info","args":{"name_or_role":"doctor"}}</tool_call>

        Return only this format, concise:
        Opening: one sentence.
        Expect: 3 likely caller questions.
        Say: 3 short responses.
        Ready: key details to keep nearby.
    """.trimIndent()

    fun summaryPrompt(transcript: String): String = """
        Summarize this phone call transcript in 2 short lines.
        Include concrete outcomes, times, names, or follow-ups if present.
        If the transcript is unclear, say what was discussed.

        Transcript:
        $transcript
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

    private fun compactContext(context: UserContext): String {
        return buildList {
            add("Name=${context.name.ifBlank { "unknown" }}")
            add("Language=${context.preferredLanguage}")
            if (context.voiceFriendlyAddress.isNotBlank()) {
                add("Address=${context.voiceFriendlyAddress}")
            }
            if (context.importantPeople.isNotBlank()) {
                add("People=${context.importantPeople}")
            }
            if (context.medicalNotes.isNotBlank()) {
                add("Medical=${context.medicalNotes}")
            }
            if (context.paymentHint.isNotBlank()) {
                add("Payment=${context.paymentHint}")
            }
        }.joinToString("; ")
    }
}
