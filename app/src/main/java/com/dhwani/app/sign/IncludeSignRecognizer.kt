package com.dhwani.app.sign

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.os.SystemClock
import android.util.Log
import org.json.JSONObject
import java.io.Closeable
import java.nio.FloatBuffer
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.roundToInt

data class SignCandidate(
    val gloss: String,
    val confidence: Float,
)

data class SignRecognition(
    val candidates: List<SignCandidate>,
    val framingReliable: Boolean = true,
    val temporalAgreement: Float = 1f,
) {
    val top: SignCandidate get() = candidates.first()
}

class IncludeSignRecognizer(context: Context) : Closeable {
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

        val warmupStartedAt = SystemClock.elapsedRealtime()
        val warmup = runModel(listOf(FloatArray(SignLandmarkPreprocessor.FEATURES) { Float.NaN }))
        check(warmup.size == OUTPUT_CLASSES) { "INCLUDE model warm-up returned an invalid output" }
        Log.i(TAG, "INCLUDE model warm-up completed in ${SystemClock.elapsedRealtime() - warmupStartedAt}ms")
    }

    fun recognize(
        frames: List<FloatArray>,
        sourceFramesPerSecond: Float = SignFrameRateNormalizer.TARGET_FRAMES_PER_SECOND,
    ): SignRecognition {
        val startedAt = SystemClock.elapsedRealtime()
        val normalizedFrames = SignFrameRateNormalizer.normalize(
            frames = frames,
            sourceFramesPerSecond = sourceFramesPerSecond,
        )
        val probabilities = SignTemporalConsensus.createViews(normalizedFrames).map(::runModel)
        return SignTemporalConsensus.aggregate(probabilities, labels).also {
            Log.i(
                TAG,
                "INCLUDE ${probabilities.size}-view inference raw=${frames.size} " +
                    "normalized=${normalizedFrames.size} sourceFps=$sourceFramesPerSecond completed in " +
                    "${SystemClock.elapsedRealtime() - startedAt}ms",
            )
        }
    }

    override fun close() {
        session.close()
    }

    private fun runModel(frames: List<FloatArray>): FloatArray {
        val input = SignLandmarkPreprocessor.prepare(frames)
        OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(input),
            longArrayOf(
                1,
                SignLandmarkPreprocessor.TARGET_FRAMES.toLong(),
                SignLandmarkPreprocessor.FEATURES.toLong(),
            ),
        ).use { tensor ->
            session.run(mapOf(INPUT_NAME to tensor)).use { output ->
                @Suppress("UNCHECKED_CAST")
                val logits = (output[0].value as Array<FloatArray>)[0]
                return softmax(logits)
            }
        }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: return FloatArray(0)
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
        const val MODEL_ASSET = "models/sign/include_transformer_small.onnx"
        const val LABELS_ASSET = "models/sign/include_transformer_labels.json"
        const val INPUT_NAME = "landmarks"
        const val OUTPUT_CLASSES = 263
        const val TAG = "DhwaniSign"
    }
}

internal object SignFrameRateNormalizer {
    const val TARGET_FRAMES_PER_SECOND = 30f

    fun normalize(
        frames: List<FloatArray>,
        sourceFramesPerSecond: Float,
    ): List<FloatArray> {
        require(frames.isNotEmpty()) { "No pose landmarks were captured" }
        require(frames.all { it.size == SignLandmarkPreprocessor.FEATURES }) {
            "Unexpected landmark frame shape"
        }
        val source = frames.take(SignLandmarkPreprocessor.TARGET_FRAMES)
        if (source.size == 1 || !sourceFramesPerSecond.isFinite() || sourceFramesPerSecond <= 0f) {
            return source
        }

        val targetCount = (
            ((source.size - 1) * TARGET_FRAMES_PER_SECOND / sourceFramesPerSecond).roundToInt() + 1
            ).coerceIn(source.size, SignLandmarkPreprocessor.TARGET_FRAMES)
        if (targetCount == source.size) return source

        val sourceSpan = source.size - 1f
        val targetSpan = targetCount - 1f
        return List(targetCount) { targetIndex ->
            val sourcePosition = targetIndex * sourceSpan / targetSpan
            val lowerIndex = floor(sourcePosition).toInt().coerceIn(source.indices)
            val upperIndex = (lowerIndex + 1).coerceIn(source.indices)
            if (lowerIndex == upperIndex) {
                source[lowerIndex].copyOf()
            } else {
                interpolateFrame(
                    start = source[lowerIndex],
                    end = source[upperIndex],
                    fraction = sourcePosition - lowerIndex,
                )
            }
        }
    }

