package com.dhwani.app.sign

import com.dhwani.app.data.UserContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignVocabularyTest {
    @Test
    fun demoVocabularyContainsPhoneCallSigns() {
        val glosses = SignVocabulary.demoPhrases.map { it.gloss }

        assertTrue("HELLO" in glosses)
        assertTrue("RESCHEDULE" in glosses)
        assertTrue("APPOINTMENT" in glosses)
        assertTrue("PLEASE REPEAT" in glosses)
    }

    @Test
    fun fallbackUsesHindiWhenRequested() {
        val sentence = SignVocabulary.fallbackSentence(
            gloss = "THANK YOU",
            languageLabel = "Hindi",
            context = UserContext(),
        )

        assertEquals("धन्यवाद।", sentence)
    }

    @Test
    fun unknownGlossBecomesReadableSentence() {
        val sentence = SignVocabulary.fallbackSentence(
            gloss = "CALL_DOCTOR_NOW",
            languageLabel = "English",
            context = UserContext(),
        )

        assertEquals("Call doctor now", sentence)
    }

    @Test
    fun homeGlossUsesSavedVoiceFriendlyAddress() {
        val context = UserContext().copyFromSetupFields(
            homeAddress = "14 MG Road, Bengaluru",
            voiceFriendlyAddress = "near Trinity Metro, Bengaluru",
        )

        val sentence = SignVocabulary.fallbackSentence(
            gloss = "MY HOME",
            languageLabel = "English",
            context = context,
        )

        assertEquals("My home is near Trinity Metro, Bengaluru.", sentence)
    }
}
