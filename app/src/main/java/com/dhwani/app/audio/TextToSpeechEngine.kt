package com.dhwani.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TextToSpeechEngine(context: Context) : AutoCloseable {
    private val initialization = CompletableDeferred<Boolean>()
    private val speakMutex = Mutex()
    @Volatile
    private var closed = false
    private val tts = TextToSpeech(context.applicationContext) { status ->
        initialization.complete(status == TextToSpeech.SUCCESS)
    }

    init {
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
    }

    suspend fun speak(text: String, lang: String = "en-IN") = speakMutex.withLock {
        if (text.isBlank()) return@withLock
        check(!closed) { "Text-to-speech is closed" }
        check(withTimeout(INITIALIZATION_TIMEOUT_MS) { initialization.await() }) {
            "Android text-to-speech failed to initialize"
        }
        check(!closed) { "Text-to-speech is closed" }

        val languageResult = tts.setLanguage(Locale.forLanguageTag(lang))
        check(languageResult != TextToSpeech.LANG_MISSING_DATA) {
            "Install Android speech data for $lang"
        }
        check(languageResult != TextToSpeech.LANG_NOT_SUPPORTED) {
            "Android text-to-speech does not support $lang"
        }

        val utteranceId = System.currentTimeMillis().toString()
        suspendCancellableCoroutine { continuation ->
            val completed = AtomicBoolean(false)
            val finish: (Throwable?) -> Unit = { error ->
                if (completed.compareAndSet(false, true) && continuation.isActive) {
                    if (error == null) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWithException(error)
                    }
                }
            }

            tts.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(doneId: String?) {
                        if (doneId == utteranceId) finish(null)
                    }

                    override fun onStop(stoppedId: String?, interrupted: Boolean) {
                        if (stoppedId == utteranceId) finish(null)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(errorId: String?) {
                        if (errorId == utteranceId) {
                            finish(IllegalStateException("Android text-to-speech playback failed"))
                        }
                    }
                },
            )
            continuation.invokeOnCancellation {
                if (completed.compareAndSet(false, true)) tts.stop()
            }
            if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) == TextToSpeech.ERROR) {
                finish(IllegalStateException("Android text-to-speech rejected the reply"))
            }
        }
    }

    override fun close() {
        closed = true
        initialization.complete(false)
        tts.stop()
        tts.shutdown()
    }

    companion object {
        private const val INITIALIZATION_TIMEOUT_MS = 10_000L
    }
}
