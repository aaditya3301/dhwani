package com.dhwani.app.ui.sign

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.dhwani.app.sign.SignFrameAnalyzer
import com.dhwani.app.ui.CallState
import com.dhwani.app.ui.CallViewModel
import com.dhwani.app.ui.onboarding.HandBubbleIcon
import com.dhwani.app.ui.theme.CameraSwitchIcon
import com.dhwani.app.ui.theme.CameraVideoIcon
import com.dhwani.app.ui.theme.RecordVoiceOverIcon
import java.util.concurrent.Executors

@Composable
fun SignInterpreterScreen(
    state: CallState,
    vm: CallViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var analyzer by remember { mutableStateOf<SignFrameAnalyzer?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lensFacing) {
        onDispose {
            analyzer?.close()
            analyzer = null
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF9FBF9)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        HandBubbleIcon(color = Color(0xFF0288D1))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sign Interpreter",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E2F23)
                            )
                        )
                        Text(
                            text = "Real-time ISL Gesture Recognition",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = Color(0xFF708A77)
                            )
                        )
                    }
                }

                // Camera Lens Flip Toggle Button
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                            CameraSelector.LENS_FACING_BACK
                        } else {
                            CameraSelector.LENS_FACING_FRONT
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFEEF5F0))
                ) {
                    CameraSwitchIcon(size = 22.dp, tint = Color(0xFF2E8540))
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Camera View Box with Frame overlay
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (cameraError != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cameraError ?: "Camera unavailable",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                    }

                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        try {
                                            val cameraProvider = cameraProviderFuture.get()
                                            val preview = Preview.Builder().build().also {
                                                it.setSurfaceProvider(previewView.surfaceProvider)
                                            }

                                            val signAnalyzer = SignFrameAnalyzer(
                                                context = ctx,
                                                onReady = { vm.onSignRecognizerReady() },
                                                onStatus = { msg -> vm.onSignCaptureStatus(msg) },
                                                onResult = { rec -> vm.onSignRecognition(rec) },
                                                onError = { err -> vm.onSignCaptureError(err) }
                                            )
                                            analyzer = signAnalyzer

                                            val imageAnalysis = ImageAnalysis.Builder()
                                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                                .build()
                                                .also {
                                                    it.setAnalyzer(
                                                        Executors.newSingleThreadExecutor(),
                                                        signAnalyzer
                                                    )
                                                }

                                            val cameraSelector = CameraSelector.Builder()
                                                .requireLensFacing(lensFacing)
                                                .build()

                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                cameraSelector,
                                                preview,
                                                imageAnalysis
                                            )
                                        } catch (e: Exception) {
                                            cameraError = "Could not initialize camera: ${e.message}"
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))

                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Guidance Frame Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .border(
                                    2.dp,
                                    if (state.isSignCapturing) Color(0xFF2E8540) else Color(0x66FFFFFF),
                                    RoundedCornerShape(16.dp)
                                )
                        )

                        // Status Badge inside Camera Overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color(0xCC0F1712))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (state.isSignCapturing) Color(0xFF56C874) else Color(0xFFFFB300)
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        state.isSignCapturing && state.signStatus.isNotBlank() -> state.signStatus
                                        state.isSignCapturing -> "Perform one complete sign"
                                        state.signStatus.isNotBlank() -> state.signStatus
                                        !state.isSignRecognizerReady -> "Loading sign models..."
                                        else -> "Ready to recognize"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        if (state.isSignCapturing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x99000000)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(54.dp),
                                        color = Color.White,
                                        strokeWidth = 5.dp,
                                    )
                                    Text(
                                        text = if (state.signStatus.startsWith("Recognizing")) {
                                            "Finding the closest ISL sign..."
                                        } else {
                                            "Recording your sign..."
                                        },
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                    Text(
                                        text = state.signStatus.ifBlank { "Move naturally and finish the sign" },
                                        color = Color(0xFFDCEFE1),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val activeAnalyzer = analyzer
                            if (activeAnalyzer == null || !state.isSignRecognizerReady) {
                                vm.onSignCaptureError("The sign camera is still loading. Try again in a moment.")
                            } else {
                                vm.startSignCapture()
                                activeAnalyzer.startCapture()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E8540),
                            contentColor = Color.White
                        ),
                        enabled = analyzer != null &&
                            state.isSignRecognizerReady &&
                            !state.isSignCapturing
                    ) {
                        CameraVideoIcon(size = 20.dp, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                state.isSignCapturing -> "Recording..."
                                !state.isSignRecognizerReady -> "Loading sign models..."
                                else -> "Recognize sign"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    if (state.signSentence.isNotBlank()) {
                        OutlinedButton(
                            onClick = vm::speakSignedSentence,
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF2E8540)
                            )
                        ) {
                            RecordVoiceOverIcon(size = 20.dp, tint = Color(0xFF2E8540))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Speak",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Recognized Sign Output Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFEEF3EE), RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Interpreted Result",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF708A77)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (state.signSentence.isNotBlank()) {
                            Text(
                                text = state.signSentence,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E2F23)
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = Color(0xFFE4F3E8)
                                ) {
                                    Text(
                                        text = "Sign: ${state.selectedSignGloss}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF2E8540)
                                        )
                                    )
                                }

                                Text(
                                    text = "Ready to speak or send",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = Color(0xFF8E9B90)
                                    )
                                )
                            }
                        } else {
                            Text(
                                text = "Tap Recognize, then perform one complete sign with your upper body visible.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    color = Color(0xFF708A77)
                                )
                            )
                        }
                    }
                }

                // Preset ISL Sign Phrases Section
                Text(
                    text = "Supported ISL signs",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E2F23)
                    )
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.signPhrases.forEach { phrase ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.selectSignPhrase(phrase) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFEEF3EE), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "✌️",
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = phrase.gloss,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E2F23)
                                            )
                                        )
                                        Text(
                                            text = phrase.english,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                color = Color(0xFF708A77)
                                            )
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = "Select",
                                    tint = Color(0xFF2E8540),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
