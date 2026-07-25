package com.dhwani.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
fun HomeNavIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color(0xFF2E8540)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.08f

        val roofPath = Path().apply {
            moveTo(w * 0.18f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.82f, h * 0.48f)
        }
        drawPath(path = roofPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val housePath = Path().apply {
            moveTo(w * 0.28f, h * 0.42f)
            lineTo(w * 0.28f, h * 0.82f)
            lineTo(w * 0.72f, h * 0.82f)
            lineTo(w * 0.72f, h * 0.42f)
        }
        drawPath(path = housePath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun LiveNavIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color(0xFF2E8540)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f

        val bars = listOf(0.35f, 0.75f, 1.0f, 0.65f, 0.40f)
        bars.forEachIndexed { i, heightFactor ->
            val barX = w * 0.20f + (i * w * 0.15f)
            val barH = h * 0.60f * heightFactor
            val barY = h * 0.50f - barH / 2f
            drawLine(
                color = tint,
                start = Offset(barX, barY),
                end = Offset(barX, barY + barH),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun CallNavIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color(0xFF2E8540)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f

        val phonePath = Path().apply {
            moveTo(w * 0.28f, h * 0.24f)
            cubicTo(w * 0.28f, h * 0.20f, w * 0.35f, h * 0.20f, w * 0.38f, h * 0.28f)
            lineTo(w * 0.45f, h * 0.42f)
            cubicTo(w * 0.48f, h * 0.48f, w * 0.42f, h * 0.54f, w * 0.36f, h * 0.58f)
            cubicTo(w * 0.42f, h * 0.70f, w * 0.54f, h * 0.78f, w * 0.64f, h * 0.82f)
            cubicTo(w * 0.68f, h * 0.76f, w * 0.74f, h * 0.72f, w * 0.80f, h * 0.75f)
            lineTo(w * 0.90f, h * 0.82f)
            cubicTo(w * 0.96f, h * 0.86f, w * 0.94f, h * 0.96f, w * 0.86f, h * 0.96f)
            cubicTo(w * 0.45f, h * 0.96f, w * 0.18f, h * 0.69f, w * 0.18f, h * 0.28f)
            close()
        }
        drawPath(path = phonePath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun YouNavIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color(0xFF2E8540)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f

        drawCircle(
            color = tint,
            radius = w * 0.20f,
            center = Offset(w * 0.50f, h * 0.32f),
            style = Stroke(width = stroke)
        )

        val shoulderPath = Path().apply {
            moveTo(w * 0.20f, h * 0.82f)
            cubicTo(w * 0.20f, h * 0.64f, w * 0.35f, h * 0.58f, w * 0.50f, h * 0.58f)
            cubicTo(w * 0.65f, h * 0.58f, w * 0.80f, h * 0.64f, w * 0.80f, h * 0.82f)
        }
        drawPath(path = shoulderPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun BellNotificationIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color(0xFF1E2F23)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.08f

        val bellPath = Path().apply {
            moveTo(w * 0.50f, h * 0.15f)
            cubicTo(w * 0.35f, h * 0.15f, w * 0.25f, h * 0.28f, w * 0.25f, h * 0.48f)
            lineTo(w * 0.20f, h * 0.68f)
            lineTo(w * 0.80f, h * 0.68f)
            lineTo(w * 0.75f, h * 0.48f)
            cubicTo(w * 0.75f, h * 0.28f, w * 0.65f, h * 0.15f, w * 0.50f, h * 0.15f)
            close()
        }
        drawPath(path = bellPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val clapperPath = Path().apply {
            moveTo(w * 0.40f, h * 0.75f)
            cubicTo(w * 0.40f, h * 0.85f, w * 0.60f, h * 0.85f, w * 0.60f, h * 0.75f)
        }
        drawPath(path = clapperPath, color = tint, style = Stroke(width = stroke))
    }
}

@Composable
fun ShieldCheckIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color(0xFF2E8540)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.08f

        val shieldPath = Path().apply {
            moveTo(w * 0.50f, h * 0.12f)
            lineTo(w * 0.82f, h * 0.24f)
            lineTo(w * 0.82f, h * 0.52f)
            cubicTo(w * 0.82f, h * 0.78f, w * 0.50f, h * 0.90f, w * 0.50f, h * 0.90f)
            cubicTo(w * 0.50f, h * 0.90f, w * 0.18f, h * 0.78f, w * 0.18f, h * 0.52f)
            lineTo(w * 0.18f, h * 0.24f)
            close()
        }
        drawPath(path = shieldPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val checkPath = Path().apply {
            moveTo(w * 0.38f, h * 0.48f)
            lineTo(w * 0.48f, h * 0.58f)
            lineTo(w * 0.65f, h * 0.38f)
        }
        drawPath(path = checkPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun AIBrainIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color(0xFF9C27B0)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.08f

        val brainPath = Path().apply {
            moveTo(w * 0.50f, h * 0.20f)
            cubicTo(w * 0.30f, h * 0.20f, w * 0.20f, h * 0.35f, w * 0.20f, h * 0.50f)
            cubicTo(w * 0.20f, h * 0.65f, w * 0.35f, h * 0.80f, w * 0.50f, h * 0.80f)
            cubicTo(w * 0.65f, h * 0.80f, w * 0.80f, h * 0.65f, w * 0.80f, h * 0.50f)
            cubicTo(w * 0.80f, h * 0.35f, w * 0.70f, h * 0.20f, w * 0.50f, h * 0.20f)
        }
        drawPath(path = brainPath, color = tint, style = Stroke(width = stroke))

        drawLine(color = tint, start = Offset(w * 0.50f, h * 0.20f), end = Offset(w * 0.50f, h * 0.80f), strokeWidth = stroke)
    }
}

@Composable
fun MicIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.09f

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.35f, h * 0.12f),
            size = Size(w * 0.30f, h * 0.46f),
            cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
        )

        val arcPath = Path().apply {
            moveTo(w * 0.22f, h * 0.38f)
            cubicTo(w * 0.22f, h * 0.72f, w * 0.78f, h * 0.72f, w * 0.78f, h * 0.38f)
        }
        drawPath(path = arcPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))

        drawLine(color = tint, start = Offset(w * 0.50f, h * 0.68f), end = Offset(w * 0.50f, h * 0.84f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.32f, h * 0.84f), end = Offset(w * 0.68f, h * 0.84f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun MicOffIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color(0xFF55665A)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.09f

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.35f, h * 0.12f),
            size = Size(w * 0.30f, h * 0.46f),
            cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
        )

        drawLine(color = tint, start = Offset(w * 0.15f, h * 0.15f), end = Offset(w * 0.85f, h * 0.85f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun CallEndRedIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.12f

        val phonePath = Path().apply {
            moveTo(w * 0.20f, h * 0.40f)
            cubicTo(w * 0.35f, h * 0.60f, w * 0.65f, h * 0.60f, w * 0.80f, h * 0.40f)
        }
        drawPath(path = phonePath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun VolumeUpIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = Color(0xFF2E8540)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.09f

        val speakerPath = Path().apply {
            moveTo(w * 0.15f, h * 0.38f)
            lineTo(w * 0.35f, h * 0.38f)
            lineTo(w * 0.55f, h * 0.20f)
            lineTo(w * 0.55f, h * 0.80f)
            lineTo(w * 0.35f, h * 0.62f)
            lineTo(w * 0.15f, h * 0.62f)
            close()
        }
        drawPath(path = speakerPath, color = tint)

        val wave1 = Path().apply {
            moveTo(w * 0.70f, h * 0.32f)
            cubicTo(w * 0.78f, h * 0.42f, w * 0.78f, h * 0.58f, w * 0.70f, h * 0.68f)
        }
        drawPath(path = wave1, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun AccessTimeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = Color(0xFFA0B5A6)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.09f

        drawCircle(color = tint, radius = w * 0.40f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(width = stroke))
        drawLine(color = tint, start = Offset(w * 0.50f, h * 0.50f), end = Offset(w * 0.50f, h * 0.28f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.50f, h * 0.50f), end = Offset(w * 0.68f, h * 0.50f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun ContactPhoneIcon(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = Color(0xFF2E8540)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.09f

        drawCircle(color = tint, radius = w * 0.16f, center = Offset(w * 0.50f, h * 0.32f), style = Stroke(width = stroke))
        val bodyPath = Path().apply {
            moveTo(w * 0.25f, h * 0.78f)
            cubicTo(w * 0.25f, h * 0.58f, w * 0.38f, h * 0.54f, w * 0.50f, h * 0.54f)
            cubicTo(w * 0.62f, h * 0.54f, w * 0.75f, h * 0.58f, w * 0.75f, h * 0.78f)
        }
        drawPath(path = bodyPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun PersonOutlineIcon(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = Color(0xFF708A77)
) {
    YouNavIcon(modifier = modifier, size = size, tint = tint)
}

@Composable
fun AddCircleOutlineIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = Color(0xFF2E8540)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.09f

        drawCircle(color = tint, radius = w * 0.40f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(width = stroke))
        drawLine(color = tint, start = Offset(w * 0.50f, h * 0.30f), end = Offset(w * 0.50f, h * 0.70f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.30f, h * 0.50f), end = Offset(w * 0.70f, h * 0.50f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun RecordVoiceOverIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = Color(0xFFF57F17)
) {
    VolumeUpIcon(modifier = modifier, size = size, tint = tint)
}

@Composable
fun CameraSwitchIcon(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = Color(0xFF2E8540)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.08f

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.20f, h * 0.30f),
            size = Size(w * 0.60f, h * 0.45f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = stroke)
        )
        val lensPath = Path().apply {
            moveTo(w * 0.40f, h * 0.30f)
            lineTo(w * 0.45f, h * 0.20f)
            lineTo(w * 0.55f, h * 0.20f)
            lineTo(w * 0.60f, h * 0.30f)
        }
        drawPath(path = lensPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawCircle(color = tint, radius = w * 0.12f, center = Offset(w * 0.50f, h * 0.52f), style = Stroke(width = stroke))
    }
}

@Composable
fun CameraVideoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.08f

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.15f, h * 0.28f),
            size = Size(w * 0.50f, h * 0.44f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        val arrowPath = Path().apply {
            moveTo(w * 0.68f, h * 0.38f)
            lineTo(w * 0.88f, h * 0.28f)
            lineTo(w * 0.88f, h * 0.72f)
            lineTo(w * 0.68f, h * 0.62f)
            close()
        }
        drawPath(path = arrowPath, color = tint)
    }
}
