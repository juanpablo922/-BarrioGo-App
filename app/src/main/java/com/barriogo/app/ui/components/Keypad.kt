package com.barriogo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.barriogo.app.ui.theme.KeypadKey

/** Teclado numérico 0-9 con borrar, usado para el PIN de 4 dígitos y para montos rápidos. */
@Composable
fun NumericKeypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf("123", "456", "789", " 0⌫")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { ch ->
                    when (ch) {
                        ' ' -> Spacer34()
                        '⌫' -> KeyButton(onClick = onBackspace) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Borrar")
                        }
                        else -> KeyButton(onClick = { onDigit(ch) }) {
                            Text(ch.toString(), style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.Spacer34() {
    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
}

@Composable
private fun RowScope.KeyButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(KeypadKey)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Fila de puntos que muestra cuántos dígitos del PIN ya se escribieron. */
@Composable
fun PinDots(length: Int, filled: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(length) { i ->
            val on = i < filled
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}
