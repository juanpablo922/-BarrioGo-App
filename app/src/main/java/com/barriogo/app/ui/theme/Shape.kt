package com.barriogo.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Radios del Figma mapeados a la escala de Material 3:
 * small 8 (casillas de PIN), medium 12 (campos y botones), large 16 (tarjetas de selección).
 * Los avatares, teclas y sensores usan CircleShape.
 */
val BarrioShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
