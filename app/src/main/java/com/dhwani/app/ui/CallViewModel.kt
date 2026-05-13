package com.dhwani.app.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dhwani.app.audio.SpeakerphoneRecorder
import com.dhwani.app.audio.SpeechToText
import com.dhwani.app.audio.TextToSpeechEngine
import com.dhwani.app.audio.VoskModelManager
import com.dhwani.app.call.CallService
import com.dhwani.app.llm.GemmaEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CallViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val recorder = SpeakerphoneRecorder()
    private val tts = TextToSpeechEngine(appContext)
    private var stt: SpeechToText? = null
    private var transcriptJob: Job? = null
    private var startJob: Job? = null

    private val _state = MutableStateFlow(CallState(modelStatus = GemmaEngine.status))
    val state: StateFlow<CallState> = _state.asStateFlow()

    fun startCallPipe() {
        if (_state.value.isRunning || startJob?.isActive == true) return
        startJob = viewModelScope.launch {
            _state.update { it.copy(liveCaption = "Preparing speech model...") }
            withContext(Dispatchers.Default) {
                VoskModelManager.prepareModels(appContext)
            }

            routeAudioForCall()
            ContextCompat.startForegroundService(appContext, Intent(appContext, CallService::class.java))

            recorder.start()
            val speechToText = createSpeechToTextOrNull()
            stt = speechToText
            _state.update {
                it.copy(
                    isRunning = true,
                    liveCaption = if (speechToText == null) {
                        "Listening. Add a Vosk model folder to filesDir/vosk-en or filesDir/vosk-hi for captions."
                    } else {
                        "Listening..."
                    },
                    modelStatus = GemmaEngine.status,
                )
            }

            if (speechToText != null) {
                transcriptJob = viewModelScope.launch {
                    speechToText.transcribeStream(recorder.audio()).collect { transcript ->
                        _state.update {
                            it.copy(
                                liveCaption = transcript.text,
                                transcript = it.transcript + CaptionLine(
                                    text = transcript.text,
                                    isFinal = transcript.isFinal,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    fun stopCallPipe() {
        transcriptJob?.cancel()
        transcriptJob = null
        startJob?.cancel()
        startJob = null
        recorder.stop()
        stt?.close()
        stt = null
        appContext.stopService(Intent(appContext, CallService::class.java))
        _state.update { it.copy(isRunning = false, liveCaption = "Ready") }
    }

    fun onDraftChange(value: String) {
        _state.update { it.copy(draftReply = value) }
    }

    fun sendReply() {
        val text = _state.value.draftReply.trim()
        if (text.isBlank()) return
        _state.update { it.copy(draftReply = "") }
        viewModelScope.launch {
            routeAudioForCall()
            tts.speak(text, detectLanguage(text))
        }
    }

    fun testGemma() {
        viewModelScope.launch {
            _state.update { it.copy(modelStatus = "Loading Gemma...") }
            runCatching {
                withContext(Dispatchers.Default) {
                    GemmaEngine.init(appContext)
                }
                if (!GemmaEngine.isReady) {
                    error(GemmaEngine.status)
                }
                _state.update { it.copy(modelStatus = "Gemma test running...") }
                GemmaEngine.generate("Translate to Hindi: I am running late.")
            }.onSuccess { response ->
                _state.update { it.copy(modelStatus = "Gemma response: $response") }
            }.onFailure { error ->
                _state.update { it.copy(modelStatus = "Gemma test failed: ${error.message}") }
            }
        }
    }

    override fun onCleared() {
        stopCallPipe()
        tts.close()
        super.onCleared()
    }

    private fun routeAudioForCall() {
        @Suppress("DEPRECATION")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = true
    }

    private fun createSpeechToTextOrNull(): SpeechToText? {
        val en = File(appContext.filesDir, "vosk-en")
        val hi = File(appContext.filesDir, "vosk-hi")
        val modelDir = when {
            en.exists() -> en
            hi.exists() -> hi
            else -> return null
        }
        return runCatching { SpeechToText(modelDir.absolutePath) }.getOrNull()
    }

    private fun detectLanguage(text: String): String {
        return if (text.any { it in '\u0900'..'\u097F' }) "hi-IN" else "en-IN"
    }
}

data class CallState(
    val isRunning: Boolean = false,
    val liveCaption: String = "Ready",
    val draftReply: String = "",
    val transcript: List<CaptionLine> = emptyList(),
    val modelStatus: String = "Gemma not loaded",
)

data class CaptionLine(
    val text: String,
    val isFinal: Boolean,
    val id: Long = System.nanoTime(),
)
