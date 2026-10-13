package com.barriogo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.barriogo.app.R
import com.barriogo.app.ui.theme.BarrioTheme

/**
 * Teclado numérico del Figma (teclas circulares de 72 dp, dígitos 22 SemiBold).
 * - [bordered]: L4 dibuja un borde dorado sutil en cada tecla; R5 no.
 * - [bottomStart]: celda inferior izquierda opcional (L4 pone ahí el acceso con huella).
 */
@Composable
fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    bordered: Boolean = false,
    horizontalGap: Dp = BarrioTheme.spacing.xl,
    bottomStart: (@Composable () -> Unit)? = null,
) {
    val sizes = BarrioTheme.sizes
    Column(modifier, verticalArrangement = Arrangement.spacedBy(sizes.keypadGapVertical)) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(horizontalGap)) {
                row.forEach { ch -> DigitKey(ch, enabled, bordered) { onDigit(ch) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(horizontalGap)) {
            Box(Modifier.size(sizes.keypadKey), contentAlignment = Alignment.Center) { bottomStart?.invoke() }
            DigitKey('0', enabled, bordered) { onDigit('0') }
            Box(Modifier.size(sizes.keypadKey), contentAlignment = Alignment.Center) {
                val icon = @Composable {
                    Icon(painterResource(R.drawable.ic_backspace), contentDescription = "Borrar último número")
                }
                if (bordered) {
                    IconButton(onClick = onBackspace, enabled = enabled, modifier = Modifier.size(sizes.keypadKey)) { icon() }
                } else {
                    Surface(
                        onClick = onBackspace,
                        enabled = enabled,
                        shape = CircleShape,
                        color = BarrioTheme.colors.fieldContainer,
                        contentColor = BarrioTheme.colors.heading,
                        modifier = Modifier.size(sizes.keypadKey),
                    ) { Box(contentAlignment = Alignment.Center) { icon() } }
                }
            }
        }
    }
}

@Composable
private fun DigitKey(ch: Char, enabled: Boolean, bordered: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = BarrioTheme.colors.fieldContainer,
        contentColor = if (bordered) MaterialTheme.colorScheme.onSurface else BarrioTheme.colors.heading,
        border = if (bordered) BorderStroke(BarrioTheme.sizes.borderThin, BarrioTheme.colors.divider) else null,
        modifier = Modifier.size(BarrioTheme.sizes.keypadKey),
    ) {
        Box(contentAlignment = Alignment.Center) { Text(ch.toString(), style = BarrioTheme.textStyles.keypadDigit) }
    }
}

/**
 * Casillas de la clave de 4 números (Figma "Casillas PIN": 50 dp, radio 8, borde dorado).
 * Una casilla llena muestra un punto; la siguiente por llenar se resalta en amarillo.
 */
@Composable
fun PinBoxes(
    length: Int,
    filled: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    gap: Dp = BarrioTheme.spacing.sm,
) {
    Row(
        modifier.semantics { contentDescription = "$filled de $length números escritos" },
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        repeat(length) { i ->
            val borderColor = when {
                isError -> MaterialTheme.colorScheme.error
                i == filled -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }
            Box(
                Modifier
                    .size(BarrioTheme.sizes.pinBox)
                    .border(BarrioTheme.sizes.borderThin, borderColor, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                if (i < filled) {
                    Box(Modifier.size(12.dp).background(BarrioTheme.colors.heading, CircleShape))
                }
            }
        }
    }
}

/**
 * Código de verificación de [length] dígitos (Figma "Casilla OTP": 48 × 56, radio 8).
 * Es un solo campo de texto real: abre el teclado numérico del sistema, admite pegar el código
 * completo y TalkBack lo lee como un campo normal.
 */
@Composable
fun OtpField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    onDone: () -> Unit = {},
) {
    BasicTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter(Char::isDigit).take(length)) },
        modifier = modifier.semantics { contentDescription = "Código de $length dígitos" },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(BarrioTheme.spacing.xs)) {
                repeat(length) { i ->
                    val ch = value.getOrNull(i)
                    val borderColor = when {
                        isError -> MaterialTheme.colorScheme.error
                        i == value.length -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Box(
                        Modifier
                            .size(BarrioTheme.sizes.otpBoxWidth, BarrioTheme.sizes.otpBoxHeight)
                            .background(
                                if (ch != null) BarrioTheme.colors.fieldContainer else Color.Transparent,
                                MaterialTheme.shapes.small,
                            )
                            .border(BarrioTheme.sizes.borderThin, borderColor, MaterialTheme.shapes.small),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(ch?.toString() ?: "", style = BarrioTheme.textStyles.keypadDigit, color = BarrioTheme.colors.heading)
                    }
                }
            }
        },
    )
}
