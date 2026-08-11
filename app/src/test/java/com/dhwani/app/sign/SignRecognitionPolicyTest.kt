package com.dhwani.app.sign

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SignRecognitionPolicyTest {
    @Test
    fun acceptsStableGestureAcrossMostFrames() {
        val predictions = List(7) { SignCandidate("THUMBS UP", 0.88f) } +
            List(2) { SignCandidate("VICTORY", 0.70f) }

        val result = SignRecognitionPolicy.stableGesture(predictions, analyzedFrames = 12)

        assertNotNull(result)
        assertEquals("THUMBS UP", result!!.top.gloss)
    }

    @Test
    fun rejectsGestureSeenInTooFewFrames() {
        val predictions = List(4) { SignCandidate("OPEN PALM", 0.95f) }

        assertNull(SignRecognitionPolicy.stableGesture(predictions, analyzedFrames = 12))
    }

    @Test
    fun acceptsStrongSupportedIncludePrediction() {
        val recognition = recognition(
            top = SignCandidate("HELLO", 0.72f),
            second = SignCandidate("DOG", 0.12f),
            agreement = 1f,
        )

        assertNotNull(SignRecognitionPolicy.acceptInclude(recognition))
    }

    @Test
    fun acceptsStrongTwoViewConsensus() {
        val recognition = recognition(
            top = SignCandidate("HELLO", 0.43f),
            second = SignCandidate("WARM", 0.31f),
            agreement = 2f / 3f,
        )

        assertNotNull(SignRecognitionPolicy.acceptInclude(recognition))
    }

    @Test
    fun keepsNonGreetingTwoViewMatchesConservative() {
        val recognition = recognition(
            top = SignCandidate("DOCTOR", 0.58f),
            second = SignCandidate("PATIENT", 0.20f),
            agreement = 2f / 3f,
        )

        assertNull(SignRecognitionPolicy.acceptInclude(recognition))
    }

    @Test
    fun rejectsUnsupportedClosedSetWinnerEvenWhenConfident() {
        val recognition = recognition(
            top = SignCandidate("DOG", 0.91f),
            second = SignCandidate("ELECTION", 0.03f),
            agreement = 1f,
        )

        assertNull(SignRecognitionPolicy.acceptInclude(recognition))
    }

    @Test
    fun rejectsPredictionWhenTemporalViewsDisagree() {
        val recognition = recognition(
            top = SignCandidate("HELLO", 0.75f),
            second = SignCandidate("DOCTOR", 0.10f),
            agreement = 1f / 3f,
        )

        assertNull(SignRecognitionPolicy.acceptInclude(recognition))
    }

    @Test
    fun rejectsAmbiguousIncludePrediction() {
        val recognition = recognition(
            top = SignCandidate("YOU (PLURAL)", 0.58f),
            second = SignCandidate("TELEPHONE", 0.51f),
            agreement = 1f,
        )

        assertNull(SignRecognitionPolicy.acceptInclude(recognition))
    }

    @Test
    fun requiresHigherConfidenceWhenFramingIsWeak() {
        val recognition = recognition(
            top = SignCandidate("MEDICINE", 0.65f),
            second = SignCandidate("SICK", 0.10f),
            agreement = 1f,
            framingReliable = false,
        )

        assertNull(SignRecognitionPolicy.acceptInclude(recognition))
    }

    private fun recognition(
        top: SignCandidate,
        second: SignCandidate,
        agreement: Float,
        framingReliable: Boolean = true,
    ) = SignRecognition(
        candidates = listOf(top, second),
        framingReliable = framingReliable,
        temporalAgreement = agreement,
    )
}
