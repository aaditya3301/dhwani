package com.dhwani.app.ui

import com.dhwani.app.audio.SpeechToText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallTranscriptTest {
    @Test
    fun partialUpdatesReplaceTheActiveCallerLine() {
        val first = mergeCallerTranscript(
            emptyList(),
            SpeechToText.Transcript("hello", isFinal = false),
        )
        val updated = mergeCallerTranscript(
            first,
            SpeechToText.Transcript("hello can you hear me", isFinal = false),
        )

        assertEquals(1, updated.size)
        assertEquals(first.single().id, updated.single().id)
        assertEquals("hello can you hear me", updated.single().text)
        assertFalse(updated.single().isFinal)
    }

    @Test
    fun finalResultReplacesTheActivePartial() {
        val partial = CaptionLine(
            text = "namaste",
            isFinal = false,
            speaker = Speaker.Caller,
            id = 42L,
        )

        val updated = mergeCallerTranscript(
            listOf(partial),
            SpeechToText.Transcript("namaste kaise hain", isFinal = true),
        )

        assertEquals(1, updated.size)
        assertEquals(42L, updated.single().id)
        assertEquals("namaste kaise hain", updated.single().text)
        assertTrue(updated.single().isFinal)
    }
}
