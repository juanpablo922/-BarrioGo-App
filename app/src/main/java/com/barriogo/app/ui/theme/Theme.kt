package com.barriogo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Roles del Figma mapeados al esquema de color de Material 3 (tema oscuro, el del prototipo). */
private val DarkScheme = darkColorScheme(
    primary = BarrioPalette.Yellow400,
    onPrimary = BarrioPalette.Green900,
    secondary = BarrioPalette.Gold400,
    onSecondary = BarrioPalette.Green900,
    tertiary = BarrioPalette.Mint400,
    onTertiary = BarrioPalette.Green900,
    background = BarrioPalette.Green900,
    onBackground = BarrioPalette.Neutral50,
    surface = BarrioPalette.Green900,
    onSurface = BarrioPalette.Neutral50,
    surfaceVariant = BarrioPalette.Green700,
    onSurfaceVariant = BarrioPalette.Sage100,
    surfaceContainer = BarrioPalette.Green700,
    surfaceContainerHigh = BarrioPalette.Green700,
    outline = BarrioPalette.Gold400,
    outlineVariant = BarrioPalette.Gold400.copy(alpha = 0.3f),
    error = BarrioPalette.Amber400,
    onError = BarrioPalette.Green900,
)

// Tema claro reservado para una futura iteración (el prototipo lo define en la sección
// "Tema claro" de Figma); la app usa el tema oscuro, que es el de casi todas las pantallas.
private val LightScheme = lightColorScheme(
    primary = BarrioPalette.Yellow400,
    onPrimary = BarrioPalette.Green900,
    background = BarrioPalette.Cream50,
    surface = Color(0xFFFFFFFF),
)

/** Colores del Figma que Material 3 no cubre con un rol propio. */
@Immutable
data class BarrioExtendedColors(
    /** Títulos y lettering ("BARRIO"). */
    val heading: Color,
    /** Etiquetas secundarias y placeholder ("Correo electrónico", "tu@correo.com"). */
    val textMuted: Color,
    /** Placeholder sobre una superficie sólida (R6 "Ej. Don Juan"). */
    val placeholderOnSurface: Color,
    /** Descripción dentro de tarjetas de selección. */
    val cardDescription: Color,
    /** Relleno de campo sólido (registro) y semitransparente (ingreso). */
    val fieldContainer: Color,
    val fieldContainerSubtle: Color,
    /** Línea sutil (conector pendiente del stepper, borde de teclas, barra inferior). */
    val divider: Color,
    /** Éxito: pasos completados, aviso "Saliste de la tienda", requisito cumplido. */
    val success: Color,
    val onSuccess: Color,
    /** Botón principal deshabilitado (el Figma lo dibuja dorado al 45 % de opacidad). */
    val disabledContainer: Color,
    val disabledContent: Color,
    /** Sombra amarilla del botón principal. */
    val primaryGlow: Color,
    /** Colores de avatar que rotan entre perfiles (L3). */
    val avatarColors: List<Pair<Color, Color>>,
)

private val DarkExtended = BarrioExtendedColors(
    heading = BarrioPalette.Cream50,
    textMuted = BarrioPalette.Neutral50.copy(alpha = 0.5f),
    placeholderOnSurface = BarrioPalette.Sage400,
    cardDescription = BarrioPalette.Sage200,
    fieldContainer = BarrioPalette.Green700,
    fieldContainerSubtle = BarrioPalette.Green700.copy(alpha = 0.5f),
    divider = BarrioPalette.Gold400.copy(alpha = 0.3f),
    success = BarrioPalette.Mint400,
    onSuccess = BarrioPalette.Green900,
    disabledContainer = lerp(BarrioPalette.Green900, BarrioPalette.Gold400, 0.45f),
    disabledContent = BarrioPalette.Green900,
    primaryGlow = BarrioPalette.Yellow400.copy(alpha = 0.2f),
    avatarColors = listOf(
        BarrioPalette.Yellow400 to BarrioPalette.Green900,
        BarrioPalette.Green700 to BarrioPalette.Neutral50,
        BarrioPalette.Gold400 to BarrioPalette.Green900,
        BarrioPalette.Cream50 to BarrioPalette.Green900,
    ),
)

private val LocalBarrioColors = staticCompositionLocalOf { DarkExtended }
private val LocalBarrioTextStyles = staticCompositionLocalOf { BarrioTextStyles() }

/** Punto de acceso a los tokens propios: `BarrioTheme.colors.success`, `BarrioTheme.spacing.md`... */
object BarrioTheme {
    val colors: BarrioExtendedColors
        @Composable @ReadOnlyComposable get() = LocalBarrioColors.current
    val textStyles: BarrioTextStyles
        @Composable @ReadOnlyComposable get() = LocalBarrioTextStyles.current
    val spacing = BarrioSpacing
    val sizes = BarrioSizes
}

@Composable
fun BarrioGoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    CompositionLocalProvider(
        LocalBarrioColors provides DarkExtended,
        LocalBarrioTextStyles provides BarrioTextStyles(),
    ) {
        MaterialTheme(colorScheme = scheme, typography = BarrioTypography, shapes = BarrioShapes, content = content)
    }
}
