package com.dhwani.app.ui

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dhwani.app.data.CallSummary
import com.dhwani.app.data.UserContext
import com.dhwani.app.sign.SignCandidate
import com.dhwani.app.sign.SignFrameAnalyzer
import com.dhwani.app.sign.SignPhrase
import com.dhwani.app.sign.SignRecognition
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@Composable
fun CallScreen(vm: CallViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DhwaniHeader(
                selected = state.selectedSection,
                onSelect = vm::selectSection,
            )
        },
    ) { padding ->
        when (state.selectedSection) {
            AppSection.LIVE -> LiveCallContent(
                state = state,
                vm = vm,
                modifier = Modifier.padding(padding),
            )

            AppSection.CALL -> PlanCallContent(
                state = state,
                vm = vm,
                modifier = Modifier.padding(padding),
            )

            AppSection.YOU -> ProfileContent(
                state = state,
                vm = vm,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun DhwaniHeader(
    selected: AppSection,
    onSelect: (AppSection) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Text(
                text = "Dhwani",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
            TabRow(
                selectedTabIndex = selected.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
            ) {
                AppSection.entries.forEach { section ->
                    Tab(
                        selected = section == selected,
                        onClick = { onSelect(section) },
                        text = { Text(section.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveCallContent(
    state: CallState,
    vm: CallViewModel,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val signItemIndex = 4 +
        (if (state.transcript.isNotEmpty()) 1 else 0) +
        (if (state.isSuggesting || state.suggestions.isNotEmpty()) 1 else 0)

    LaunchedEffect(state.isSignPanelOpen) {
        if (state.isSignPanelOpen) listState.animateScrollToItem(signItemIndex)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.isRunning) "Captions are on" else "Live call",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (state.isRunning) "Listening in ${state.captionLanguage.label}" else "Use after putting the call on speaker",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CaptionLanguage.entries.forEach { language ->
                        FilterChip(
                            selected = state.captionLanguage == language,
                            onClick = { vm.selectCaptionLanguage(language) },
                            enabled = !state.isRunning && !state.isStarting,
                            label = { Text(if (language == CaptionLanguage.ENGLISH) "EN" else "HI") },
                        )
                    }
                }
            }
        }

        item {
            if (state.isRunning) {
                OutlinedButton(
                    onClick = vm::stopCallPipe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Text("End session")
                }
            } else {
                Button(
                    onClick = vm::startCallPipe,
                    enabled = !state.isStarting && !state.isSpeaking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    if (state.isStarting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(if (state.isStarting) "Getting ready" else "Start captions")
                }
            }
        }

        item {
            CaptionPanel(state.liveCaption)
        }

        if (state.transcript.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Conversation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    state.transcript.takeLast(6).forEach { line ->
                        TranscriptLine(line)
                    }
                }
            }
        }

        if (state.isSuggesting || state.suggestions.isNotEmpty()) {
            item {
                QuickReplies(
                    state = state,
                    onRefresh = vm::refreshSmartReplies,
                    onSpeak = vm::speakSuggestion,
                )
            }
        }

        item {
            ReplyComposer(
                state = state,
                onDraftChange = vm::onDraftChange,
                onSpeak = vm::sendReply,
                onSign = vm::toggleSignPanel,
            )
        }

        if (state.isSignPanelOpen) {
            item {
                SignInputSection(
                    state = state,
                    onClose = vm::toggleSignPanel,
                    onCapture = vm::startSignCapture,
                    onRecognizerReady = vm::onSignRecognizerReady,
                    onCaptureStatus = vm::onSignCaptureStatus,
                    onRecognition = vm::onSignRecognition,
                    onCaptureError = vm::onSignCaptureError,
                    onSelectCandidate = vm::selectSignCandidate,
                    onSelectPhrase = vm::selectSignPhrase,
                    onUseSentence = vm::useSignedSentence,
                    onSpeakSentence = vm::speakSignedSentence,
                )
            }
        }

        if (state.replyStatus.isNotBlank() || state.summaryStatus.isNotBlank()) {
            item {
                Text(
                    text = listOf(state.replyStatus, state.summaryStatus)
                        .filter(String::isNotBlank)
                        .joinToString("  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CaptionPanel(caption: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 170.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Caller",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (caption == "Ready") "Caller speech will appear here." else caption,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Composable
private fun TranscriptLine(line: CaptionLine) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (line.speaker == Speaker.User) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (line.speaker == Speaker.User) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(0.88f),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = line.speaker.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (line.isFinal) line.text else "${line.text}...",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickReplies(
    state: CallState,
    onRefresh: () -> Unit,
    onSpeak: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Quick replies",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (state.isSuggesting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onRefresh) { Text("Refresh") }
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.suggestions.forEach { suggestion ->
                FilterChip(
                    selected = false,
                    onClick = { onSpeak(suggestion) },
                    enabled = !state.isSpeaking,
                    label = { Text(suggestion) },
                )
            }
        }
    }
}

@Composable
private fun ReplyComposer(
    state: CallState,
    onDraftChange: (String) -> Unit,
    onSpeak: () -> Unit,
    onSign: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.draftReply,
            onValueChange = onDraftChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Type a reply") },
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onSign,
                modifier = Modifier.weight(1f),
            ) {
                Text("Sign reply")
            }
            Button(
                onClick = onSpeak,
                enabled = state.draftReply.isNotBlank() && !state.isSpeaking,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (state.isSpeaking) "Speaking" else "Speak reply")
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
private fun SignInputSection(
    state: CallState,
    onClose: () -> Unit,
    onCapture: () -> Unit,
    onRecognizerReady: () -> Unit,
    onCaptureStatus: (String) -> Unit,
    onRecognition: (SignRecognition) -> Unit,
    onCaptureError: (String) -> Unit,
    onSelectCandidate: (SignCandidate) -> Unit,
    onSelectPhrase: (SignPhrase) -> Unit,
    onUseSentence: () -> Unit,
    onSpeakSentence: () -> Unit,
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var showCallPhrases by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sign reply",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Keep both shoulders, one elbow, and your signing hand visible",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onClose) { Text("Close") }
        }

        if (!cameraPermission.status.isGranted) {
            Text(
                text = "Allow the camera to recognize a sign. It stays on this phone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = cameraPermission::launchPermissionRequest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Allow camera")
            }
            return@Column
        }

        if (!showCallPhrases) {
            SignCameraPreview(
                captureRequestId = state.signCaptureRequestId,
                onReady = onRecognizerReady,
                onStatus = onCaptureStatus,
                onResult = onRecognition,
                onError = onCaptureError,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(8.dp)),
            )

            Button(
                onClick = onCapture,
                enabled = state.isSignRecognizerReady &&
                    !state.isSignCapturing &&
                    !state.isSignTranslating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.isSignCapturing || !state.isSignRecognizerReady) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    when {
                        !state.isSignRecognizerReady -> "Getting recognizer ready"
                        state.isSignCapturing -> "Keep signing"
                        else -> "Recognize my sign"
                    },
                )
            }

            if (state.signStatus.isNotBlank()) {
                Text(
                    text = state.signStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!state.isSignCapturing && state.signCandidates.isNotEmpty()) {
                Text(
                    text = "Choose the closest result",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.signCandidates.forEach { candidate ->
                        FilterChip(
                            selected = state.selectedSignGloss == candidate.gloss,
                            onClick = { onSelectCandidate(candidate) },
                            enabled = !state.isSignTranslating,
                            label = {
                                Text(
                                    "${candidate.gloss.lowercase().replaceFirstChar { it.titlecase() }} " +
                                        "${(candidate.confidence * 100).roundToInt()}%",
                                )
                            },
                        )
                    }
                }
            }
        }

        if (!state.isSignCapturing) {
            OutlinedButton(
                onClick = { showCallPhrases = !showCallPhrases },
                enabled = !state.isSignTranslating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (showCallPhrases) "Hide call phrases" else "Choose a call phrase instead")
            }
            if (showCallPhrases) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.signPhrases.forEach { phrase ->
                        FilterChip(
                            selected = state.selectedSignGloss == phrase.gloss,
                            onClick = { onSelectPhrase(phrase) },
                            enabled = !state.isSignTranslating,
                            label = {
                                Text(phrase.gloss.lowercase().replaceFirstChar { it.titlecase() })
                            },
                        )
                    }
                }
            }
        }

        if (state.signSentence.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = state.signSentence,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(14.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onUseSentence,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Edit first")
                }
                Button(
                    onClick = onSpeakSentence,
                    enabled = !state.isSpeaking,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Speak now")
                }
            }
        }
    }
}

