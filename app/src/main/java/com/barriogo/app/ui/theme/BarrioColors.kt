package com.barriogo.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class BarrioColorPalette(
    val Background: Color,
    val Surface: Color,
    val Yellow: Color,
    val YellowStroke: Color,
    val OnYellow: Color,
    val TextPrimary: Color,
    val TextHint: Color,
    val Error: Color,
    val CardBg: Color,
    val CardSubtle: Color,
    val PositiveGreen: Color,
    val NegativeRed: Color,
    val DarkNav: Color,
    val isDark: Boolean,
)

val DarkBarrioPalette = BarrioColorPalette(
    Background = Color(0xFF164030),      // fondo verde oscuro Figma
    Surface = Color(0xFF1C4C37),         // campos de texto / círculo del botón de tema
    Yellow = Color(0xFFF2C849),          // botón principal y links
    YellowStroke = Color(0xFFF2C849).copy(alpha = 0.55f),
    OnYellow = Color(0xFF164030),
    TextPrimary = Color(0xFFFFFFFF),
    TextHint = Color(0xFF8FA89B),
    Error = Color(0xFFFF8A80),
    CardBg = Color(0xFF1B4938),
    CardSubtle = Color(0xFF143B2C),
    PositiveGreen = Color(0xFF5CD696),
    NegativeRed = Color(0xFFFF6B6B),
    DarkNav = Color(0xFF0F2E22),
    isDark = true,
)

val LightBarrioPalette = BarrioColorPalette(
    Background = Color(0xFFF6F3EB),      // Tono claro del prototipo Figma (crema cálido)
    Surface = Color(0xFFFFFFFF),         // Superficie blanca limpia
    Yellow = Color(0xFFE5B324),          // Dorado/amarillo corporativo legible
    YellowStroke = Color(0xFFD4A31B),    // Borde acentuado
    OnYellow = Color(0xFF164030),        // Texto oscuro contrastado
    TextPrimary = Color(0xFF164030),     // Texto principal verde bosque elegante
    TextHint = Color(0xFF637D71),        // Texto secundario legible
    Error = Color(0xFFD32F2F),           // Error estándar
    CardBg = Color(0xFFFFFFFF),          // Tarjetas en blanco limpio
    CardSubtle = Color(0xFFE5DFC9),      // Bordes sutiles en tarjetas
    PositiveGreen = Color(0xFF1B8A4E),   // Verde positivo legible en claro
    NegativeRed = Color(0xFFC62828),     // Rojo egreso legible en claro
    DarkNav = Color(0xFFFFFFFF),         // Barra inferior clara
    isDark = false,
)

val LocalBarrioColors = staticCompositionLocalOf { DarkBarrioPalette }

/** Colores tomados del Figma "Proyecto" con soporte dinámico para Modo Oscuro y Modo Claro. */
object BarrioColors {
    val Background: Color @Composable get() = LocalBarrioColors.current.Background
    val Surface: Color @Composable get() = LocalBarrioColors.current.Surface
    val Yellow: Color @Composable get() = LocalBarrioColors.current.Yellow
    val YellowStroke: Color @Composable get() = LocalBarrioColors.current.YellowStroke
    val OnYellow: Color @Composable get() = LocalBarrioColors.current.OnYellow
    val TextPrimary: Color @Composable get() = LocalBarrioColors.current.TextPrimary
    val TextHint: Color @Composable get() = LocalBarrioColors.current.TextHint
    val Error: Color @Composable get() = LocalBarrioColors.current.Error
    val CardBg: Color @Composable get() = LocalBarrioColors.current.CardBg
    val CardSubtle: Color @Composable get() = LocalBarrioColors.current.CardSubtle
    val PositiveGreen: Color @Composable get() = LocalBarrioColors.current.PositiveGreen
    val NegativeRed: Color @Composable get() = LocalBarrioColors.current.NegativeRed
    val DarkNav: Color @Composable get() = LocalBarrioColors.current.DarkNav
    val isDark: Boolean @Composable get() = LocalBarrioColors.current.isDark
}
