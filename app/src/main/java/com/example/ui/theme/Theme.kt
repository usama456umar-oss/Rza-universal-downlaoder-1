package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RzaPrimary,
    onPrimary = Color.Black,
    primaryContainer = RzaPrimaryDark,
    onPrimaryContainer = RzaPrimaryGlow,
    secondary = RzaTextSecondary,
    onSecondary = Color.Black,
    secondaryContainer = RzaSurfaceElevated,
    onSecondaryContainer = RzaTextPrimary,
    tertiary = RzaAccentPink,
    onTertiary = Color.White,
    background = RzaBackground,
    onBackground = RzaTextPrimary,
    surface = RzaSurface,
    onSurface = RzaTextPrimary,
    surfaceVariant = RzaSurfaceElevated,
    onSurfaceVariant = RzaTextSecondary,
    outline = RzaBorder,
    outlineVariant = RzaBorderSubtle,
    error = RzaError,
    onError = Color.Black
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // RZA Downloader uses consistent premium dark neon styling
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
