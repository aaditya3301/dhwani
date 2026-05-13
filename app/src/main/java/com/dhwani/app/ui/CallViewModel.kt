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
import com.dhwani.app.data.UserContext
import com.dhwani.app.data.UserContextStore
import com.dhwani.app.llm.GemmaEngine
import com.dhwani.app.llm.Phase2Assistant
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
    private val contextStore = UserContextStore(appContext)
    private var stt: SpeechToText? = null
    private var transcriptJob: Job? = null
    private var startJob: Job? = null
    private var suggestionJob: Job? = null
    private var briefingJob: Job? = null

    private val _state = MutableStateFlow(
        CallState(
            modelStatus = GemmaEngine.status,
            userContext = contextStore.load(),
        ),
    )
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
                        if (transcript.isFinal) {
                            requestSmartReplies(transcript.text)
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
        suggestionJob?.cancel()
        suggestionJob = null
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

    fun speakSuggestion(suggestion: String) {
        viewModelScope.launch {
            routeAudioForCall()
            tts.speak(suggestion, detectLanguage(suggestion))
        }
    }

    fun useSuggestion(suggestion: String) {
        _state.update { it.copy(draftReply = suggestion) }
    }

    fun toggleContextEditor() {
        _state.update { it.copy(isContextEditorOpen = !it.isContextEditorOpen) }
    }

    fun onContextChange(userContext: UserContext) {
        _state.update { it.copy(userContext = userContext) }
    }

    fun saveContext() {
        val userContext = _state.value.userContext
        contextStore.save(userContext)
        _state.update {
            it.copy(
                userContext = userContext,
                contextMessage = if (userContext.isConfigured) "Context saved" else "Add your name to finish setup",
            )
        }
    }

    fun onCallGoalChange(value: String) {
        _state.update { it.copy(callGoal = value) }
    }

    fun generateBriefing() {
        val goal = _state.value.callGoal.trim()
        if (goal.isBlank()) {
            _state.update { it.copy(briefing = "Type a call goal first.") }
            return
        }

        briefingJob?.cancel()
        briefingJob = viewModelScope.launch {
            _state.update { it.copy(isBriefingLoading = true, briefing = "Preparing briefing...") }
            runCatching {
                ensureGemmaReady()
                GemmaEngine.generate(Phase2Assistant.briefingPrompt(_state.value.userContext, goal))
            }.onSuccess { briefing ->
                _state.update { it.copy(isBriefingLoading = false, briefing = briefing.trim()) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isBriefingLoading = false,
                        briefing = "Briefing unavailable: ${error.message}",
                    )
                }
            }
        }
    }

    fun testGemma() {
        viewModelScope.launch {
            _state.update { it.copy(modelStatus = "Loading Gemma...") }
            runCatching {
                ensureGemmaReady()
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

    private fun requestSmartReplies(latestUtterance: String) {
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            _state.update { it.copy(isSuggesting = true) }
            runCatching {
                ensureGemmaReady()
                val current = _state.value
                val rollingTranscript = current.transcript
                    .takeLast(8)
                    .joinToString("\n") { if (it.isFinal) "Caller: ${it.text}" else "Caller partial: ${it.text}" }
                val prompt = Phase2Assistant.smartReplyPrompt(
                    context = current.userContext,
                    rollingTranscript = rollingTranscript,
                    latestUtterance = latestUtterance,
                )
                Phase2Assistant.parseSuggestions(GemmaEngine.generate(prompt))
            }.onSuccess { suggestions ->
                _state.update { it.copy(isSuggesting = false, suggestions = suggestions) }
            }.onFailure {
                _state.update {
                    it.copy(
                        isSuggesting = false,
                        suggestions = Phase2Assistant.fallbackSuggestions(),
                    )
                }
            }
        }
    }

    private suspend fun ensureGemmaReady() {
        withContext(Dispatchers.Default) {
            GemmaEngine.init(appContext)
        }
        if (!GemmaEngine.isReady) {
            error(GemmaEngine.status)
        }
        _state.update { it.copy(modelStatus = GemmaEngine.status) }
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
    val userContext: UserContext = UserContext(),
    val isContextEditorOpen: Boolean = false,
    val contextMessage: String = "",
    val suggestions: List<String> = emptyList(),
    val isSuggesting: Boolean = false,
    val callGoal: String = "",
    val briefing: String = "",
    val isBriefingLoading: Boolean = false,
)

data class CaptionLine(
    val text: String,
    val isFinal: Boolean,
    val id: Long = System.nanoTime(),
)
