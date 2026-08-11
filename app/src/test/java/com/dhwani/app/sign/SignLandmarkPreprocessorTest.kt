package com.dhwani.app.sign

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignLandmarkPreprocessorTest {
    @Test
    fun scalesCoordinatesAndPadsToOfficialModelShape() {
        val first = emptyFrame().apply {
            this[0] = 0.25f
            this[1] = 0.50f
        }
        val second = emptyFrame().apply {
            this[0] = 0.75f
            this[1] = 0.25f
        }

        val output = SignLandmarkPreprocessor.prepare(listOf(first, second))

        assertEquals(169 * 134, output.size)
        assertEquals(480f, output[0], 0.001f)
        assertEquals(540f, output[1], 0.001f)
        assertEquals(1440f, output[134], 0.001f)
        assertEquals(270f, output[135], 0.001f)
        assertEquals(0f, output[2 * 134], 0.001f)
        assertTrue(output.all(Float::isFinite))
    }

    @Test
    fun interpolatesMissingCoordinatesInBothDirections() {
        val first = emptyFrame().apply { this[0] = 0.25f }
        val middle = emptyFrame().apply { this[1] = 0.40f }
        val last = emptyFrame().apply { this[0] = 0.75f }

        val output = SignLandmarkPreprocessor.prepare(listOf(first, middle, last))

        assertEquals(480f, output[0], 0.001f)
        assertEquals(960f, output[134], 0.001f)
        assertEquals(1440f, output[2 * 134], 0.001f)
        assertEquals(432f, output[1], 0.001f)
        assertEquals(432f, output[135], 0.001f)
        assertEquals(432f, output[2 * 134 + 1], 0.001f)
        assertEquals(0f, output[2], 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsWrongLandmarkShape() {
        SignLandmarkPreprocessor.prepare(listOf(FloatArray(54)))
    }

    @Test
    fun temporalViewsTrimBothEndsOfTheRecordedSign() {
        val frames = List(24) { emptyFrame() }

        val views = SignTemporalConsensus.createViews(frames)

        assertEquals(listOf(24, 21, 21), views.map { it.size })
    }

    @Test
    fun normalizesPhoneRateLandmarksToTrainingFrameRate() {
        val frames = listOf(
            handFrame(x = 0.30f),
            handFrame(x = 0.60f),
            handFrame(x = 0.90f),
        )

        val normalized = SignFrameRateNormalizer.normalize(
            frames = frames,
            sourceFramesPerSecond = 10f,
        )

        assertEquals(7, normalized.size)
        val wristX = 25 * 2
        assertEquals(0.30f, normalized.first()[wristX], 0.001f)
        assertEquals(0.60f, normalized[3][wristX], 0.001f)
        assertEquals(0.90f, normalized.last()[wristX], 0.001f)
    }

    @Test
    fun temporalConsensusReportsTwoOfThreeAgreement() {
        val labels = listOf("HELLO", "DOCTOR", "DOG")
        val recognition = SignTemporalConsensus.aggregate(
            probabilitySets = listOf(
                floatArrayOf(0.80f, 0.10f, 0.10f),
                floatArrayOf(0.70f, 0.20f, 0.10f),
                floatArrayOf(0.20f, 0.70f, 0.10f),
            ),
            labels = labels,
        )

        assertEquals("HELLO", recognition.top.gloss)
        assertEquals(2f / 3f, recognition.temporalAgreement, 0.001f)
    }

    @Test
    fun motionAnalyzerSeparatesHeldAndMovingHands() {
        val held = List(12) { handFrame(x = 0.45f) }
        val moving = List(12) { index -> handFrame(x = 0.30f + index * 0.02f) }

        assertFalse(SignMotionAnalyzer.hasMeaningfulMotion(held))
        assertTrue(SignMotionAnalyzer.hasMeaningfulMotion(moving))
    }

    private fun emptyFrame(): FloatArray = FloatArray(134) { Float.NaN }

    private fun handFrame(x: Float): FloatArray = emptyFrame().apply {
        val leftWristOffset = 25 * 2
        this[leftWristOffset] = x
        this[leftWristOffset + 1] = 0.40f
    }
}
