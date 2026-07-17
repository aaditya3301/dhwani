package com.dhwani.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: String,
    val phoneNumbers: List<String> = emptyList(),
    val notes: String = "",
    val lastCallSummary: String = "",
)

data class Address(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val fullAddress: String,
    val landmark: String = "",
    val voiceFriendly: String = "",
)

data class MedicalContext(
    val id: String = "medical-default",
    val medications: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val conditions: List<String> = emptyList(),
    val emergencyContact: String = "",
)

data class PaymentHint(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val safeToShare: String,
)

data class UserContext(
    val name: String = "",
    val preferredLanguage: String = "English",
    val addresses: List<Address> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val medical: MedicalContext = MedicalContext(),
    val paymentHints: List<PaymentHint> = emptyList(),
) {
    val isConfigured: Boolean
        get() = name.isNotBlank()

    val homeAddress: String
        get() = homeAddressRecord?.fullAddress.orEmpty()

    val voiceFriendlyAddress: String
        get() = homeAddressRecord?.voiceFriendly.orEmpty()

    val importantPeople: String
        get() = contacts.joinToString("\n") { contact ->
            buildString {
                append(contact.name)
                if (contact.role.isNotBlank()) append(" (${contact.role})")
                if (contact.phoneNumbers.isNotEmpty()) append(": ${contact.phoneNumbers.joinToString(", ")}")
                if (contact.notes.isNotBlank()) append(" - ${contact.notes}")
            }
        }

    val medicalNotes: String
        get() = buildList {
            if (medical.medications.isNotEmpty()) add("Medications: ${medical.medications.joinToString(", ")}")
            if (medical.allergies.isNotEmpty()) add("Allergies: ${medical.allergies.joinToString(", ")}")
            if (medical.conditions.isNotEmpty()) add("Conditions: ${medical.conditions.joinToString(", ")}")
            if (medical.emergencyContact.isNotBlank()) add("Emergency contact: ${medical.emergencyContact}")
        }.joinToString("\n")

    val paymentHint: String
        get() = paymentHints.joinToString("\n") { "${it.label}: ${it.safeToShare}" }

    private val homeAddressRecord: Address?
        get() = addresses.firstOrNull { it.label.equals("home", ignoreCase = true) }
            ?: addresses.firstOrNull()

    fun copyFromSetupFields(
        name: String = this.name,
        preferredLanguage: String = this.preferredLanguage,
        homeAddress: String = this.homeAddress,
        voiceFriendlyAddress: String = this.voiceFriendlyAddress,
        importantPeople: String = this.importantPeople,
        medicalNotes: String = this.medicalNotes,
        paymentHint: String = this.paymentHint,
    ): UserContext {
        return copy(
            name = name,
            preferredLanguage = preferredLanguage.ifBlank { "English" },
            addresses = parseAddress(homeAddress, voiceFriendlyAddress),
            contacts = parseContacts(importantPeople),
            medical = parseMedical(medicalNotes),
            paymentHints = parsePaymentHints(paymentHint),
        )
    }

    fun forPrompt(): String = buildString {
        appendLine("User name: ${name.ifBlank { "Unknown" }}")
        appendLine("Preferred language: $preferredLanguage")
        if (addresses.isNotEmpty()) {
            appendLine("Addresses:")
            addresses.forEach { address ->
                appendLine("- ${address.label}: ${address.voiceFriendly.ifBlank { address.fullAddress }}")
            }
        }
        if (contacts.isNotEmpty()) {
            appendLine("Contacts:")
            contacts.forEach { contact ->
                appendLine("- ${contact.name} (${contact.role}): ${contact.phoneNumbers.joinToString(", ")} ${contact.notes}")
            }
        }
        if (medicalNotes.isNotBlank()) appendLine("Medical: $medicalNotes")
        if (paymentHint.isNotBlank()) appendLine("Payment hints: $paymentHint")
    }

    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("preferredLanguage", preferredLanguage)
        .put("addresses", JSONArray().apply {
            addresses.forEach { address ->
                put(
                    JSONObject()
                        .put("id", address.id)
                        .put("label", address.label)
                        .put("fullAddress", address.fullAddress)
                        .put("landmark", address.landmark)
                        .put("voiceFriendly", address.voiceFriendly),
                )
            }
        })
        .put("contacts", JSONArray().apply {
            contacts.forEach { contact ->
                put(
                    JSONObject()
                        .put("id", contact.id)
                        .put("name", contact.name)
                        .put("role", contact.role)
                        .put("phoneNumbers", JSONArray(contact.phoneNumbers))
                        .put("notes", contact.notes)
                        .put("lastCallSummary", contact.lastCallSummary),
                )
            }
        })
        .put(
            "medical",
            JSONObject()
                .put("id", medical.id)
                .put("medications", JSONArray(medical.medications))
                .put("allergies", JSONArray(medical.allergies))
                .put("conditions", JSONArray(medical.conditions))
                .put("emergencyContact", medical.emergencyContact),
        )
        .put("paymentHints", JSONArray().apply {
            paymentHints.forEach { hint ->
                put(
                    JSONObject()
                        .put("id", hint.id)
                        .put("label", hint.label)
                        .put("safeToShare", hint.safeToShare),
                )
            }
        })

    companion object {
        fun fromJson(json: JSONObject): UserContext {
            return UserContext(
                name = json.optString("name"),
                preferredLanguage = json.optString("preferredLanguage", "English"),
                addresses = json.optJSONArray("addresses")?.mapObjects { item ->
                    Address(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                        label = item.optString("label", "home"),
                        fullAddress = item.optString("fullAddress"),
                        landmark = item.optString("landmark"),
                        voiceFriendly = item.optString("voiceFriendly"),
                    )
                }.orEmpty(),
                contacts = json.optJSONArray("contacts")?.mapObjects { item ->
                    Contact(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                        name = item.optString("name"),
                        role = item.optString("role"),
                        phoneNumbers = item.optJSONArray("phoneNumbers")?.mapStrings().orEmpty(),
                        notes = item.optString("notes"),
                        lastCallSummary = item.optString("lastCallSummary"),
                    )
                }.orEmpty(),
                medical = json.optJSONObject("medical")?.let { item ->
                    MedicalContext(
                        id = item.optString("id", "medical-default"),
                        medications = item.optJSONArray("medications")?.mapStrings().orEmpty(),
                        allergies = item.optJSONArray("allergies")?.mapStrings().orEmpty(),
                        conditions = item.optJSONArray("conditions")?.mapStrings().orEmpty(),
                        emergencyContact = item.optString("emergencyContact"),
                    )
                } ?: MedicalContext(),
                paymentHints = json.optJSONArray("paymentHints")?.mapObjects { item ->
                    PaymentHint(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                        label = item.optString("label", "default"),
                        safeToShare = item.optString("safeToShare"),
                    )
                }.orEmpty(),
            ).withLegacyFallbacks(json)
        }

        private fun parseAddress(fullAddress: String, voiceFriendlyAddress: String): List<Address> {
            if (fullAddress.isBlank() && voiceFriendlyAddress.isBlank()) return emptyList()
            return listOf(
                Address(
                    label = "home",
                    fullAddress = fullAddress.trim(),
                    voiceFriendly = voiceFriendlyAddress.trim().ifBlank { fullAddress.trim() },
                ),
            )
        }

        private fun parseContacts(raw: String): List<Contact> {
            return raw.lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { line ->
                    val role = Regex("\\(([^)]+)\\)").find(line)?.groupValues?.getOrNull(1).orEmpty()
                    val phoneNumbers = Regex("(?:\\+?\\d[\\d\\s-]{6,}\\d)")
                        .findAll(line)
                        .map { it.value.trim() }
                        .toList()
                    val name = line
                        .substringBefore(":")
                        .replace(Regex("\\([^)]+\\)"), "")
                        .trim()
                        .ifBlank { line.take(40) }
                    Contact(
                        name = name,
                        role = role.ifBlank { "Contact" },
                        phoneNumbers = phoneNumbers,
                        notes = line,
                    )
                }.toList()
        }

        private fun parseMedical(raw: String): MedicalContext {
            val lines = raw.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
            val medications = lines.valuesAfter("medications")
            val allergies = lines.valuesAfter("allergies")
            val conditions = lines.valuesAfter("conditions")
            return MedicalContext(
                medications = medications,
                allergies = allergies,
                conditions = conditions.ifEmpty {
                    if (medications.isEmpty() && allergies.isEmpty()) lines else emptyList()
                },
                emergencyContact = lines.firstOrNull { it.startsWith("emergency", ignoreCase = true) }
                    ?.substringAfter(":")
                    ?.trim()
                    .orEmpty(),
            )
        }

        private fun parsePaymentHints(raw: String): List<PaymentHint> {
            return raw.lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { line ->
                    PaymentHint(
                        label = line.substringBefore(":").trim().ifBlank { "default" },
                        safeToShare = line.substringAfter(":", line).trim(),
                    )
                }.toList()
        }
    }
}

