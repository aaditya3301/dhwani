package com.dhwani.app.sign

import com.dhwani.app.data.UserContext

data class SignPhrase(
    val gloss: String,
    val english: String,
    val hindi: String,
)

object SignVocabulary {
    val demoPhrases: List<SignPhrase> = listOf(
        SignPhrase("HELLO", "Hello.", "नमस्ते।"),
        SignPhrase("HOW ARE YOU", "How are you?", "आप कैसे हैं?"),
        SignPhrase("THANK YOU", "Thank you.", "धन्यवाद।"),
        SignPhrase("GOOD MORNING", "Good morning.", "सुप्रभात।"),
        SignPhrase("DOCTOR", "I need to speak to the doctor.", "मुझे डॉक्टर से बात करनी है।"),
        SignPhrase("HOSPITAL", "I need to go to the hospital.", "मुझे अस्पताल जाना है।"),
        SignPhrase("MEDICINE", "I am calling about my medicines.", "मैं अपनी दवाइयों के बारे में बात कर रहा हूं।"),
        SignPhrase("TELEPHONE", "Please call me.", "कृपया मुझे फोन कीजिए।"),
        SignPhrase("MONEY", "I am calling about the money.", "मैं पैसों के बारे में बात कर रहा हूं।"),
    )

    fun find(gloss: String): SignPhrase? {
        return demoPhrases.firstOrNull { it.gloss.equals(gloss.trim(), ignoreCase = true) }
    }

    fun fallbackSentence(gloss: String, languageLabel: String, context: UserContext): String {
        val cleanGloss = gloss.trim().replace('_', ' ')
        val normalizedGloss = gloss.trim().uppercase().replace('_', ' ')
        QUICK_GESTURE_SENTENCES[normalizedGloss]?.let { (english, hindi) ->
            return if (languageLabel.equals("Hindi", ignoreCase = true)) hindi else english
        }
        if (normalizedGloss == "YOU(PLURAL)" || normalizedGloss == "YOU (PLURAL)") {
            return if (languageLabel.equals("Hindi", ignoreCase = true)) "आप सब।" else "All of you."
        }
        if (normalizedGloss == "THANKYOU") {
            return if (languageLabel.equals("Hindi", ignoreCase = true)) "धन्यवाद।" else "Thank you."
        }
        if (cleanGloss.equals("MY HOME", ignoreCase = true) && context.voiceFriendlyAddress.isNotBlank()) {
            return if (languageLabel.equals("Hindi", ignoreCase = true)) {
                "मेरा घर ${context.voiceFriendlyAddress} है।"
            } else {
                "My home is ${context.voiceFriendlyAddress}."
            }
        }
        val phrase = find(gloss)
        if (phrase != null) {
            return if (languageLabel.equals("Hindi", ignoreCase = true)) phrase.hindi else phrase.english
        }
        return cleanGloss.lowercase().replaceFirstChar { it.titlecase() }
    }

    private val QUICK_GESTURE_SENTENCES = mapOf(
        "THUMBS UP" to ("Yes." to "हां।"),
        "THUMBS DOWN" to ("No." to "नहीं।"),
        "OPEN PALM" to ("Open palm." to "खुली हथेली।"),
        "CLOSED FIST" to ("Stop." to "रुकिए।"),
        "POINTING UP" to ("One." to "एक।"),
        "VICTORY" to ("Victory." to "जीत।"),
        "I LOVE YOU" to ("I love you." to "मैं आपसे प्यार करता हूं।"),
    )
}

object SignInterpreter {
    fun glossToSentencePrompt(
        context: UserContext,
        conversationContext: String,
        gloss: String,
        targetLanguage: String,
    ): String = """
        You convert Indian Sign Language gloss into one natural phone-call sentence.

        User context:
        ${context.forPrompt()}

        Recent conversation:
        ${conversationContext.ifBlank { "None" }}

        ISL gloss:
        "$gloss"

        Output exactly one sentence for the user to say now.
        Use ${if (targetLanguage.equals("Hindi", ignoreCase = true)) "Hindi in Devanagari script" else "English"}.
        Preserve only the literal meaning of the gloss. Never add an action, answer, fact, or intent from the recent conversation.
        Use recent conversation only to resolve grammar or politeness when the gloss already contains that meaning.
        For a single noun or pronoun gloss, return a short literal phrase. Example: YOU(PLURAL) means "All of you", not "You are free".
        If the gloss asks for saved personal context, use the user context above.
        Do not explain. Do not mention sign language, captions, AI, or Dhwani.
    """.trimIndent()
}
