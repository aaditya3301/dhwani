package com.dhwani.app.sign

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.json.JSONObject
import java.io.Closeable
import java.nio.FloatBuffer
import kotlin.math.exp
import kotlin.math.hypot

data class SignCandidate(
    val gloss: String,
    val confidence: Float,
)

data class SignRecognition(
    val candidates: List<SignCandidate>,
) {
    val top: SignCandidate get() = candidates.first()
}

class OpenHandsSignRecognizer(context: Context) : Closeable {
    private val environment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val labels: List<String>

    init {
        val assets = context.assets
        val model = assets.open(MODEL_ASSET).use { it.readBytes() }
        session = environment.createSession(model, OrtSession.SessionOptions())
        val labelsJson = assets.open(LABELS_ASSET).bufferedReader().use { it.readText() }
        val labelsArray = JSONObject(labelsJson).getJSONArray("display_labels")
        labels = List(labelsArray.length()) { labelsArray.getString(it) }
        check(labels.size == OUTPUT_CLASSES) { "INCLUDE label count does not match model output" }
    }

    fun recognize(frames: List<FloatArray>): SignRecognition {
        val input = SignLandmarkPreprocessor.prepare(frames)
        OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(input),
            longArrayOf(1, SignLandmarkPreprocessor.TARGET_FRAMES.toLong(), FEATURES.toLong()),
        ).use { tensor ->
            session.run(mapOf(INPUT_NAME to tensor)).use { output ->
                @Suppress("UNCHECKED_CAST")
                val logits = (output[0].value as Array<FloatArray>)[0]
                val probabilities = softmax(logits)
                val candidates = probabilities.indices
                    .sortedByDescending { probabilities[it] }
                    .take(3)
                    .map { SignCandidate(labels[it], probabilities[it]) }
                return SignRecognition(candidates)
            }
        }
    }

    override fun close() {
        session.close()
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.max()
        val values = FloatArray(logits.size)
        var total = 0.0
        logits.indices.forEach { index ->
            val value = exp((logits[index] - max).toDouble())
            values[index] = value.toFloat()
            total += value
        }
        if (total == 0.0) return values
        values.indices.forEach { values[it] = (values[it] / total).toFloat() }
        return values
    }

    private companion object {
        const val MODEL_ASSET = "models/sign/openhands_include_bilstm.onnx"
        const val LABELS_ASSET = "models/sign/include_labels.json"
        const val INPUT_NAME = "landmarks"
        const val OUTPUT_CLASSES = 263
        const val FEATURES = 54
    }
}

object SignLandmarkPreprocessor {
    const val TARGET_FRAMES = 32
    private const val POINTS = 27
    private const val FEATURES = POINTS * 2
    private const val LEFT_SHOULDER = 3
    private const val RIGHT_SHOULDER = 4

    fun prepare(frames: List<FloatArray>): FloatArray {
        require(frames.isNotEmpty()) { "No valid body and hand landmarks were captured" }
        require(frames.all { it.size == FEATURES }) { "Expected 27 x/y landmark pairs" }
        val sampled = List(TARGET_FRAMES) { outputIndex ->
            val sourceIndex = if (TARGET_FRAMES == 1) {
                0
            } else {
                ((outputIndex.toFloat() / (TARGET_FRAMES - 1)) * (frames.size - 1))
                    .toInt()
                    .coerceIn(frames.indices)
            }
            frames[sourceIndex]
        }

        var centerX = 0f
        var centerY = 0f
        var meanShoulderDistance = 0f
        sampled.forEach { frame ->
            val leftX = frame[LEFT_SHOULDER * 2]
            val leftY = frame[LEFT_SHOULDER * 2 + 1]
            val rightX = frame[RIGHT_SHOULDER * 2]
            val rightY = frame[RIGHT_SHOULDER * 2 + 1]
            centerX += (leftX + rightX) / 2f
            centerY += (leftY + rightY) / 2f
            meanShoulderDistance += hypot(leftX - rightX, leftY - rightY)
        }
        centerX /= sampled.size
        centerY /= sampled.size
        meanShoulderDistance /= sampled.size
        require(meanShoulderDistance > 1e-4f) { "Keep your shoulders visible in the camera" }

        val output = FloatArray(TARGET_FRAMES * FEATURES)
        val scale = 1f / meanShoulderDistance
        sampled.forEachIndexed { frameIndex, frame ->
            repeat(POINTS) { pointIndex ->
                val source = pointIndex * 2
                val target = frameIndex * FEATURES + source
                output[target] = (frame[source] - centerX) * scale
                output[target + 1] = (frame[source + 1] - centerY) * scale
            }
        }
        return output
    }
}
