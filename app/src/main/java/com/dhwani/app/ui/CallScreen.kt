package com.dhwani.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dhwani.app.ui.theme.CallEndRedIcon
import com.dhwani.app.ui.theme.MicIcon
import com.dhwani.app.ui.theme.MicOffIcon
import com.dhwani.app.ui.theme.ShieldCheckIcon
import com.dhwani.app.ui.theme.VolumeUpIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallScreen(
    vm: CallViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by vm.state.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF9FBF9)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E2F23)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Live call",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E8540)
                        )
                    )

                    if (state.isRunning) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "00:01:24",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                color = Color(0xFF708A77),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF1E2F23)
                    )
                }
            }

            // Language Selector Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFEEF5F0))
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        val isEn = state.captionLanguage == CaptionLanguage.ENGLISH
                        val isHi = state.captionLanguage == CaptionLanguage.HINDI

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(50))
                                .background(if (isEn) Color(0xFF2E8540) else Color.Transparent)
                                .clickable { vm.selectCaptionLanguage(CaptionLanguage.ENGLISH) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "English",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEn) Color.White else Color(0xFF55665A)
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(50))
                                .background(if (isHi) Color(0xFF2E8540) else Color.Transparent)
                                .clickable { vm.selectCaptionLanguage(CaptionLanguage.HINDI) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hindi",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHi) Color.White else Color(0xFF55665A)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Content: Idle State vs Active State
            if (!state.isRunning) {
                LiveCallIdleContent(
                    onStartCaptions = vm::startCallPipe,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LiveCallActiveContent(
                    state = state,
                    vm = vm,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LiveCallIdleContent(
    onStartCaptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic Waveform Graphic
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedAudioWaveform(modifier = Modifier.padding(vertical = 24.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Use after putting the call on speaker",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF55665A)
                )
            )
        }

        // Start Captions Button
        Button(
            onClick = onStartCaptions,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2E8540),
                contentColor = Color.White
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MicIcon(size = 22.dp, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start captions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Bottom Privacy Shield Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFEEF3EE), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE4F3E8)),
                    contentAlignment = Alignment.Center
                ) {
                    ShieldCheckIcon(size = 22.dp, tint = Color(0xFF2E8540))
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "All processing is on-device",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E2F23)
                        )
                    )
                    Text(
                        text = "No data leaves your phone.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = Color(0xFF708A77)
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveCallActiveContent(
    state: CallState,
    vm: CallViewModel,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.transcript.size) {
        if (state.transcript.isNotEmpty()) {
            listState.animateScrollToItem(state.transcript.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Live captions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E2F23)
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (state.transcript.isEmpty()) {
                item {
                    CaptionBubble(
                        speaker = "Caller",
                        text = "Hello, this is Dr. Sharma. How are you feeling today?",
                        time = "10:42 AM",
                        isCaller = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CaptionBubble(
                        speaker = "You",
                        text = "I am feeling better today, thank you.",
                        time = "10:42 AM",
                        isCaller = false
                    )
                }
            } else {
                items(state.transcript) { line ->
                    val nowFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    CaptionBubble(
                        speaker = if (line.speaker == Speaker.Caller) "Caller" else "You",
                        text = line.text,
                        time = nowFormatted,
                        isCaller = line.speaker == Speaker.Caller
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Smart replies",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E2F23)
                            )
                        )
                        Text(
                            text = "See all >",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp,
                                color = Color(0xFF2E8540),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable { vm.refreshSmartReplies() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val suggestions = if (state.suggestions.isNotEmpty()) state.suggestions else listOf(
                        "I am feeling better",
                        "Thank you, doctor",
                        "Can you explain more?"
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFE8F5E9))
                                    .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(50))
                                    .clickable { vm.speakSuggestion(suggestion) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1E5E3A)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    VolumeUpIcon(size = 16.dp, tint = Color(0xFF2E8540))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reply Composer Input Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.draftReply,
                onValueChange = vm::onDraftChange,
                placeholder = { Text("Type a reply...", color = Color(0xFFA0B5A6)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF2E8540),
                    unfocusedBorderColor = Color(0xFFE2ECE4)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E8540))
                    .clickable { vm.sendReply() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // In-call Control Bar (Mute, End Call, TTS)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { }
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F4F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        MicOffIcon(size = 22.dp, tint = Color(0xFF55665A))
                    }
                    Text(
                        text = "Mute",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = Color(0xFF55665A)
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { vm.stopCallPipe() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935)),
                        contentAlignment = Alignment.Center
                    ) {
                        CallEndRedIcon(size = 28.dp, tint = Color.White)
                    }
                    Text(
                        text = "End call",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { vm.sendReply() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F4F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        VolumeUpIcon(size = 22.dp, tint = Color(0xFF55665A))
                    }
                    Text(
                        text = "TTS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = Color(0xFF55665A)
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptionBubble(
    speaker: String,
    text: String,
    time: String,
    isCaller: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCaller) Color.White else Color(0xFFF4F9F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isCaller) Color(0xFFEEF3EE) else Color(0xFFD4EAD8),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = speaker,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCaller) Color(0xFF55665A) else Color(0xFF2E8540)
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = Color(0xFF90A495)
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        tint = Color(0xFFB0C4B5),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = Color(0xFF1E2F23)
                )
            )
        }
    }
}

@Composable
private fun AnimatedAudioWaveform(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.size(280.dp, 100.dp)) {
        val w = size.width
        val h = size.height
        val barCount = 28
        val barWidth = w / (barCount * 1.6f)
        val brandGreen = Color(0xFF2E8540)

        for (i in 0 until barCount) {
            val x = i * (barWidth * 1.6f) + barWidth / 2f
            val baseHeight = kotlin.math.sin(phase + i * 0.35f) * 0.45f + 0.55f
            val barHeight = h * 0.85f * baseHeight.coerceIn(0.15f, 1.0f)
            val yTop = (h - barHeight) / 2f

            drawLine(
                color = brandGreen,
                start = Offset(x, yTop),
                end = Offset(x, yTop + barHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
