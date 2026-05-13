package com.dhwani.app.data

import android.content.Context
import org.json.JSONObject

data class UserContext(
    val name: String = "",
    val preferredLanguage: String = "English",
    val homeAddress: String = "",
    val voiceFriendlyAddress: String = "",
    val importantPeople: String = "",
    val medicalNotes: String = "",
    val paymentHint: String = "",
) {
    val isConfigured: Boolean
        get() = name.isNotBlank()

    fun forPrompt(): String = buildString {
        appendLine("User name: ${name.ifBlank { "Unknown" }}")
        appendLine("Preferred language: $preferredLanguage")
        if (homeAddress.isNotBlank()) appendLine("Home address: $homeAddress")
        if (voiceFriendlyAddress.isNotBlank()) appendLine("Voice-friendly address: $voiceFriendlyAddress")
        if (importantPeople.isNotBlank()) appendLine("Important people and contacts: $importantPeople")
        if (medicalNotes.isNotBlank()) appendLine("Medical notes: $medicalNotes")
        if (paymentHint.isNotBlank()) appendLine("Safe payment hint: $paymentHint")
    }
}

class UserContextStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): UserContext {
        val raw = prefs.getString(KEY_CONTEXT, null) ?: return UserContext()
        return runCatching {
            val json = JSONObject(raw)
            UserContext(
                name = json.optString("name"),
                preferredLanguage = json.optString("preferredLanguage", "English"),
                homeAddress = json.optString("homeAddress"),
                voiceFriendlyAddress = json.optString("voiceFriendlyAddress"),
                importantPeople = json.optString("importantPeople"),
                medicalNotes = json.optString("medicalNotes"),
                paymentHint = json.optString("paymentHint"),
            )
        }.getOrDefault(UserContext())
    }

    fun save(userContext: UserContext) {
        val json = JSONObject()
            .put("name", userContext.name)
            .put("preferredLanguage", userContext.preferredLanguage)
            .put("homeAddress", userContext.homeAddress)
            .put("voiceFriendlyAddress", userContext.voiceFriendlyAddress)
            .put("importantPeople", userContext.importantPeople)
            .put("medicalNotes", userContext.medicalNotes)
            .put("paymentHint", userContext.paymentHint)
        prefs.edit().putString(KEY_CONTEXT, json.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "dhwani_context"
        private const val KEY_CONTEXT = "user_context"
    }
}
