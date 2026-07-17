package com.dhwani.app.sign

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.holisticlandmarker.HolisticLandmarker
import com.google.mediapipe.tasks.vision.holisticlandmarker.HolisticLandmarkerResult
import java.io.Closeable

class SignFrameAnalyzer(
    private val context: Context,
    private val onStatus: (String) -> Unit,
    private val onResult: (SignRecognition) -> Unit,
    private val onError: (String) -> Unit,
) : ImageAnalysis.Analyzer, Closeable {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val capturedFrames = mutableListOf<FloatArray>()
    private var holisticLandmarker: HolisticLandmarker? = null
    private var recognizer: OpenHandsSignRecognizer? = null
    private var captureRequested = false
    private var captureStartedAt = 0L
    private var lastAnalyzedAt = 0L
    private var lastTimestamp = 0L

    @Synchronized
    fun startCapture() {
        capturedFrames.clear()
        captureRequested = true
        captureStartedAt = 0L
        lastAnalyzedAt = 0L
        postStatus("Loading sign recognizer...")
    }

    override fun analyze(image: ImageProxy) {
        try {
            if (!captureRequested) return
            ensureModels()
            val now = SystemClock.elapsedRealtime()
            if (captureStartedAt == 0L) {
                captureStartedAt = now
                postStatus("Sign naturally, then hold for a moment")
            }
            if (now - lastAnalyzedAt < FRAME_INTERVAL_MS) return
            lastAnalyzedAt = now

            val bitmap = image.toUprightBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()
            val timestamp = maxOf(SystemClock.uptimeMillis(), lastTimestamp + 1L)
            lastTimestamp = timestamp
            val result = holisticLandmarker!!.detectForVideo(mpImage, timestamp)
            extractFrame(result)?.let(capturedFrames::add)

            val elapsed = now - captureStartedAt
            if (elapsed >= CAPTURE_DURATION_MS) finishCapture()
        } catch (error: Throwable) {
            captureRequested = false
            postError(error.message ?: "Sign recognition failed")
        } finally {
            image.close()
        }
    }

    private fun ensureModels() {
        if (holisticLandmarker == null) {
            val options = HolisticLandmarker.HolisticLandmarkerOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath(HOLISTIC_ASSET)
                        .build(),
                )
                .setRunningMode(RunningMode.VIDEO)
                .build()
            holisticLandmarker = HolisticLandmarker.createFromOptions(context, options)
        }
        if (recognizer == null) recognizer = OpenHandsSignRecognizer(context)
    }

    private fun finishCapture() {
        captureRequested = false
        if (capturedFrames.size < MIN_VALID_FRAMES) {
            postError("I could not see both your upper body and a hand. Try again in better light.")
            return
        }
        postStatus("Recognizing sign...")
        val recognition = recognizer!!.recognize(capturedFrames.toList())
        mainHandler.post { onResult(recognition) }
    }

    private fun extractFrame(result: HolisticLandmarkerResult): FloatArray? {
        val pose = result.poseLandmarks()
        if (pose.size < 15) return null
        val leftHand = result.leftHandLandmarks()
        val rightHand = result.rightHandLandmarks()
        if (leftHand.size < 21 && rightHand.size < 21) return null

        val points = ArrayList<NormalizedLandmark?>(27)
        POSE_INDEXES.forEach { points += pose[it] }
        HAND_INDEXES.forEach { points += leftHand.getOrNull(it) }
        HAND_INDEXES.forEach { points += rightHand.getOrNull(it) }
        return FloatArray(54).also { values ->
            points.forEachIndexed { index, point ->
                values[index * 2] = point?.x() ?: 0f
                values[index * 2 + 1] = point?.y() ?: 0f
            }
        }
    }

    override fun close() {
        captureRequested = false
        holisticLandmarker?.close()
        holisticLandmarker = null
        recognizer?.close()
        recognizer = null
    }

    private fun postStatus(message: String) = mainHandler.post { onStatus(message) }

    private fun postError(message: String) = mainHandler.post { onError(message) }

    private fun ImageProxy.toUprightBitmap(): Bitmap {
        val plane = planes[0]
        plane.buffer.rewind()
        val rowPadding = plane.rowStride - plane.pixelStride * width
        val paddedWidth = width + rowPadding / plane.pixelStride
        val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(plane.buffer)
        val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
        if (imageInfo.rotationDegrees == 0) return cropped
        val matrix = Matrix().apply { postRotate(imageInfo.rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
    }

    private companion object {
        const val HOLISTIC_ASSET = "models/sign/holistic_landmarker.task"
        const val CAPTURE_DURATION_MS = 3_200L
        const val FRAME_INTERVAL_MS = 90L
        const val MIN_VALID_FRAMES = 6
        val POSE_INDEXES = intArrayOf(0, 2, 5, 11, 12, 13, 14)
        val HAND_INDEXES = intArrayOf(0, 4, 5, 8, 9, 12, 13, 16, 17, 20)
    }
}
