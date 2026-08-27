package com.gymos.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GymosColors = darkColorScheme(
    primary = Color(0xFFFF4500),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCC3700),
    secondary = Color(0xFFFF4500),
    background = Color(0xFF0D0D0D),
    surface = Color(0xFF111111),
    surfaceVariant = Color(0xFF1A1A1A),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFAAAAAA),
    error = Color(0xFFFF4444),
    tertiary = Color(0xFF00FF88)
)

@Composable
fun GymosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GymosColors,
        content = content
    )
}