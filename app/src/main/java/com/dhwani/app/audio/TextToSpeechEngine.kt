package com.dhwani.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TextToSpeechEngine(context: Context) : AutoCloseable {
    private var ready = false
    private val tts = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
    }

    init {
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
    }

    suspend fun speak(text: String, lang: String = "en-IN") = suspendCoroutine<Unit> { cont ->
        if (!ready || text.isBlank()) {
            cont.resume(Unit)
            return@suspendCoroutine
        }

        val utteranceId = System.currentTimeMillis().toString()
        val completed = AtomicBoolean(false)
        tts.language = Locale.forLanguageTag(lang)
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(doneId: String?) {
                    if (doneId == utteranceId && completed.compareAndSet(false, true)) {
                        cont.resume(Unit)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(errorId: String?) {
                    if (errorId == utteranceId && completed.compareAndSet(false, true)) {
                        cont.resume(Unit)
                    }
                }
            },
        )
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun close() {
        tts.stop()
        tts.shutdown()
    }
}
