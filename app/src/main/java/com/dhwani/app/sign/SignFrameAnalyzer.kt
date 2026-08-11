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
import com.google.mediapipe.tasks.components.processors.ClassifierOptions
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.google.mediapipe.tasks.vision.holisticlandmarker.HolisticLandmarker
import com.google.mediapipe.tasks.vision.holisticlandmarker.HolisticLandmarkerResult
import java.io.Closeable
import kotlin.math.abs
import kotlin.math.hypot

class SignFrameAnalyzer(
    private val context: Context,
    private val onReady: () -> Unit,
    private val onStatus: (String) -> Unit,
    private val onResult: (SignRecognition) -> Unit,
    private val onError: (String) -> Unit,
) : ImageAnalysis.Analyzer, Closeable {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val capturedFrames = mutableListOf<FloatArray>()
    private val gesturePredictions = mutableListOf<SignCandidate>()
    private var holisticLandmarker: HolisticLandmarker? = null
    private var gestureRecognizer: GestureRecognizer? = null
    private var signRecognizer: IncludeSignRecognizer? = null
    private var captureRequested = false
    private var captureStartedAt = 0L
    private var lastAnalyzedAt = 0L
    private var lastTimestamp = 0L
    private var readyNotified = false
    private var analyzedFrameCount = 0
    private var gestureAnalyzedFrameCount = 0
    private var handFrameCount = 0
    private var reliableFrameCount = 0

    @Synchronized
    fun startCapture() {
        capturedFrames.clear()
        gesturePredictions.clear()
        captureRequested = true
        captureStartedAt = 0L
        lastAnalyzedAt = 0L
        analyzedFrameCount = 0
        gestureAnalyzedFrameCount = 0
        handFrameCount = 0
        reliableFrameCount = 0
        postStatus("Sign now - perform it once")
    }

    override fun analyze(image: ImageProxy) {
        try {
            if (holisticLandmarker == null || gestureRecognizer == null) ensureModels()
            if (!readyNotified) {
                readyNotified = true
                mainHandler.post { onReady() }
            }
            if (!captureRequested) return

            val now = SystemClock.elapsedRealtime()
            if (captureStartedAt == 0L) captureStartedAt = now
            if (now - lastAnalyzedAt < FRAME_INTERVAL_MS) return
            lastAnalyzedAt = now

            val bitmap = image.toUprightBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()
            val timestamp = maxOf(SystemClock.uptimeMillis(), lastTimestamp + 1L)
            lastTimestamp = timestamp

            if (analyzedFrameCount % GESTURE_SAMPLE_EVERY == 0) {
                gestureAnalyzedFrameCount += 1
                recordGesture(gestureRecognizer!!.recognizeForVideo(mpImage, timestamp))
            }
            val holisticResult = holisticLandmarker!!.detectForVideo(mpImage, timestamp)
            analyzedFrameCount += 1
            val frame = extractFrame(holisticResult)
            capturedFrames += frame.values
            if (frame.hasHand) handFrameCount += 1
            if (frame.framingReliable) reliableFrameCount += 1

            val elapsed = now - captureStartedAt
            if (elapsed >= CAPTURE_DURATION_MS) {
                finishCapture()
            } else if (elapsed >= ALMOST_DONE_STATUS_AFTER_MS) {
                postStatus("Finishing - return to a resting pose")
            } else if (elapsed >= HOLD_STATUS_AFTER_MS) {
                postStatus("Keep going - complete the sign")
            }
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
        if (gestureRecognizer == null) {
            val classifierOptions = ClassifierOptions.builder()
                .setMaxResults(1)
                .setScoreThreshold(MIN_GESTURE_FRAME_CONFIDENCE)
                .build()
            val options = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath(GESTURE_ASSET)
                        .build(),
                )
                .setRunningMode(RunningMode.VIDEO)
                .setNumHands(2)
                .setMinHandDetectionConfidence(0.45f)
                .setMinHandPresenceConfidence(0.45f)
                .setMinTrackingConfidence(0.45f)
                .setCannedGesturesClassifierOptions(classifierOptions)
                .build()
            gestureRecognizer = GestureRecognizer.createFromOptions(context, options)
        }
        if (signRecognizer == null) {
            signRecognizer = IncludeSignRecognizer(context)
        }
    }

    private fun recordGesture(result: GestureRecognizerResult) {
        val candidate = result.gestures()
            .asSequence()
            .flatMap { it.asSequence() }
            .filterNot { it.categoryName().equals("None", ignoreCase = true) }
            .maxByOrNull { it.score() }
            ?: return
        gesturePredictions += SignCandidate(
            gloss = candidate.categoryName().toDisplayGesture(),
            confidence = candidate.score(),
        )
    }

    private fun finishCapture() {
        captureRequested = false
        Log.i(
            TAG,
            "Capture complete: analyzed=$analyzedFrameCount hand=$handFrameCount " +
                "modelFrames=${capturedFrames.size} reliable=$reliableFrameCount " +
                "gestureVotes=${gesturePredictions.size}",
        )
        postStatus("Recognizing with the ISL model...")

        val stableGesture = SignRecognitionPolicy.stableGesture(
            predictions = gesturePredictions,
            analyzedFrames = gestureAnalyzedFrameCount,
        )
        val hasMeaningfulMotion = SignMotionAnalyzer.hasMeaningfulMotion(capturedFrames)
        if (stableGesture != null && !hasMeaningfulMotion) {
            mainHandler.post { onResult(stableGesture) }
            return
        }

        if (capturedFrames.size >= MIN_MODEL_FRAMES && handFrameCount >= MIN_HAND_FRAMES) {
            val framingReliable = reliableFrameCount >= MIN_RELIABLE_FRAMES &&
                reliableFrameCount * 2 >= handFrameCount
            val captureDurationMs = (SystemClock.elapsedRealtime() - captureStartedAt).coerceAtLeast(1L)
            val sourceFramesPerSecond = analyzedFrameCount * 1_000f / captureDurationMs
            val rawRecognition = signRecognizer!!.recognize(
                frames = capturedFrames,
                sourceFramesPerSecond = sourceFramesPerSecond,
            ).copy(
                framingReliable = framingReliable,
            )
            Log.i(
                TAG,
                "INCLUDE top=${rawRecognition.top.gloss} " +
                    "confidence=${rawRecognition.top.confidence} " +
                    "agreement=${rawRecognition.temporalAgreement} motion=$hasMeaningfulMotion",
            )
            SignRecognitionPolicy.acceptInclude(rawRecognition)?.let { recognition ->
                mainHandler.post { onResult(recognition) }
                return
            }
        }

        if (stableGesture != null) {
            mainHandler.post { onResult(stableGesture) }
            return
        }

        val rejected = if (handFrameCount >= MIN_HAND_FRAMES) {
            SignRecognitionPolicy.unknownSign()
        } else {
            SignRecognitionPolicy.noSign()
        }
        mainHandler.post { onResult(rejected) }
    }

    private fun extractFrame(result: HolisticLandmarkerResult): ExtractedSignFrame {
        val pose = result.poseLandmarks()
        val leftHand = result.leftHandLandmarks().takeIf { it.size >= 21 }.orEmpty()
        val rightHand = result.rightHandLandmarks().takeIf { it.size >= 21 }.orEmpty()
        val hasHand = leftHand.isNotEmpty() || rightHand.isNotEmpty()

        val framingReliable = pose.size >= POSE_POINTS && hasReliableUpperBody(pose) &&
            (hasReliableHand(leftHand) || hasReliableHand(rightHand))
        val values = FloatArray(SignLandmarkPreprocessor.FEATURES) { Float.NaN }
        if (pose.size >= POSE_POINTS) {
            repeat(POSE_POINTS) { pointIndex ->
                values[pointIndex * 2] = pose[pointIndex].x()
                values[pointIndex * 2 + 1] = pose[pointIndex].y()
            }
        }
        leftHand.copyInto(values, LEFT_HAND_OFFSET)
        rightHand.copyInto(values, RIGHT_HAND_OFFSET)
        return ExtractedSignFrame(values, hasHand, framingReliable)
    }

    private fun List<NormalizedLandmark>.copyInto(output: FloatArray, offset: Int) {
        forEachIndexed { pointIndex, point ->
            output[offset + pointIndex * 2] = point.x()
            output[offset + pointIndex * 2 + 1] = point.y()
        }
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
        return hand.count { it.isInsideFrame() } >= MIN_VISIBLE_HAND_POINTS
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
        gestureRecognizer?.close()
        gestureRecognizer = null
        signRecognizer?.close()
        signRecognizer = null
        readyNotified = false
    }

    private fun postStatus(message: String) = mainHandler.post { onStatus(message) }

    private fun postError(message: String) = mainHandler.post { onError(message) }

    private fun ImageProxy.toUprightBitmap(): Bitmap {
        val plane = planes[0]
        val buffer = plane.buffer
        buffer.rewind()
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        require(pixelStride >= 4) {
            "Camera did not provide an RGBA frame (pixel stride $pixelStride)"
        }
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

    private fun String.toDisplayGesture(): String = when (this) {
        "Closed_Fist" -> "CLOSED FIST"
        "Open_Palm" -> "OPEN PALM"
        "Pointing_Up" -> "POINTING UP"
        "Thumb_Down" -> "THUMBS DOWN"
        "Thumb_Up" -> "THUMBS UP"
        "Victory" -> "VICTORY"
        "ILoveYou" -> "I LOVE YOU"
        else -> replace('_', ' ').uppercase()
    }

    private companion object {
        const val HOLISTIC_ASSET = "models/sign/holistic_landmarker.task"
        const val GESTURE_ASSET = "models/sign/gesture_recognizer.task"
        const val CAPTURE_DURATION_MS = 3_600L
        const val HOLD_STATUS_AFTER_MS = 1_100L
        const val ALMOST_DONE_STATUS_AFTER_MS = 2_700L
        const val FRAME_INTERVAL_MS = 45L
        const val GESTURE_SAMPLE_EVERY = 2
        const val MIN_MODEL_FRAMES = 12
        const val MIN_HAND_FRAMES = 8
        const val MIN_RELIABLE_FRAMES = 8
        const val MIN_GESTURE_FRAME_CONFIDENCE = 0.35f
        const val POSE_POINTS = 25
        const val LEFT_HAND_OFFSET = POSE_POINTS * 2
        const val RIGHT_HAND_OFFSET = LEFT_HAND_OFFSET + 21 * 2
        const val LEFT_SHOULDER = 11
        const val RIGHT_SHOULDER = 12
        const val LEFT_ELBOW = 13
        const val RIGHT_ELBOW = 14
        const val MIN_POSE_VISIBILITY = 0.30f
        const val MIN_POSE_PRESENCE = 0.30f
        const val MIN_SHOULDER_WIDTH = 0.06f
        const val MAX_SHOULDER_WIDTH = 0.80f
        const val MIN_VISIBLE_HAND_POINTS = 14
        const val FRAME_MARGIN = 0f
        const val TAG = "DhwaniSign"
    }
}

private data class ExtractedSignFrame(
    val values: FloatArray,
    val hasHand: Boolean,
    val framingReliable: Boolean,
)

internal object SignMotionAnalyzer {
    private const val MOTION_THRESHOLD = 0.10f
    private val trackedPointIndexes = intArrayOf(0, 4, 8, 12, 16, 20)
    private val handOffsets = intArrayOf(25 * 2, 25 * 2 + 21 * 2)

    fun hasMeaningfulMotion(frames: List<FloatArray>): Boolean {
        return handOffsets.any { handOffset ->
            trackedPointIndexes.any { pointIndex ->
                val xIndex = handOffset + pointIndex * 2
                val yIndex = xIndex + 1
                val points = frames.mapNotNull { frame ->
                    val x = frame.getOrNull(xIndex) ?: return@mapNotNull null
                    val y = frame.getOrNull(yIndex) ?: return@mapNotNull null
                    if (x.isFinite() && y.isFinite()) x to y else null
                }
                if (points.size < 4) return@any false
                val xRange = points.maxOf { it.first } - points.minOf { it.first }
                val yRange = points.maxOf { it.second } - points.minOf { it.second }
                hypot(xRange, yRange) >= MOTION_THRESHOLD
            }
        }
    }
}
