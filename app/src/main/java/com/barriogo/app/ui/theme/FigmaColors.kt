package com.barriogo.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Colores de las pantallas de caja y gastos del Figma "Proyecto" (K0/K1, C1, c2–c4, G1, G2).
 * El Figma está en modo oscuro; en modo claro se usan los equivalentes de LightBarrioPalette.
 */
object FigmaColors {
    private val dark: Boolean @Composable get() = BarrioColors.isDark

    /** #22593F · tarjetas, campos y teclas. */
    val Tarjeta: Color @Composable get() = if (dark) Color(0xFF22593F) else Color(0xFFFFFFFF)
    /** #103528 · avisos y fondo del teclado. */
    val Hundido: Color @Composable get() = if (dark) Color(0xFF103528) else Color(0xFFF3EEE4)
    /** #1C4E36 · campos de c2 (Efectivo contado, Observaciones). */
    val Campo: Color @Composable get() = if (dark) Color(0xFF1C4E36) else Color(0xFFFFFDF9)
    val BordeCampo: Color @Composable get() = if (dark) Color.White.copy(alpha = 0.18f) else Color(0x336B5628)
    /** #FAF5EF · títulos y texto principal. */
    val Crema: Color @Composable get() = if (dark) Color(0xFFFAF5EF) else Color(0xFF0F2E22)
    /** #D9E0DB · texto de párrafo. */
    val Cuerpo: Color @Composable get() = if (dark) Color(0xFFD9E0DB) else Color(0xFF3D4D45)
    /** #A6B8AD · subtítulos y placeholders. */
    val Tenue: Color @Composable get() = if (dark) Color(0xFFA6B8AD) else Color(0xFF5A6B62)
    /** #CCAC6D · etiquetas doradas y bordes. */
    val Dorado: Color @Composable get() = if (dark) Color(0xFFCCAC6D) else Color(0xFF6B5628)
    /** #F2C849 · botón principal, títulos de cabecera y bordes activos. */
    val Amarillo: Color @Composable get() = if (dark) Color(0xFFF2C849) else Color(0xFFC2870C)
    val AmarilloBoton = Color(0xFFF2C849)
    val SobreAmarillo = Color(0xFF164030)
    /** #6FCF97 · entradas, éxito. */
    val Verde: Color @Composable get() = if (dark) Color(0xFF6FCF97) else Color(0xFF1B8A4E)
    /** #F2B544 · salidas, gastos y alertas suaves. */
    val Naranja: Color @Composable get() = if (dark) Color(0xFFF2B544) else Color(0xFFB7791F)
    /** #F28C44 · faltante / advertencia del cierre. */
    val Alerta: Color @Composable get() = if (dark) Color(0xFFF28C44) else Color(0xFFC2410C)
}