@Composable
private fun SignCameraPreview(
    captureRequestId: Long,
    onReady: () -> Unit,
    onStatus: (String) -> Unit,
    onResult: (SignRecognition) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val analyzer = remember(context) {
        SignFrameAnalyzer(
            context = context.applicationContext,
            onReady = onReady,
            onStatus = onStatus,
            onResult = onResult,
            onError = onError,
        )
    }

    LaunchedEffect(captureRequestId) {
        if (captureRequestId > 0L) analyzer.startCapture()
    }

    DisposableEffect(lifecycleOwner, previewView, analyzer) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraExecutor = Executors.newSingleThreadExecutor()
        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .apply { setAnalyzer(cameraExecutor, analyzer) }
            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageAnalysis,
                )
            }.onFailure { onError(it.message ?: "Could not open the front camera") }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
            cameraExecutor.execute {
                analyzer.close()
                cameraExecutor.shutdown()
            }
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
private fun PlanCallContent(
    state: CallState,
    vm: CallViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Make a call",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = state.callGoal,
            onValueChange = vm::onCallGoalChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("What is this call about?") },
            minLines = 2,
            maxLines = 3,
        )
        OutlinedTextField(
            value = state.callPhoneNumber,
            onValueChange = vm::onCallPhoneNumberChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phone number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = vm::generateBriefing,
                enabled = !state.isBriefingLoading && state.callGoal.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text(if (state.isBriefingLoading) "Preparing" else "Prepare")
            }
            Button(
                onClick = vm::placeOutgoingCall,
                enabled = state.callPhoneNumber.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Open dialer")
            }
        }

        if (state.isBriefingLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (state.briefing.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = state.briefing,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        CallHistorySection(
            summaries = state.recentCallSummaries,
            isOpen = state.isCallHistoryOpen,
            onToggle = vm::toggleCallHistory,
            onClear = vm::clearCallHistory,
        )
    }
}

