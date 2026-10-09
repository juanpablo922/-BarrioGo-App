package com.barriogo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.ui.theme.BarrioColors

/** Teclado numérico 0-9 con diseño circular del prototipo. */
@Composable
fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    leftActionIcon: (@Composable () -> Unit)? = null
) {
    val rows = listOf("123", "456", "789", " 0⌫")
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { ch ->
                    when (ch) {
                        ' ' -> {
                            if (leftActionIcon != null) {
                                KeyButton(onClick = {}) { leftActionIcon() }
                            } else {
                                SpacerKey()
                            }
                        }
                        '⌫' -> KeyButton(onClick = onBackspace) {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Borrar",
                                tint = BarrioColors.TextHint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        else -> KeyButton(onClick = { onDigit(ch) }) {
                            Text(
                                ch.toString(),
                                color = BarrioColors.TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpacerKey() {
    Box(modifier = Modifier.size(68.dp))
}

@Composable
private fun KeyButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(BarrioColors.CardBg)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** 4 Casillas cuadradas de PIN con borde dorado como en el video Figma. */
@Composable
fun PinSquares(length: Int = 4, filled: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(length) { i ->
            val isFilled = i < filled
            Box(
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isFilled) BarrioColors.Yellow else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (isFilled) BarrioColors.Yellow else BarrioColors.YellowStroke,
                        shape = RoundedCornerShape(12.dp)
                    )
            )
        }
    }
}

/** Fila de puntos que muestra cuántos dígitos del PIN ya se escribieron (legacy). */
@Composable
fun PinDots(length: Int, filled: Int, modifier: Modifier = Modifier) {
    PinSquares(length = length, filled = filled, modifier = modifier)
}
