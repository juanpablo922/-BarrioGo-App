package com.barriogo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDarkAlt,
    onSurfaceVariant = TextMuted,
    error = Danger,
    outline = Divider,
)

private val LightScheme = lightColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    background = Color(0xFFF6F3EB),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF164030),
    onSurface = Color(0xFF164030),
)

@Composable
fun BarrioGoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = if (darkTheme) DarkBarrioPalette else LightBarrioPalette
    val scheme = if (darkTheme) DarkScheme else LightScheme
    CompositionLocalProvider(LocalBarrioColors provides palette) {
        MaterialTheme(colorScheme = scheme, typography = BarrioTypography, content = content)
    }
}