@Composable
private fun CallHistorySection(
    summaries: List<CallSummary>,
    isOpen: Boolean,
    onToggle: () -> Unit,
    onClear: () -> Unit,
) {
    HorizontalDivider()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Recent calls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (summaries.isEmpty()) "No saved summaries" else "${summaries.size} saved",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onToggle) { Text(if (isOpen) "Hide" else "Show") }
    }

    if (isOpen) {
        summaries.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider()
            Column(
                modifier = Modifier.padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(item.timestamp)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(item.summary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (summaries.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onClear) {
                    Text("Clear history")
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    state: CallState,
    vm: CallViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Your details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Saved privately on this phone for more useful replies.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BasicProfileFields(
            context = state.userContext,
            onNameChange = vm::onSetupNameChange,
            onLanguageChange = vm::onSetupLanguageChange,
            onHomeAddressChange = vm::onSetupHomeAddressChange,
        )
        TextButton(onClick = vm::toggleContextEditor) {
            Text(if (state.isContextEditorOpen) "Fewer details" else "Add more details")
        }
        if (state.isContextEditorOpen) {
            AdvancedProfileFields(
                context = state.userContext,
                onVoiceAddressChange = vm::onSetupVoiceAddressChange,
                onPeopleChange = vm::onSetupPeopleChange,
                onMedicalChange = vm::onSetupMedicalChange,
                onPaymentChange = vm::onSetupPaymentChange,
            )
        }
        Button(
            onClick = vm::saveContext,
            enabled = state.userContext.name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("Save details")
        }
        if (state.contextMessage.isNotBlank()) {
            Text(
                text = state.contextMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(top = 6.dp))
        Text(
            text = "On-device features",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = when {
                state.modelStatus.startsWith("Gemma unavailable") -> state.modelStatus
                state.modelStatus == "Gemma loaded" -> "Smart replies are ready"
                else -> "Smart replies load when first needed"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BasicProfileFields(
    context: UserContext,
    onNameChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onHomeAddressChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = context.name,
        onValueChange = onNameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Name") },
        singleLine = true,
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Reply language", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("English", "Hindi").forEach { language ->
                FilterChip(
                    selected = context.preferredLanguage.equals(language, ignoreCase = true),
                    onClick = { onLanguageChange(language) },
                    label = { Text(language) },
                )
            }
        }
    }
    OutlinedTextField(
        value = context.homeAddress,
        onValueChange = onHomeAddressChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Home address") },
        minLines = 2,
        maxLines = 3,
    )
}

@Composable
private fun AdvancedProfileFields(
    context: UserContext,
    onVoiceAddressChange: (String) -> Unit,
    onPeopleChange: (String) -> Unit,
    onMedicalChange: (String) -> Unit,
    onPaymentChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = context.voiceFriendlyAddress,
            onValueChange = onVoiceAddressChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Short address to say aloud") },
            maxLines = 2,
        )
        OutlinedTextField(
            value = context.importantPeople,
            onValueChange = onPeopleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Important people") },
            minLines = 2,
            maxLines = 4,
        )
        OutlinedTextField(
            value = context.medicalNotes,
            onValueChange = onMedicalChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Medical notes") },
            minLines = 2,
            maxLines = 4,
        )
        OutlinedTextField(
            value = context.paymentHint,
            onValueChange = onPaymentChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Safe payment detail") },
            maxLines = 2,
        )
    }
}
