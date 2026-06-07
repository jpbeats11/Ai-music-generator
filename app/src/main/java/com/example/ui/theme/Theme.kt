package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkStudioColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = DarkSurfaceCard,
    onPrimaryContainer = TextPrimary,
    secondary = CyberCyan,
    onSecondary = TextDark,
    tertiary = BrightMagenta,
    onTertiary = Color.White,
    background = MidnightIndigo,
    onBackground = TextPrimary,
    surface = DeepStudioSlate,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = TextMuted,
    outline = Color(0xFF374151)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme for a premium music player experience
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve the custom neon visual vibe
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkStudioColorScheme,
        typography = Typography,
        content = content
    )
}
