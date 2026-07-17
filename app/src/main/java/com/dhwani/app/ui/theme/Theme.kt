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
    primary = Color(0xFF006C67),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1E9),
    onPrimaryContainer = Color(0xFF00201E),
    secondary = Color(0xFF465F87),
    secondaryContainer = Color(0xFFD9E2FF),
    tertiary = Color(0xFFA84732),
    tertiaryContainer = Color(0xFFFFDAD1),
    background = Color(0xFFF8FAF9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDF2F0),
    outline = Color(0xFF707A78),
    outlineVariant = Color(0xFFBFC9C6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7BDAD2),
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF00504C),
    secondary = Color(0xFFB0C6F4),
    secondaryContainer = Color(0xFF2E466D),
    tertiary = Color(0xFFFFB4A3),
    tertiaryContainer = Color(0xFF7F2E1E),
    background = Color(0xFF101413),
    surface = Color(0xFF171B1A),
    surfaceVariant = Color(0xFF252B29),
    outline = Color(0xFF899391),
    outlineVariant = Color(0xFF3F4947),
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
        fontSize = 23.sp,
        lineHeight = 29.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontSize = 17.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
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
