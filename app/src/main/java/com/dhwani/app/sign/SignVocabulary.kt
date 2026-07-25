package com.dhwani.app.sign

import com.dhwani.app.data.UserContext

data class SignPhrase(
    val gloss: String,
    val english: String,
    val hindi: String,
)

object SignVocabulary {
    val demoPhrases: List<SignPhrase> = listOf(
        SignPhrase("HELLO", "Hello, I am here.", "नमस्ते, मैं लाइन पर हूं।"),
        SignPhrase("YES", "Yes, that works for me.", "हां, यह मेरे लिए ठीक है।"),
        SignPhrase("NO", "No, that will not work.", "नहीं, यह मेरे लिए ठीक नहीं है।"),
        SignPhrase("PLEASE REPEAT", "Please repeat that.", "कृपया फिर से कहिए।"),
        SignPhrase("THANK YOU", "Thank you.", "धन्यवाद।"),
        SignPhrase("BYE", "Bye, thank you for your time.", "अलविदा, आपके समय के लिए धन्यवाद।"),
        SignPhrase("WAIT", "Please wait a moment.", "कृपया एक पल रुकिए।"),
        SignPhrase("APPOINTMENT", "I am calling about my appointment.", "मैं अपनी अपॉइंटमेंट के बारे में बात कर रहा हूं।"),
        SignPhrase("RESCHEDULE", "I need to reschedule my appointment.", "मुझे अपनी अपॉइंटमेंट बदलनी है।"),
        SignPhrase("DOCTOR", "I need to speak to the doctor.", "मुझे डॉक्टर से बात करनी है।"),
        SignPhrase("ADDRESS", "I can share my address.", "मैं अपना पता बता सकता हूं।"),
        SignPhrase("MY HOME", "My home address is saved in Dhwani.", "मेरा घर का पता ध्वनि में सेव है।"),
        SignPhrase("PAYMENT", "I can share my UPI ID.", "मैं अपना UPI ID बता सकता हूं।"),
        SignPhrase("MEDICINE", "I am calling about my medicines.", "मैं अपनी दवाइयों के बारे में बात कर रहा हूं।"),
        SignPhrase("CONFIRM", "Please confirm this.", "कृपया इसकी पुष्टि कर दीजिए।"),
        SignPhrase("CALL BACK", "I will call back soon.", "मैं थोड़ी देर में वापस कॉल करूंगा।"),
    )

    fun find(gloss: String): SignPhrase? {
        return demoPhrases.firstOrNull { it.gloss.equals(gloss.trim(), ignoreCase = true) }
    }

    fun fallbackSentence(gloss: String, languageLabel: String, context: UserContext): String {
        val cleanGloss = gloss.trim().replace('_', ' ')
        val normalizedGloss = gloss.trim().uppercase().replace('_', ' ')
        if (normalizedGloss == "YOU(PLURAL)") {
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
