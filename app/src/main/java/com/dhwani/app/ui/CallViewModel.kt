package com.dhwani.app.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dhwani.app.audio.SpeakerphoneRecorder
import com.dhwani.app.audio.SpeechToText
import com.dhwani.app.audio.TextToSpeechEngine
import com.dhwani.app.audio.VoskModelManager
import com.dhwani.app.call.CallService
import com.dhwani.app.data.CallSummary
import com.dhwani.app.data.CallLogStore
import com.dhwani.app.data.UserContext
import com.dhwani.app.data.UserContextStore
import com.dhwani.app.llm.GemmaEngine
import com.dhwani.app.llm.CallAssistant
import com.dhwani.app.llm.AssistantToolDispatcher
import com.dhwani.app.sign.SignCandidate
import com.dhwani.app.sign.SignPhrase
import com.dhwani.app.sign.SignRecognition
import com.dhwani.app.sign.SignVocabulary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

class CallViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val recorder = SpeakerphoneRecorder()
    private val tts = TextToSpeechEngine(appContext)
    private val contextStore = UserContextStore(appContext)
    private val callLogStore = CallLogStore(appContext)
    private var stt: SpeechToText? = null
    private var transcriptJob: Job? = null
    private var startJob: Job? = null
    private var suggestionJob: Job? = null
    private var briefingJob: Job? = null
    private var summaryJob: Job? = null
    private var speechJob: Job? = null
    private var lastSuggestionAtMs: Long = 0
    private var lastSuggestionUtterance: String = ""
    private var callGeneration: Long = 0L
    private var callStartedAtMs: Long = 0L
    private var previousAudioMode: Int? = null
    private var previousSpeakerphoneState: Boolean? = null

    private val _state = MutableStateFlow(
        CallState(
            modelStatus = GemmaEngine.status,
            userContext = contextStore.load(),
            recentSummaries = formatRecentSummaries(),
            recentCallSummaries = callLogStore.loadRecent(10),
            signPhrases = SignVocabulary.demoPhrases,
        ),
    )
    val state: StateFlow<CallState> = _state.asStateFlow()

    fun startCallPipe() {
        if (_state.value.isRunning ||
            _state.value.isStarting ||
            _state.value.isSpeaking ||
            startJob?.isActive == true
        ) return
        val generation = ++callGeneration
        startJob = viewModelScope.launch {
            _state.update { it.copy(isStarting = true, liveCaption = "Preparing speech model...") }
            try {
                withContext(Dispatchers.IO) {
                    VoskModelManager.prepareModels(appContext)
                }

                routeAudioForCall()
                ContextCompat.startForegroundService(appContext, Intent(appContext, CallService::class.java))

                recorder.start()
                val speechToText = createSpeechToTextOrNull()
                    ?: error("The ${_state.value.captionLanguage.label} speech model is missing or incomplete")
                stt = speechToText
                _state.update {
                    it.copy(
                        isRunning = true,
                        isStarting = false,
                        liveCaption = "Listening for ${it.captionLanguage.label}...",
                        modelStatus = GemmaEngine.status,
                    )
                }
                callStartedAtMs = System.currentTimeMillis()

                transcriptJob = viewModelScope.launch {
                    speechToText.transcribeStream(recorder.audio()).collect { transcript ->
                        _state.update {
                            it.copy(
                                liveCaption = transcript.text,
                                transcript = mergeCallerTranscript(it.transcript, transcript),
                            )
                        }
                        if (transcript.isFinal && GemmaEngine.isReady) {
                            requestSmartReplies(transcript.text, force = false)
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                if (generation == callGeneration) releaseCallResources()
                throw cancelled
            } catch (error: Throwable) {
                if (generation == callGeneration) {
                    releaseCallResources()
                    _state.update {
                        it.copy(
                            isRunning = false,
                            isStarting = false,
                            liveCaption = "Could not start captions: ${error.message ?: error.javaClass.simpleName}",
                        )
                    }
                }
            }
        }
    }

    fun stopCallPipe() {
        stopCallPipe(saveSummary = true)
    }

    private fun stopCallPipe(saveSummary: Boolean) {
        val transcriptToSummarize = _state.value.transcript
            .filter { it.isFinal || it.speaker == Speaker.User }
            .takeLast(30)
            .joinToString("\n") { "${it.speaker.label}: ${it.text}" }
        val durationSec = if (callStartedAtMs > 0L) {
            ((System.currentTimeMillis() - callStartedAtMs) / 1000L).toInt()
        } else {
            0
        }
        callStartedAtMs = 0L

        callGeneration += 1L
        startJob?.cancel()
        startJob = null
        releaseCallResources()
        _state.update {
            it.copy(
                isRunning = false,
                isStarting = false,
                liveCaption = "Ready",
                draftReply = "",
                transcript = emptyList(),
                suggestions = emptyList(),
                isSuggesting = false,
                isSpeaking = false,
            )
        }

        if (saveSummary && transcriptToSummarize.isNotBlank() && GemmaEngine.isReady) {
            summarizeStoppedCall(transcriptToSummarize, durationSec)
        }
    }

    fun onDraftChange(value: String) {
        _state.update { it.copy(draftReply = value) }
    }

    fun sendReply() {
        val text = _state.value.draftReply.trim()
        if (text.isBlank() || _state.value.isSpeaking) return
        appendUserLine(text)
        _state.update { it.copy(draftReply = "") }
        speakText(text)
    }

    fun speakSuggestion(suggestion: String) {
        if (_state.value.isSpeaking) return
        appendUserLine(suggestion)
        speakText(suggestion)
    }

    fun useSuggestion(suggestion: String) {
        _state.update { it.copy(draftReply = suggestion) }
    }

    fun toggleSignPanel() {
        _state.update {
            val opening = !it.isSignPanelOpen
            it.copy(
                isSignPanelOpen = opening,
                isSignCapturing = if (opening) it.isSignCapturing else false,
                isSignRecognizerReady = false,
            )
        }
    }

    fun selectSection(section: AppSection) {
        _state.update { it.copy(selectedSection = section) }
    }

    fun startSignCapture() {
        if (_state.value.isSignCapturing || _state.value.isSignTranslating) return
        if (!_state.value.isSignRecognizerReady) {
            _state.update { it.copy(signStatus = "Getting the sign recognizer ready...") }
            return
        }
        _state.update {
            it.copy(
                isSignCapturing = true,
                isSignTranslating = false,
                signCaptureRequestId = it.signCaptureRequestId + 1L,
                signStatus = "Starting capture...",
                signCandidates = emptyList(),
                selectedSignGloss = "",
                signSentence = "",
            )
        }
    }

    fun onSignRecognizerReady() {
        if (!_state.value.isSignPanelOpen) return
        _state.update {
            it.copy(
                isSignRecognizerReady = true,
                signStatus = if (it.isSignCapturing) it.signStatus else "Ready to recognize",
            )
        }
    }

    fun onSignCaptureStatus(message: String) {
        if (!_state.value.isSignCapturing) return
        _state.update { it.copy(signStatus = message) }
    }

    fun onSignRecognition(recognition: SignRecognition) {
        if (!_state.value.isSignCapturing) return
        val top = recognition.top
        val qualityLine =
            "Detected movement → ${top.gloss}. Tap it to confirm, or Speak after it fills the reply."
        _state.update {
            it.copy(
                isSignCapturing = false,
                signCandidates = recognition.candidates,
                signStatus = qualityLine,
            )
        }
        // Auto-select hardcoded BYE so the reply sentence is ready immediately.
        if (top.gloss.equals("BYE", ignoreCase = true)) {
            translateSignGloss(top.gloss)
        }
    }

    fun onSignCaptureError(message: String) {
        _state.update {
            it.copy(
                isSignCapturing = false,
                signStatus = message,
            )
        }
    }

    fun selectSignCandidate(candidate: SignCandidate) {
        translateSignGloss(candidate.gloss)
    }

    fun selectSignPhrase(phrase: SignPhrase) {
        translateSignGloss(phrase.gloss)
    }

    fun useSignedSentence() {
        val sentence = _state.value.signSentence.trim()
        if (sentence.isBlank()) return
        _state.update {
            it.copy(
                draftReply = sentence,
                isSignPanelOpen = false,
                signStatus = "",
            )
        }
    }

    fun speakSignedSentence() {
        val sentence = _state.value.signSentence.trim()
        if (sentence.isBlank() || _state.value.isSpeaking) return
        appendUserLine(sentence)
        speakText(sentence)
    }

    fun selectCaptionLanguage(language: CaptionLanguage) {
        if (_state.value.isRunning || _state.value.isStarting) return
        _state.update { it.copy(captionLanguage = language) }
    }

    fun toggleContextEditor() {
        _state.update { it.copy(isContextEditorOpen = !it.isContextEditorOpen) }
    }

    fun toggleBriefingPanel() {
        _state.update { it.copy(isBriefingPanelOpen = !it.isBriefingPanelOpen) }
    }

    fun refreshSmartReplies() {
        val latest = _state.value.transcript.lastOrNull {
            it.isFinal && it.speaker == Speaker.Caller
        }?.text
            ?: _state.value.liveCaption.takeIf { it != "Ready" }
            ?: return
        requestSmartReplies(latest, force = true)
    }

    fun onContextChange(userContext: UserContext) {
        _state.update { it.copy(userContext = userContext) }
    }

    fun onSetupNameChange(value: String) {
        _state.update { it.copy(userContext = it.userContext.copyFromSetupFields(name = value)) }
    }

    fun onSetupLanguageChange(value: String) {
        _state.update { it.copy(userContext = it.userContext.copyFromSetupFields(preferredLanguage = value)) }
    }

    fun onSetupHomeAddressChange(value: String) {
        _state.update { it.copy(userContext = it.userContext.copyFromSetupFields(homeAddress = value)) }
    }

    fun onSetupVoiceAddressChange(value: String) {
        _state.update { it.copy(userContext = it.userContext.copyFromSetupFields(voiceFriendlyAddress = value)) }
    }

    fun onSetupPeopleChange(value: String) {
        _state.update { it.copy(userContext = it.userContext.copyFromSetupFields(importantPeople = value)) }
    }

    fun onSetupMedicalChange(value: String) {
        _state.update { it.copy(userContext = it.userContext.copyFromSetupFields(medicalNotes = value)) }
    }

    fun onSetupPaymentChange(value: String) {
        _state.update { it.copy(userContext = it.userContext.copyFromSetupFields(paymentHint = value)) }
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

    fun onCallPhoneNumberChange(value: String) {
        _state.update { it.copy(callPhoneNumber = value) }
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
                generateWithLocalTools(
                    initialPrompt = { dispatcher ->
                        CallAssistant.briefingPrompt(
                            context = _state.value.userContext,
                            goal = goal,
                            recentSummaries = _state.value.recentSummaries,
                            toolContext = dispatcher.availableContext(),
                        )
                    },
                    promptWithToolResults = { dispatcher, toolResults ->
                        CallAssistant.briefingPrompt(
                            context = _state.value.userContext,
                            goal = goal,
                            recentSummaries = _state.value.recentSummaries,
                            toolContext = dispatcher.availableContext(),
                            toolResults = toolResults,
                        )
                    },
                )
            }.onSuccess { briefing ->
                _state.update {
                    it.copy(
                        isBriefingLoading = false,
                        briefing = AssistantToolDispatcher.removeToolBlocks(briefing).trim(),
                        callPhoneNumber = it.callPhoneNumber.ifBlank { inferPhoneNumber(goal, it.userContext) },
                    )
                }
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

    fun toggleCallHistory() {
        _state.update { it.copy(isCallHistoryOpen = !it.isCallHistoryOpen) }
    }

    fun clearCallHistory() {
        callLogStore.clear()
        _state.update {
            it.copy(
                recentSummaries = "",
                recentCallSummaries = emptyList(),
                summaryStatus = "Call history cleared",
            )
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
        stopCallPipe(saveSummary = false)
        tts.close()
        super.onCleared()
    }

    private fun requestSmartReplies(latestUtterance: String, force: Boolean) {
        val cleanUtterance = latestUtterance.trim()
        if (cleanUtterance.isBlank()) return

        val now = System.currentTimeMillis()
        if (!force) {
            val isDuplicate = cleanUtterance.equals(lastSuggestionUtterance, ignoreCase = true)
            val isCoolingDown = now - lastSuggestionAtMs < SMART_REPLY_COOLDOWN_MS
            if (isDuplicate || isCoolingDown || suggestionJob?.isActive == true) return
        }

        lastSuggestionAtMs = now
        lastSuggestionUtterance = cleanUtterance
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            _state.update { it.copy(isSuggesting = true) }
            runCatching {
                ensureGemmaReady()
                val current = _state.value
                val rollingTranscript = current.transcript
                    .takeLast(8)
                    .joinToString("\n") { line ->
                        val suffix = if (line.isFinal || line.speaker == Speaker.User) "" else " partial"
                        "${line.speaker.label}$suffix: ${line.text}"
                    }
                val prompt = CallAssistant.smartReplyPrompt(
                    context = current.userContext,
                    rollingTranscript = rollingTranscript,
                    latestUtterance = cleanUtterance,
                    recentSummaries = current.recentSummaries,
                    toolContext = AssistantToolDispatcher(current.userContext, callLogStore).availableContext(),
                )
                val response = generateWithLocalTools(
                    initialPrompt = { prompt },
                    promptWithToolResults = { dispatcher, toolResults ->
                        CallAssistant.smartReplyPrompt(
                            context = _state.value.userContext,
                            rollingTranscript = rollingTranscript,
                            latestUtterance = cleanUtterance,
                            recentSummaries = _state.value.recentSummaries,
                            toolContext = dispatcher.availableContext(),
                            toolResults = toolResults,
                        )
                    },
                )
                CallAssistant.parseSuggestions(response)
            }.onSuccess { suggestions ->
                _state.update { it.copy(isSuggesting = false, suggestions = suggestions) }
            }.onFailure {
                _state.update {
                    it.copy(
                        isSuggesting = false,
                        suggestions = CallAssistant.fallbackSuggestions(),
                    )
                }
            }
        }
    }

    private fun appendUserLine(text: String) {
        _state.update {
            it.copy(
                transcript = it.transcript + CaptionLine(
                    text = text,
                    isFinal = true,
                    speaker = Speaker.User,
                ),
            )
        }
    }

    private fun summarizeStoppedCall(transcript: String, durationSec: Int) {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            _state.update { it.copy(summaryStatus = "Summarizing call...") }
            runCatching {
                ensureGemmaReady()
                GemmaEngine.generate(CallAssistant.summaryPrompt(transcript))
            }.onSuccess { summary ->
                callLogStore.add(
                    summary = summary,
                    transcript = transcript,
                    direction = "manual",
                    contactName = inferContactName(_state.value.callGoal, _state.value.userContext),
                    durationSec = durationSec,
                )
                _state.update {
                    it.copy(
                        summaryStatus = "Call summary saved",
                        recentSummaries = formatRecentSummaries(),
                        recentCallSummaries = callLogStore.loadRecent(10),
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(summaryStatus = "Summary unavailable: ${error.message}") }
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

    private suspend fun generateWithLocalTools(
        initialPrompt: (AssistantToolDispatcher) -> String,
        promptWithToolResults: (AssistantToolDispatcher, String) -> String,
    ): String {
        val dispatcher = AssistantToolDispatcher(_state.value.userContext, callLogStore)
        var response = GemmaEngine.generate(initialPrompt(dispatcher))
        repeat(MAX_TOOL_ROUNDS) {
            val toolCalls = AssistantToolDispatcher.extractToolCalls(response)
            if (toolCalls.isEmpty()) return response

            val toolResults = toolCalls.map(dispatcher::dispatch)
            response = GemmaEngine.generate(
                promptWithToolResults(
                    dispatcher,
                    AssistantToolDispatcher.formatToolResults(toolResults),
                ),
            )
        }
        return AssistantToolDispatcher.removeToolBlocks(response)
    }

    private fun routeAudioForCall() {
        if (previousAudioMode == null) previousAudioMode = audioManager.mode
        if (previousSpeakerphoneState == null) {
            @Suppress("DEPRECATION")
            previousSpeakerphoneState = audioManager.isSpeakerphoneOn
        }
        @Suppress("DEPRECATION")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = true
    }

    private fun createSpeechToTextOrNull(): SpeechToText? {
        val modelDir = File(appContext.filesDir, _state.value.captionLanguage.modelDirectory)
        if (!VoskModelManager.isUsableModel(modelDir)) return null
        return runCatching { SpeechToText(modelDir.absolutePath) }.getOrNull()
    }

    private fun speakText(text: String) {
        _state.update { it.copy(isSpeaking = true, replyStatus = "Speaking...") }
        speechJob = viewModelScope.launch {
            val restoreAfterSpeaking = !_state.value.isRunning
            try {
                routeAudioForCall()
                tts.speak(text, detectLanguage(text))
                _state.update { it.copy(isSpeaking = false, replyStatus = "Reply spoken") }
            } catch (cancelled: CancellationException) {
                _state.update { it.copy(isSpeaking = false) }
                throw cancelled
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        isSpeaking = false,
                        replyStatus = "Could not speak: ${error.message ?: error.javaClass.simpleName}",
                    )
                }
            } finally {
                if (restoreAfterSpeaking) restoreAudioRoute()
            }
        }
    }

    fun placeOutgoingCall() {
        val phoneNumber = _state.value.callPhoneNumber.trim()
        if (phoneNumber.isBlank()) {
            _state.update {
                it.copy(briefing = appendStatus(it.briefing, "Add a phone number before placing the call."))
            }
            return
        }

        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber)}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            appContext.startActivity(intent)
        }.onSuccess {
            _state.update {
                it.copy(
                    selectedSection = AppSection.LIVE,
                    liveCaption = "When the call connects, tap Start captions.",
                )
            }
        }.onFailure { error ->
            _state.update {
                it.copy(briefing = appendStatus(it.briefing, "Could not open dialer: ${error.message}"))
            }
        }
    }

    private fun releaseCallResources() {
        val activeTranscriptJob = transcriptJob
        val activeStt = stt
        if (activeTranscriptJob != null) {
            activeTranscriptJob.invokeOnCompletion { activeStt?.close() }
            activeTranscriptJob.cancel()
        } else {
            activeStt?.close()
        }
        transcriptJob = null
        suggestionJob?.cancel()
        suggestionJob = null
        speechJob?.cancel()
        speechJob = null
        recorder.stop()
        stt = null
        appContext.stopService(Intent(appContext, CallService::class.java))
        restoreAudioRoute()
    }

    private fun restoreAudioRoute() {
        @Suppress("DEPRECATION")
        previousSpeakerphoneState?.let { audioManager.isSpeakerphoneOn = it }
        previousAudioMode?.let { audioManager.mode = it }
        previousSpeakerphoneState = null
        previousAudioMode = null
    }

    private fun detectLanguage(text: String): String {
        return if (text.any { it in '\u0900'..'\u097F' }) "hi-IN" else "en-IN"
    }

    private fun formatRecentSummaries(): String {
        return callLogStore.loadRecent()
            .joinToString("\n") { "- ${it.summary}" }
    }

    private fun inferPhoneNumber(goal: String, context: UserContext): String {
        val goalWords = goal.lowercase()
        val contact = context.contacts.firstOrNull { candidate ->
            goalWords.contains(candidate.name.lowercase()) ||
                goalWords.contains(candidate.role.lowercase()) ||
                candidate.notes.lowercase().split(Regex("\\W+")).any { it.length > 2 && goalWords.contains(it) }
        } ?: context.contacts.firstOrNull()
        return contact?.phoneNumbers?.firstOrNull().orEmpty()
    }

    private fun inferContactName(goal: String, context: UserContext): String {
        val goalWords = goal.lowercase()
        return context.contacts.firstOrNull { candidate ->
            goalWords.contains(candidate.name.lowercase()) ||
                goalWords.contains(candidate.role.lowercase())
        }?.name.orEmpty()
    }

    private fun appendStatus(existing: String, status: String): String {
        return listOf(existing, status)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    private fun translateSignGloss(gloss: String) {
        val cleanGloss = gloss.trim()
        if (cleanGloss.isBlank()) return
        val current = _state.value
        val sentence = SignVocabulary.fallbackSentence(
            gloss = cleanGloss,
            languageLabel = current.captionLanguage.label,
            context = current.userContext,
        )
        _state.update {
            it.copy(
                selectedSignGloss = cleanGloss,
                isSignTranslating = false,
                signSentence = sentence,
                signStatus = "Reply ready",
            )
        }
    }

    companion object {
        private const val SMART_REPLY_COOLDOWN_MS = 2_500L
        private const val MAX_TOOL_ROUNDS = 2
        // Softmax over 263 classes rarely peaks this high on bad inputs; use as "confident" UX.
        private const val SIGN_SUGGESTION_THRESHOLD = 0.35f
        private const val SIGN_MARGIN_THRESHOLD = 0.08f
    }
}

data class CallState(
    val selectedSection: AppSection = AppSection.LIVE,
    val isRunning: Boolean = false,
    val isStarting: Boolean = false,
    val liveCaption: String = "Ready",
    val draftReply: String = "",
    val transcript: List<CaptionLine> = emptyList(),
    val captionLanguage: CaptionLanguage = CaptionLanguage.ENGLISH,
    val isSpeaking: Boolean = false,
    val replyStatus: String = "",
    val modelStatus: String = "Gemma not loaded",
    val userContext: UserContext = UserContext(),
    val isContextEditorOpen: Boolean = false,
    val contextMessage: String = "",
    val suggestions: List<String> = emptyList(),
    val isSuggesting: Boolean = false,
    val callGoal: String = "",
    val briefing: String = "",
    val isBriefingPanelOpen: Boolean = true,
    val isBriefingLoading: Boolean = false,
    val callPhoneNumber: String = "",
    val recentSummaries: String = "",
    val recentCallSummaries: List<CallSummary> = emptyList(),
    val isCallHistoryOpen: Boolean = false,
    val summaryStatus: String = "",
    val isSignPanelOpen: Boolean = false,
    val isSignRecognizerReady: Boolean = false,
    val isSignCapturing: Boolean = false,
    val signCaptureRequestId: Long = 0L,
    val isSignTranslating: Boolean = false,
    val signStatus: String = "",
    val selectedSignGloss: String = "",
    val signSentence: String = "",
    val signCandidates: List<SignCandidate> = emptyList(),
    val signPhrases: List<SignPhrase> = emptyList(),
)

enum class AppSection(val label: String) {
    LIVE("Live"),
    CALL("Call"),
    YOU("You"),
}

data class CaptionLine(
    val text: String,
    val isFinal: Boolean,
    val speaker: Speaker,
    val id: Long = System.nanoTime(),
)

enum class Speaker(val label: String) {
    Caller("Caller"),
    User("You"),
}

enum class CaptionLanguage(
    val label: String,
    val modelDirectory: String,
) {
    ENGLISH("English", "vosk-en"),
    HINDI("Hindi", "vosk-hi"),
}

internal fun mergeCallerTranscript(
    lines: List<CaptionLine>,
    transcript: SpeechToText.Transcript,
): List<CaptionLine> {
    val partialIndex = lines.indexOfLast { it.speaker == Speaker.Caller && !it.isFinal }
    if (partialIndex < 0) {
        return lines + CaptionLine(
            text = transcript.text,
            isFinal = transcript.isFinal,
            speaker = Speaker.Caller,
        )
    }

    return lines.toMutableList().apply {
        this[partialIndex] = this[partialIndex].copy(
            text = transcript.text,
            isFinal = transcript.isFinal,
        )
    }
}
