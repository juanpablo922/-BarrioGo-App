package com.barriogo.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Colores tomados del Figma "Proyecto" (pantalla Login). */
object BarrioColors {
    val Background = Color(0xFF164030)      // fondo verde oscuro
    val Surface = Color(0xFF1C4C37)         // campos de texto / círculo del botón de tema
    val Yellow = Color(0xFFF2C849)          // botón principal y links
    val YellowStroke = Color(0xFFF2C849).copy(alpha = 0.55f) // borde de los campos
    val OnYellow = Color(0xFF164030)        // texto sobre el botón amarillo
    val TextPrimary = Color(0xFFFFFFFF)
    val TextHint = Color(0xFF8FA89B)        // placeholder y label del correo
    val Error = Color(0xFFFF8A80)
}