    private fun interpolateFrame(
        start: FloatArray,
        end: FloatArray,
        fraction: Float,
    ): FloatArray = FloatArray(SignLandmarkPreprocessor.FEATURES) { feature ->
        val startValue = start[feature]
        val endValue = end[feature]
        if (startValue.isFinite() && endValue.isFinite()) {
            startValue + (endValue - startValue) * fraction
        } else {
            Float.NaN
        }
    }
}

object SignLandmarkPreprocessor {
    const val TARGET_FRAMES = 169
    const val FEATURES = 134
    private const val FRAME_WIDTH = 1920f
    private const val FRAME_HEIGHT = 1080f

    fun prepare(frames: List<FloatArray>): FloatArray {
        require(frames.isNotEmpty()) { "No pose landmarks were captured" }
        require(frames.all { it.size == FEATURES }) {
            "Expected 25 pose and 42 hand x/y landmark pairs"
        }

        val source = frames.take(TARGET_FRAMES)
        val output = FloatArray(TARGET_FRAMES * FEATURES)
        repeat(FEATURES) { feature ->
            interpolateFeature(source, feature, output)
        }
        return output
    }

    private fun interpolateFeature(
        frames: List<FloatArray>,
        feature: Int,
        output: FloatArray,
    ) {
        val validIndexes = frames.indices.filter { frames[it][feature].isFinite() }
        if (validIndexes.isEmpty()) return

        val scale = if (feature % 2 == 0) FRAME_WIDTH else FRAME_HEIGHT
        val firstIndex = validIndexes.first()
        val firstValue = frames[firstIndex][feature]
        for (frameIndex in 0..firstIndex) {
            output[frameIndex * FEATURES + feature] = firstValue * scale
        }

        validIndexes.zipWithNext().forEach { (startIndex, endIndex) ->
            val startValue = frames[startIndex][feature]
            val endValue = frames[endIndex][feature]
            val distance = endIndex - startIndex
            for (frameIndex in (startIndex + 1)..endIndex) {
                val fraction = (frameIndex - startIndex).toFloat() / distance
                val value = startValue + (endValue - startValue) * fraction
                output[frameIndex * FEATURES + feature] = value * scale
            }
        }

        val lastIndex = validIndexes.last()
        val lastValue = frames[lastIndex][feature] * scale
        for (frameIndex in lastIndex until frames.size) {
            output[frameIndex * FEATURES + feature] = lastValue
        }
    }
}

internal object SignTemporalConsensus {
    private const val MIN_FRAMES_FOR_TRIMMED_VIEWS = 9

    fun createViews(frames: List<FloatArray>): List<List<FloatArray>> {
        require(frames.isNotEmpty()) { "No pose landmarks were captured" }
        val bounded = frames.take(SignLandmarkPreprocessor.TARGET_FRAMES)
        if (bounded.size < MIN_FRAMES_FOR_TRIMMED_VIEWS) return listOf(bounded)

        val trim = (bounded.size / 8).coerceIn(1, bounded.size / 3)
        return listOf(
            bounded,
            bounded.drop(trim),
            bounded.dropLast(trim),
        )
    }

    fun aggregate(
        probabilitySets: List<FloatArray>,
        labels: List<String>,
    ): SignRecognition {
        require(probabilitySets.isNotEmpty()) { "No model predictions were produced" }
        require(probabilitySets.all { it.size == labels.size }) {
            "Model prediction size does not match INCLUDE labels"
        }

        val averaged = FloatArray(labels.size)
        probabilitySets.forEach { probabilities ->
            probabilities.indices.forEach { index -> averaged[index] += probabilities[index] }
        }
        averaged.indices.forEach { averaged[it] /= probabilitySets.size }

        val viewWinners = probabilitySets.map { probabilities ->
            probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        }
        val winner = viewWinners
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<Int, Int>> { it.value }
                    .thenByDescending { averaged[it.key] },
            )
            .first()
        val rankedIndexes = buildList {
            add(winner.key)
            addAll(
                averaged.indices
                    .filterNot { it == winner.key }
                    .sortedByDescending { averaged[it] }
                    .take(2),
            )
        }

        return SignRecognition(
            candidates = rankedIndexes.map { index ->
                SignCandidate(labels[index], averaged[index])
            },
            temporalAgreement = winner.value.toFloat() / probabilitySets.size,
        )
    }
}