class UserContextStore(context: Context) {
    private val secureStore = SecureJsonStore(
        context = context.applicationContext,
        prefsName = PREFS_NAME,
        keyAlias = KEY_ALIAS,
    )

    fun load(): UserContext {
        return secureStore.readObject(KEY_CONTEXT)?.let(UserContext::fromJson) ?: UserContext()
    }

    fun save(userContext: UserContext) {
        secureStore.writeObject(KEY_CONTEXT, userContext.toJson())
    }

    companion object {
        private const val PREFS_NAME = "dhwani_context"
        private const val KEY_CONTEXT = "user_context"
        private const val KEY_ALIAS = "dhwani_context_key"
    }
}

private fun UserContext.withLegacyFallbacks(json: JSONObject): UserContext {
    if (addresses.isNotEmpty() || contacts.isNotEmpty() || paymentHints.isNotEmpty() || medicalNotes.isNotBlank()) {
        return this
    }
    return copyFromSetupFields(
        homeAddress = json.optString("homeAddress"),
        voiceFriendlyAddress = json.optString("voiceFriendlyAddress"),
        importantPeople = json.optString("importantPeople"),
        medicalNotes = json.optString("medicalNotes"),
        paymentHint = json.optString("paymentHint"),
    )
}

private fun JSONArray.mapStrings(): List<String> {
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { add(transform(it)) }
        }
    }
}

private fun List<String>.valuesAfter(label: String): List<String> {
    val line = firstOrNull { it.startsWith(label, ignoreCase = true) } ?: return emptyList()
    return line.substringAfter(":")
        .split(",", ";")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}
