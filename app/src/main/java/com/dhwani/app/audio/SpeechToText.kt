package com.dhwani.app.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer

class SpeechToText(modelPath: String) : AutoCloseable {
    private val model = Model(modelPath)
    private val recognizer = Recognizer(model, SpeakerphoneRecorder.SAMPLE_RATE.toFloat())

    fun transcribeStream(pcm: Flow<ShortArray>): Flow<Transcript> = flow {
        pcm.collect { chunk ->
            val bytes = ByteArray(chunk.size * 2)
            for (index in chunk.indices) {
                bytes[index * 2] = (chunk[index].toInt() and 0xFF).toByte()
                bytes[index * 2 + 1] = ((chunk[index].toInt() shr 8) and 0xFF).toByte()
            }

            val isFinal = recognizer.acceptWaveForm(bytes, bytes.size)
            val json = if (isFinal) recognizer.result else recognizer.partialResult
            val key = if (isFinal) "text" else "partial"
            val text = JSONObject(json).optString(key)
            if (text.isNotBlank()) emit(Transcript(text, isFinal))
        }
    }.flowOn(Dispatchers.Default)

    override fun close() {
        recognizer.close()
        model.close()
    }

    data class Transcript(
        val text: String,
        val isFinal: Boolean,
    )
}
