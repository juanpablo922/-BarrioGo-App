package com.barriogo.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta base del Figma "Proyecto" (valores crudos, sin significado).
 * No se usa directamente en pantallas: alimenta el esquema de Material 3 ([Theme.kt]) y los
 * colores extendidos de [BarrioExtendedColors]. Las pantallas piden colores por su rol.
 */
object BarrioPalette {
    val Green900 = Color(0xFF164030)   // fondo de todas las pantallas
    val Green700 = Color(0xFF22593F)   // campos, tarjetas, teclas
    val Yellow400 = Color(0xFFF2C849)  // acción principal, links, marca "GO"
    val Gold400 = Color(0xFFCCAC6D)    // bordes, acción secundaria
    val Mint400 = Color(0xFF6FCF97)    // éxito, pasos completados
    val Amber400 = Color(0xFFF2B544)   // mensajes de error / validación
    val Cream50 = Color(0xFFFAF5EF)    // títulos
    val Neutral50 = Color(0xFFF2F2F2)  // texto de cuerpo
    val Sage100 = Color(0xFFD9E0DB)    // texto de apoyo
    val Sage200 = Color(0xFFC7D4CC)    // descripción en tarjetas
    val Sage400 = Color(0xFFA6B8AD)    // placeholder sobre superficie sólida
}

// ---- Colores heredados de los módulos de negocio (Inicio, Caja, Movimientos...) ----
// Se conservan para no alterar esas pantallas; el flujo de acceso usa los roles de MaterialTheme
// y BarrioTheme.colors.
val BgDark = BarrioPalette.Green900     // fondo principal (igual a colors.xml/themes.xml)
val SurfaceDark = Color(0xFF1E4E3B)     // tarjetas sobre el fondo
val SurfaceDarkAlt = Color(0xFF19402F)  // tarjetas secundarias / filas de lista
val KeypadKey = Color(0xFF0F2E21)       // teclas del teclado numérico (módulos de montos)

val Accent = BarrioPalette.Yellow400    // amarillo de acción principal
val OnAccent = Color(0xFF2A2105)        // texto sobre amarillo

val TextPrimary = Color(0xFFFFFFFF)
val TextMuted = Color(0xFFBFD9CB)       // texto secundario sobre verde oscuro
val TextFaint = Color(0xFF8FAE9E)

val PositiveGreen = Color(0xFF7FD9A6)   // entradas, "+"
val NegativeCoral = Color(0xFFF3A28C)   // salidas, "-"
val Danger = Color(0xFFE5736B)          // anular, eliminar
val InfoBlue = Color(0xFF8FC7E8)

val Divider = Color(0xFF2C5C48)
