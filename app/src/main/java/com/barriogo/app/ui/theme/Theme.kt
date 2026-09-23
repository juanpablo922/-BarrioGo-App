package com.barriogo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

// Tema claro reservado para una futura iteración (el prototipo lo define en la sección
// "Tema claro" de Figma); la app usa el tema oscuro, que es el de casi todas las pantallas.
private val LightScheme = lightColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    background = Color(0xFFF5F1E9),
    surface = Color(0xFFFFFFFF),
)

@Composable
fun BarrioGoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    MaterialTheme(colorScheme = scheme, typography = BarrioTypography, content = content)
}
