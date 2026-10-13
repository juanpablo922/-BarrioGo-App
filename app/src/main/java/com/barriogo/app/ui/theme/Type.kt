package com.barriogo.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.barriogo.app.R

/** Inter, la fuente de todo el prototipo de Figma (incluida en res/font para funcionar sin internet). */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun inter(weight: FontWeight, size: Int, lineHeight: Int, letterSpacing: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/**
 * Escala tipográfica de Material 3 con los estilos del Figma:
 * - headlineMedium  26 Bold      → "Crea tu perfil", "Entra con tu huella"
 * - headlineSmall   22 SemiBold  → títulos de pantalla ("Ingresa a tu tienda") y barra superior
 * - titleLarge      20 SemiBold  → título de sección ("Datos del propietario")
 * - titleMedium     17 Bold      → título de tarjeta ("Administrador")
 * - bodyLarge       16 Regular   → texto de campos y párrafos
 * - bodyMedium      14 Regular   → etiquetas, ayudas, errores
 * - bodySmall       13 Regular   → descripciones y notas al pie
 * - labelLarge      16 SemiBold  → botones, links y etiquetas fuertes de campo
 */
val BarrioTypography = Typography(
    headlineMedium = inter(FontWeight.Bold, 26, 32),
    headlineSmall = inter(FontWeight.SemiBold, 22, 28),
    titleLarge = inter(FontWeight.SemiBold, 20, 25),
    titleMedium = inter(FontWeight.Bold, 17, 22),
    titleSmall = inter(FontWeight.SemiBold, 15, 20),
    bodyLarge = inter(FontWeight.Normal, 16, 24),
    bodyMedium = inter(FontWeight.Normal, 14, 20),
    bodySmall = inter(FontWeight.Normal, 13, 18),
    labelLarge = inter(FontWeight.SemiBold, 16, 20),
    labelMedium = inter(FontWeight.Normal, 14, 20),
    labelSmall = inter(FontWeight.Medium, 11, 14),
)

/** Estilos del Figma que no tienen rol equivalente en Material 3. */
@Immutable
data class BarrioTextStyles(
    /** Lettering "BARRIOGO" del splash. */
    val brandLarge: TextStyle = inter(FontWeight.Bold, 42, 50, letterSpacing = -0.84),
    /** Lettering "BARRIOGO" de ingreso y recuperación. */
    val brandMedium: TextStyle = inter(FontWeight.Bold, 35, 42, letterSpacing = -0.7),
    /** Subtítulos de 15 px ("Con estos 4 números entrarás..."). */
    val subtitle: TextStyle = inter(FontWeight.Normal, 15, 20),
    /** Número grande del contador de bloqueo ("00:30"). */
    val timer: TextStyle = inter(FontWeight.Bold, 40, 48),
    /** Inicial dentro del avatar grande del selector de perfil. */
    val avatarInitial: TextStyle = inter(FontWeight.Bold, 32, 48),
    /** Dígitos del teclado numérico y de las casillas de PIN. */
    val keypadDigit: TextStyle = inter(FontWeight.SemiBold, 22, 32),
    /** Número dentro de los pasos del stepper de registro. */
    val stepNumber: TextStyle = inter(FontWeight.Bold, 15, 18),
)

/** Tamaño grande para montos protagonistas (saldo de caja, total de una propuesta de voz). */
val MoneyDisplay = inter(FontWeight.Bold, 34, 38)
