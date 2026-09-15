package com.airship.ctvlab.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

object Netflix {
    val Red = Color(0xFFE50914)
    val Background = Color(0xFF141414)
    val Elevated = Color(0xFF1F1F1F)
    val Card = Color(0xFF2A2A2A)
    val Mute = Color(0xFFB3B3B3)
}

private val colors = darkColorScheme(
    primary = Netflix.Red,
    onPrimary = Color.White,
    surface = Netflix.Elevated,
    onSurface = Color.White,
    surfaceVariant = Netflix.Card,
    onSurfaceVariant = Netflix.Mute,
    background = Netflix.Background,
    onBackground = Color.White,
    border = Color.White,
)

@Composable
fun CtvLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
