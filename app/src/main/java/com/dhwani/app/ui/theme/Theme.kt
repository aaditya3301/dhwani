package com.dhwani.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF17594A),
    onPrimary = Color.White,
    secondary = Color(0xFF415F91),
    tertiary = Color(0xFF8A4F24),
    background = Color(0xFFF7F7F4),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5E8E1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DD8C3),
    secondary = Color(0xFFAFC7FF),
    tertiary = Color(0xFFFFB782),
)

@Composable
fun DhwaniTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
