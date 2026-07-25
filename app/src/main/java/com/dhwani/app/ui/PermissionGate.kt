package com.dhwani.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhwani.app.ui.onboarding.SoftBlobBackground
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

@Composable
private fun MicPermissionIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF2E8540)
) {
    Canvas(modifier = modifier.size(44.dp)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.08f

        // Mic capsule body
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.35f, h * 0.12f),
            size = Size(w * 0.30f, h * 0.46f),
            cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
        )

        // Mic cradle arc
        val arcPath = Path().apply {
            moveTo(w * 0.22f, h * 0.38f)
            cubicTo(w * 0.22f, h * 0.72f, w * 0.78f, h * 0.72f, w * 0.78f, h * 0.38f)
        }
        drawPath(
            path = arcPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // Mic stand stem & base
        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.68f),
            end = Offset(w * 0.50f, h * 0.84f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.32f, h * 0.84f),
            end = Offset(w * 0.68f, h * 0.84f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGate(
    permissions: MultiplePermissionsState,
    content: @Composable () -> Unit,
) {
    if (permissions.allPermissionsGranted) {
        content()
        return
    }

    SoftBlobBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color(0xFFE4F3E8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MicPermissionIcon()
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Microphone & Camera",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E5E3A)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Allow microphone and camera access to caption calls and interpret video sign language gestures in real-time. Your data stays 100% on this phone.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF55665A),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = permissions::launchMultiplePermissionRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E8540),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Grant Permission",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
