package com.dhwani.app.sign

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.holisticlandmarker.HolisticLandmarker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignModelSmokeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun allBundledSignModelsLoadAndRunOnDevice() {
        val blankBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val image = BitmapImageBuilder(blankBitmap).build()

        val gestureOptions = GestureRecognizer.GestureRecognizerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("models/sign/gesture_recognizer.task")
                    .build(),
            )
            .setRunningMode(RunningMode.IMAGE)
            .build()
        GestureRecognizer.createFromOptions(context, gestureOptions).use { recognizer ->
            assertTrue(recognizer.recognize(image).gestures().isEmpty())
        }

        val holisticOptions = HolisticLandmarker.HolisticLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("models/sign/holistic_landmarker.task")
                    .build(),
            )
            .setRunningMode(RunningMode.IMAGE)
            .build()
        HolisticLandmarker.createFromOptions(context, holisticOptions).use { landmarker ->
            assertTrue(landmarker.detect(image).poseLandmarks().isEmpty())
        }

        IncludeSignRecognizer(context).use { recognizer ->
            val recognition = recognizer.recognize(List(12) { syntheticLandmarkFrame() })
            assertEquals(3, recognition.candidates.size)
            assertTrue(recognition.candidates.all { it.confidence in 0f..1f })
        }
    }

    private fun syntheticLandmarkFrame(): FloatArray {
        return FloatArray(134) { Float.NaN }.also { frame ->
            for (point in 0 until 25) {
                frame[point * 2] = 0.30f + point * 0.01f
                frame[point * 2 + 1] = 0.20f + point * 0.008f
            }
            for (point in 0 until 21) {
                val leftOffset = 25 * 2 + point * 2
                val rightOffset = 25 * 2 + 21 * 2 + point * 2
                frame[leftOffset] = 0.30f + point * 0.008f
                frame[leftOffset + 1] = 0.35f + point * 0.006f
                frame[rightOffset] = 0.70f - point * 0.008f
                frame[rightOffset + 1] = 0.35f + point * 0.006f
            }
        }
    }
}
