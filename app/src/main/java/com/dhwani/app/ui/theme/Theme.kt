package com.dhwani.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E8540),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4F3E8),
    onPrimaryContainer = Color(0xFF0F381F),
    secondary = Color(0xFF1E5E3A),
    secondaryContainer = Color(0xFFD2E8D7),
    onSecondaryContainer = Color(0xFF0A2B19),
    tertiary = Color(0xFF388E3C),
    tertiaryContainer = Color(0xFFE8F5E9),
    background = Color(0xFFF9FBF9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF5F0),
    onSurface = Color(0xFF1A211C),
    onSurfaceVariant = Color(0xFF4A5C50),
    outline = Color(0xFF708A77),
    outlineVariant = Color(0xFFC8D8CC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF56C874),
    onPrimary = Color(0xFF003915),
    primaryContainer = Color(0xFF135226),
    onPrimaryContainer = Color(0xFFB7F2C5),
    secondary = Color(0xFF42A660),
    secondaryContainer = Color(0xFF0A3C1D),
    background = Color(0xFF0F1712),
    surface = Color(0xFF152219),
    surfaceVariant = Color(0xFF1E2F23),
    onSurface = Color(0xFFE2ECE4),
    onSurfaceVariant = Color(0xFFA2B8A8),
    outline = Color(0xFF6E8675),
    outlineVariant = Color(0xFF2A3D30),
)

private val DhwaniShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
)

private val DhwaniTypography = Typography(
    headlineSmall = TextStyle(
        fontSize = 26.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    )
)

@Composable
fun DhwaniTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = DhwaniShapes,
        typography = DhwaniTypography,
        content = content,
    )
}
