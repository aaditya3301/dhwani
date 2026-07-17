package com.dhwani.app.sign

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignLandmarkPreprocessorTest {
    @Test
    fun repeatsShortClipAndProducesModelShape() {
        val first = frame(leftShoulderX = 0.3f, rightShoulderX = 0.7f, handOffset = 0.1f)
        val second = frame(leftShoulderX = 0.3f, rightShoulderX = 0.7f, handOffset = 0.2f)

        val output = SignLandmarkPreprocessor.prepare(listOf(first, second))

        assertEquals(32 * 54, output.size)
        assertEquals(-0.5f, output[3 * 2], 0.0001f)
        assertEquals(0.5f, output[4 * 2], 0.0001f)
        assertTrue(output.all(Float::isFinite))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsClipWithoutShoulderScale() {
        SignLandmarkPreprocessor.prepare(
            listOf(frame(leftShoulderX = 0.5f, rightShoulderX = 0.5f, handOffset = 0f)),
        )
    }

    private fun frame(
        leftShoulderX: Float,
        rightShoulderX: Float,
        handOffset: Float,
    ): FloatArray = FloatArray(54).apply {
        repeat(27) { point ->
            this[point * 2] = 0.5f + handOffset
            this[point * 2 + 1] = 0.5f
        }
        this[3 * 2] = leftShoulderX
        this[4 * 2] = rightShoulderX
    }
}
