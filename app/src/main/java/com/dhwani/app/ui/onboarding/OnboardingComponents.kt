package com.dhwani.app.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SoftBlobBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FBF9))
    ) {
        // Decorative background blobs matching the reference design
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Top right soft organic mint blob
            drawCircle(
                color = Color(0xFFE4F3E8).copy(alpha = 0.6f),
                radius = size.width * 0.35f,
                center = Offset(size.width * 0.95f, size.height * 0.12f)
            )
            // Mid-left soft mint blob
            drawCircle(
                color = Color(0xFFE8F5E9).copy(alpha = 0.5f),
                radius = size.width * 0.3f,
                center = Offset(size.width * 0.02f, size.height * 0.50f)
            )
            // Bottom right soft mint blob
            drawCircle(
                color = Color(0xFFE4F3E8).copy(alpha = 0.7f),
                radius = size.width * 0.4f,
                center = Offset(size.width * 0.88f, size.height * 0.95f)
            )
        }

        content()
    }
}

@Composable
fun DhwaniLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    val brandGreen = Color(0xFF2E8540)
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val radius = w / 2f
        val strokeWidth = w * 0.055f

        // Outer green circle boundary
        drawCircle(
            color = brandGreen,
            radius = radius - strokeWidth / 2f,
            style = Stroke(width = strokeWidth)
        )

        // Mountain peak line art path
        val mountainPath = Path().apply {
            // Peak top
            moveTo(w * 0.50f, h * 0.22f)
            // Right slope
            lineTo(w * 0.75f, h * 0.54f)
            // Mountain base curve
            cubicTo(w * 0.65f, h * 0.58f, w * 0.35f, h * 0.58f, w * 0.25f, h * 0.54f)
            // Left slope
            close()

            // Inner snowline contour
            moveTo(w * 0.50f, h * 0.22f)
            lineTo(w * 0.40f, h * 0.37f)
            lineTo(w * 0.47f, h * 0.39f)
            lineTo(w * 0.54f, h * 0.37f)
            lineTo(w * 0.60f, h * 0.41f)
        }

        drawPath(
            path = mountainPath,
            color = brandGreen,
            style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Lower wave lines (sound / landscape ripples)
        val wave1 = Path().apply {
            moveTo(w * 0.23f, h * 0.64f)
            cubicTo(w * 0.40f, h * 0.58f, w * 0.60f, h * 0.70f, w * 0.77f, h * 0.64f)
        }
        drawPath(
            path = wave1,
            color = brandGreen,
            style = Stroke(width = strokeWidth * 0.8f, cap = StrokeCap.Round)
        )

        val wave2 = Path().apply {
            moveTo(w * 0.28f, h * 0.76f)
            cubicTo(w * 0.45f, h * 0.72f, w * 0.55f, h * 0.80f, w * 0.72f, h * 0.76f)
        }
        drawPath(
            path = wave2,
            color = brandGreen,
            style = Stroke(width = strokeWidth * 0.8f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SpeakerBubbleIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF388E3C)
) {
    Canvas(modifier = modifier.size(26.dp)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.08f

        // Speaker cone
        val speakerPath = Path().apply {
            moveTo(w * 0.15f, h * 0.38f)
            lineTo(w * 0.35f, h * 0.38f)
            lineTo(w * 0.55f, h * 0.20f)
            lineTo(w * 0.55f, h * 0.80f)
            lineTo(w * 0.35f, h * 0.62f)
            lineTo(w * 0.15f, h * 0.62f)
            close()
        }
        drawPath(path = speakerPath, color = color)

        // Sound waves
        val wave1 = Path().apply {
            moveTo(w * 0.70f, h * 0.32f)
            cubicTo(w * 0.78f, h * 0.42f, w * 0.78f, h * 0.58f, w * 0.70f, h * 0.68f)
        }
        drawPath(
            path = wave1,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        val wave2 = Path().apply {
            moveTo(w * 0.82f, h * 0.22f)
            cubicTo(w * 0.95f, h * 0.38f, w * 0.95f, h * 0.62f, w * 0.82f, h * 0.78f)
        }
        drawPath(
            path = wave2,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun HandBubbleIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF388E3C)
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f

        // Palm & fingers outline
        val handPath = Path().apply {
            // Wrist base
            moveTo(w * 0.35f, h * 0.85f)
            lineTo(w * 0.65f, h * 0.85f)

            // Outer right edge to pinky finger
            lineTo(w * 0.75f, h * 0.55f)
            lineTo(w * 0.75f, h * 0.32f)
            cubicTo(w * 0.75f, h * 0.25f, w * 0.65f, h * 0.25f, w * 0.65f, h * 0.32f)

            // Middle fingers
            lineTo(w * 0.65f, h * 0.20f)
            cubicTo(w * 0.65f, h * 0.12f, w * 0.53f, h * 0.12f, w * 0.53f, h * 0.20f)

            // Index finger
            lineTo(w * 0.53f, h * 0.24f)
            lineTo(w * 0.42f, h * 0.24f)
            cubicTo(w * 0.42f, h * 0.16f, w * 0.30f, h * 0.16f, w * 0.30f, h * 0.24f)

            // Thumb
            lineTo(w * 0.30f, h * 0.50f)
            lineTo(w * 0.20f, h * 0.55f)
            cubicTo(w * 0.14f, h * 0.60f, w * 0.18f, h * 0.72f, w * 0.26f, h * 0.72f)
            lineTo(w * 0.35f, h * 0.65f)
            close()
        }

        drawPath(
            path = handPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun OnboardingIllustration(
    modifier: Modifier = Modifier
) {
    val skinTone = Color(0xFFFFCC99)
    val hairColor = Color(0xFF5C3A21)
    val sweaterColor = Color(0xFF52B06A)
    val phoneColor = Color(0xFF1E5E3A)
    val bubbleBg = Color(0xFFE2F3E6)

    Box(
        modifier = modifier.size(310.dp, 260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft aura behind character
        Canvas(modifier = Modifier.size(240.dp)) {
            drawCircle(
                color = Color(0xFFE8F5E9).copy(alpha = 0.8f),
                radius = size.width / 2f
            )
        }

        // Main Character & Phone Drawing
        Canvas(modifier = Modifier.size(260.dp, 240.dp)) {
            val w = size.width
            val h = size.height

            // --- Sweater Body ---
            val sweaterPath = Path().apply {
                moveTo(w * 0.25f, h * 0.98f)
                cubicTo(w * 0.25f, h * 0.72f, w * 0.35f, h * 0.65f, w * 0.50f, h * 0.65f)
                cubicTo(w * 0.65f, h * 0.65f, w * 0.75f, h * 0.72f, w * 0.75f, h * 0.98f)
                close()
            }
            drawPath(path = sweaterPath, color = sweaterColor)

            // --- Head & Face ---
            drawCircle(
                color = skinTone,
                radius = w * 0.17f,
                center = Offset(w * 0.48f, h * 0.42f)
            )

            // Hair
            val hairPath = Path().apply {
                moveTo(w * 0.30f, h * 0.42f)
                cubicTo(w * 0.28f, h * 0.22f, w * 0.68f, h * 0.20f, w * 0.66f, h * 0.42f)
                cubicTo(w * 0.60f, h * 0.27f, w * 0.36f, h * 0.28f, w * 0.30f, h * 0.42f)
            }
            drawPath(path = hairPath, color = hairColor)

            // Eyes
            drawCircle(color = Color(0xFF2C2C2C), radius = w * 0.022f, center = Offset(w * 0.42f, h * 0.43f))
            drawCircle(color = Color(0xFF2C2C2C), radius = w * 0.022f, center = Offset(w * 0.54f, h * 0.43f))

            // Smile
            val smilePath = Path().apply {
                moveTo(w * 0.44f, h * 0.49f)
                cubicTo(w * 0.46f, h * 0.54f, w * 0.50f, h * 0.54f, w * 0.52f, h * 0.49f)
            }
            drawPath(
                path = smilePath,
                color = Color(0xFF8D4E2A),
                style = Stroke(width = w * 0.015f, cap = StrokeCap.Round)
            )

            // --- Waving Right Arm ---
            val waveArmPath = Path().apply {
                moveTo(w * 0.32f, h * 0.70f)
                cubicTo(w * 0.20f, h * 0.68f, w * 0.14f, h * 0.55f, w * 0.16f, h * 0.46f)
            }
            drawPath(
                path = waveArmPath,
                color = sweaterColor,
                style = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
            )
            // Waving Hand
            drawCircle(color = skinTone, radius = w * 0.065f, center = Offset(w * 0.16f, h * 0.42f))

            // --- Left Arm Holding Phone ---
            val phoneArmPath = Path().apply {
                moveTo(w * 0.64f, h * 0.70f)
                cubicTo(w * 0.72f, h * 0.68f, w * 0.74f, h * 0.60f, w * 0.70f, h * 0.55f)
            }
            drawPath(
                path = phoneArmPath,
                color = sweaterColor,
                style = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
            )

            // --- Smartphone ---
            drawRoundRect(
                color = phoneColor,
                topLeft = Offset(w * 0.64f, h * 0.46f),
                size = Size(w * 0.19f, h * 0.28f),
                cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
            )
            // Screen
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.66f, h * 0.48f),
                size = Size(w * 0.15f, h * 0.24f),
                cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
            )

            // Audio Waveform on Phone Screen
            val waveColor = Color(0xFF2E8540)
            val barW = w * 0.015f
            val bars = listOf(0.3f, 0.6f, 0.9f, 0.5f, 0.8f, 0.4f)
            bars.forEachIndexed { i, heightFactor ->
                val barX = w * 0.68f + (i * w * 0.021f)
                val barH = h * 0.10f * heightFactor
                val barY = h * 0.60f - barH / 2f
                drawRoundRect(
                    color = waveColor,
                    topLeft = Offset(barX, barY),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )
            }

            // Hand holding phone
            drawCircle(color = skinTone, radius = w * 0.045f, center = Offset(w * 0.73f, h * 0.68f))
        }

        // --- Floating Speech Bubble (Voice / Speaker) ---
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-20).dp, y = 20.dp)
                .size(54.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(bubbleBg),
            contentAlignment = Alignment.Center
        ) {
            SpeakerBubbleIcon()
        }

        // --- Floating Speech Bubble (Sign / Hand) ---
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-5).dp, y = 45.dp)
                .size(50.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(bubbleBg),
            contentAlignment = Alignment.Center
        ) {
            HandBubbleIcon()
        }
    }
}
