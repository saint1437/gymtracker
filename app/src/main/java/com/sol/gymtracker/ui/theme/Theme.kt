package com.sol.gymtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GymDarkColors = darkColorScheme(
    primary = Color(0xFFD7FF4F),
    onPrimary = Color(0xFF1A2200),
    secondary = Color(0xFF89D8FF),
    tertiary = Color(0xFFFFB86B),
    background = Color(0xFF0A0D11),
    surface = Color(0xFF12171D),
    surfaceVariant = Color(0xFF202832),
    onSurface = Color(0xFFE8EDF3),
    onSurfaceVariant = Color(0xFFAEB9C6),
)

@Composable
fun GymTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GymDarkColors, content = content)
}