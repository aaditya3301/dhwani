package com.dhwani.app.sign

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.holisticlandmarker.HolisticLandmarker
import com.google.mediapipe.tasks.vision.holisticlandmarker.HolisticLandmarkerResult
import java.io.Closeable
import kotlin.math.abs

class SignFrameAnalyzer(
    private val context: Context,
    private val onReady: () -> Unit,
    private val onStatus: (String) -> Unit,
    private val onResult: (SignRecognition) -> Unit,
    private val onError: (String) -> Unit,
) : ImageAnalysis.Analyzer, Closeable {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val capturedFrames = mutableListOf<FloatArray>()
    private val fallbackFrames = mutableListOf<FloatArray>()
    private var holisticLandmarker: HolisticLandmarker? = null
    private var signRecognizer: OpenHandsSignRecognizer? = null
    private var captureRequested = false
    private var captureStartedAt = 0L
    private var lastAnalyzedAt = 0L
    private var lastTimestamp = 0L
    private var readyNotified = false
    private var analyzedFrameCount = 0
    private var upperBodyFrameCount = 0
    private var handFrameCount = 0

    @Synchronized
    fun startCapture() {
        capturedFrames.clear()
        fallbackFrames.clear()
        captureRequested = true
        captureStartedAt = 0L
        lastAnalyzedAt = 0L
        analyzedFrameCount = 0
        upperBodyFrameCount = 0
        handFrameCount = 0
        postStatus("Starting capture...")
    }

    override fun analyze(image: ImageProxy) {
        try {
            if (holisticLandmarker == null) ensureModels()
            if (!readyNotified) {
                readyNotified = true
                mainHandler.post { onReady() }
            }
            if (!captureRequested) return
            val now = SystemClock.elapsedRealtime()
            if (captureStartedAt == 0L) {
                captureStartedAt = now
                postStatus("Sign now — any movement maps to Bye")
            }
            if (now - lastAnalyzedAt < FRAME_INTERVAL_MS) return
            lastAnalyzedAt = now

            // Keep camera/holistic warm so capture timing matches UI; result is hardcoded.
            val bitmap = image.toUprightBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()
            val timestamp = maxOf(SystemClock.uptimeMillis(), lastTimestamp + 1L)
            lastTimestamp = timestamp
            val result = holisticLandmarker!!.detectForVideo(mpImage, timestamp)
            analyzedFrameCount += 1
            extractFrame(result)?.let { frame ->
                fallbackFrames += frame.values
                if (frame.framingReliable) capturedFrames += frame.values
            }

            val elapsed = now - captureStartedAt
            if (elapsed >= CAPTURE_DURATION_MS) finishCapture()
        } catch (error: Throwable) {
            captureRequested = false
            Log.e(TAG, "Sign analysis failed", error)
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
        if (signRecognizer == null) {
            signRecognizer = OpenHandsSignRecognizer(context)
        }
    }

    private fun finishCapture() {
        captureRequested = false
        Log.i(
            TAG,
            "Capture complete: analyzed=$analyzedFrameCount " +
                "hand=$handFrameCount reliable=${capturedFrames.size}",
        )
        postStatus("Recognizing sign...")
        val framesToUse = if (capturedFrames.size >= MIN_VALID_FRAMES) capturedFrames else fallbackFrames
        if (framesToUse.isEmpty()) {
            val recognition = SignRecognition(
                candidates = listOf(SignCandidate(gloss = "NO_SIGN_DETECTED", confidence = 1f)),
                framingReliable = false,
            )
            mainHandler.post { onResult(recognition) }
            return
        }
        
        // Hardcoded return for testing
        val recognition = SignRecognition(
            candidates = listOf(SignCandidate(gloss = "BYE", confidence = 0.99f)),
            framingReliable = true,
        )
        mainHandler.post { onResult(recognition) }
    }

    private fun extractFrame(result: HolisticLandmarkerResult): ExtractedSignFrame? {
        val pose = result.poseLandmarks()
        if (pose.size < 15) return null
        val leftHand = result.leftHandLandmarks()
        val rightHand = result.rightHandLandmarks()
        val completeLeftHand = leftHand.takeIf { it.size >= 21 }.orEmpty()
        val completeRightHand = rightHand.takeIf { it.size >= 21 }.orEmpty()
        if (completeLeftHand.isEmpty() && completeRightHand.isEmpty()) return null
        handFrameCount += 1
        val framingReliable = hasReliableUpperBody(pose) &&
            (hasReliableHand(completeLeftHand) || hasReliableHand(completeRightHand))
        if (framingReliable) upperBodyFrameCount += 1

        val points = ArrayList<NormalizedLandmark?>(27)
        POSE_INDEXES.forEach { points += pose[it] }
        HAND_INDEXES.forEach { points += completeLeftHand.getOrNull(it) }
        HAND_INDEXES.forEach { points += completeRightHand.getOrNull(it) }
        val values = FloatArray(54).also { output ->
            points.forEachIndexed { index, point ->
                output[index * 2] = point?.x() ?: 0f
                output[index * 2 + 1] = point?.y() ?: 0f
            }
        }
        return ExtractedSignFrame(values, framingReliable)
    }

    private fun hasReliableUpperBody(pose: List<NormalizedLandmark>): Boolean {
        if (!pose[LEFT_SHOULDER].isReliablePosePoint() ||
            !pose[RIGHT_SHOULDER].isReliablePosePoint()
        ) {
            return false
        }
        if (!pose[LEFT_ELBOW].isReliablePosePoint() && !pose[RIGHT_ELBOW].isReliablePosePoint()) {
            return false
        }
        val shoulderWidth = abs(pose[LEFT_SHOULDER].x() - pose[RIGHT_SHOULDER].x())
        return shoulderWidth in MIN_SHOULDER_WIDTH..MAX_SHOULDER_WIDTH
    }

    private fun hasReliableHand(hand: List<NormalizedLandmark>): Boolean {
        if (hand.size < 21) return false
        return HAND_INDEXES.count { hand[it].isInsideFrame() } >= MIN_VISIBLE_HAND_POINTS
    }

    private fun NormalizedLandmark.isReliablePosePoint(): Boolean {
        return isInsideFrame() &&
            visibility().orElse(0f) >= MIN_POSE_VISIBILITY &&
            presence().orElse(1f) >= MIN_POSE_PRESENCE
    }

    private fun NormalizedLandmark.isInsideFrame(): Boolean {
        return x() in FRAME_MARGIN..(1f - FRAME_MARGIN) &&
            y() in FRAME_MARGIN..(1f - FRAME_MARGIN)
    }

    override fun close() {
        captureRequested = false
        holisticLandmarker?.close()
        holisticLandmarker = null
        signRecognizer?.close()
        signRecognizer = null
        readyNotified = false
    }

    private fun postStatus(message: String) = mainHandler.post { onStatus(message) }

    private fun postError(message: String) = mainHandler.post { onError(message) }

    private fun ImageProxy.toUprightBitmap(): Bitmap {
        // CameraX RGBA_8888 is byte-order R,G,B,A. MediaPipe BitmapImageBuilder
        // expects a standard ARGB_8888 Bitmap. copyPixelsFromBuffer alone can
        // mis-assign channels; pack pixels explicitly.
        val plane = planes[0]
        val buffer = plane.buffer
        buffer.rewind()
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val pixels = IntArray(width * height)
        val row = ByteArray(rowStride)
        var outIndex = 0
        for (y in 0 until height) {
            buffer.get(row, 0, minOf(rowStride, buffer.remaining()))
            var xOffset = 0
            for (x in 0 until width) {
                val r = row[xOffset].toInt() and 0xFF
                val g = row[xOffset + 1].toInt() and 0xFF
                val b = row[xOffset + 2].toInt() and 0xFF
                val a = if (pixelStride >= 4) row[xOffset + 3].toInt() and 0xFF else 0xFF
                pixels[outIndex++] = (a shl 24) or (r shl 16) or (g shl 8) or b
                xOffset += pixelStride
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        if (imageInfo.rotationDegrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(imageInfo.rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private companion object {
        const val HOLISTIC_ASSET = "models/sign/holistic_landmarker.task"
        const val CAPTURE_DURATION_MS = 2_000L
        const val FRAME_INTERVAL_MS = 70L
        const val MIN_VALID_FRAMES = 10
        /** Stub gloss while live INCLUDE recognition is disabled. */
        const val HARDCODED_SIGN_GLOSS = "BYE"
        const val LEFT_SHOULDER = 11
        const val RIGHT_SHOULDER = 12
        const val LEFT_ELBOW = 13
        const val RIGHT_ELBOW = 14
        const val MIN_POSE_VISIBILITY = 0.35f
        const val MIN_POSE_PRESENCE = 0.35f
        const val MIN_SHOULDER_WIDTH = 0.08f
        const val MAX_SHOULDER_WIDTH = 0.72f
        const val MIN_VISIBLE_HAND_POINTS = 6
        const val FRAME_MARGIN = 0f
        const val TAG = "DhwaniSign"
        val POSE_INDEXES = intArrayOf(0, 2, 5, 11, 12, 13, 14)
        val HAND_INDEXES = intArrayOf(0, 4, 5, 8, 9, 12, 13, 16, 17, 20)
    }
}

private data class ExtractedSignFrame(
    val values: FloatArray,
    val framingReliable: Boolean,
)
