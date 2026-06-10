package com.mimir.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette — тёмно-синяя с акцентом на изумрудный
private val PrimaryGreen   = Color(0xFF00D9A3)
private val PrimaryDark    = Color(0xFF00A87E)
private val Background     = Color(0xFF0F1117)
private val Surface        = Color(0xFF171B24)
private val SurfaceVariant = Color(0xFF1E2330)
private val OnSurface      = Color(0xFFE2E5EC)
private val OnSurfaceMuted = Color(0xFF8B909E)

private val DarkColors = darkColorScheme(
    primary          = PrimaryGreen,
    onPrimary        = Color(0xFF003826),
    primaryContainer = Color(0xFF004D37),
    onPrimaryContainer = Color(0xFF7FFFD4),
    secondary        = Color(0xFF4FCBFF),
    onSecondary      = Color(0xFF003547),
    background       = Background,
    onBackground     = OnSurface,
    surface          = Surface,
    onSurface        = OnSurface,
    surfaceVariant   = SurfaceVariant,
    onSurfaceVariant = OnSurfaceMuted,
    outline          = Color(0xFF2E3444),
    error            = Color(0xFFFF6B6B),
)

private val LightColors = lightColorScheme(
    primary          = Color(0xFF007A5A),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFB7F5E0),
    onPrimaryContainer = Color(0xFF002117),
    secondary        = Color(0xFF006B8A),
    onSecondary      = Color.White,
    background       = Color(0xFFF5F7FA),
    onBackground     = Color(0xFF1A1C22),
    surface          = Color.White,
    onSurface        = Color(0xFF1A1C22),
    surfaceVariant   = Color(0xFFEEF0F5),
    onSurfaceVariant = Color(0xFF5A5E6B),
    outline          = Color(0xFFDDE0E8),
    error            = Color(0xFFD32F2F),
)

@Composable
fun MimirTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography  = MimirTypography,
        content     = content
    )
}
