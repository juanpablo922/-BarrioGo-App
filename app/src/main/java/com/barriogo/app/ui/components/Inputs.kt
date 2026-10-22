package com.barriogo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.Inter

/** Colores del "Input" del Figma (Campo · L2 Correo / Contraseña). */
object FigmaInputColors {
    val fondo: Color @Composable get() = if (BarrioColors.isDark) Color(0x8022593F) else Color(0xFFFFFDF9)
    val borde: Color @Composable get() = if (BarrioColors.isDark) Color(0xFFCCAC6D) else Color(0xFF6B5628)
    val texto: Color @Composable get() = if (BarrioColors.isDark) Color(0xFFF2F2F2) else Color(0xFF0F2E22)
    val tenue: Color @Composable get() = texto.copy(alpha = 0.5f)
    val alerta: Color @Composable get() = if (BarrioColors.isDark) Color(0xFFF2B544) else BarrioColors.Error
}

/**
 * Campo del Figma: caja de 56 con esquinas de 12, borde dorado de 1, texto Inter 16,
 * ícono a la izquierda (el texto arranca en 49) y botón opcional a la derecha.
 * Debajo, a 8, el mensaje de alerta con su espacio siempre reservado.
 */
@Composable
fun FigmaInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onNext: () -> Unit = {},
    onDone: () -> Unit = {},
    error: String? = null,
    highlightBorder: Boolean = false,
    /** false = el mensaje solo ocupa lugar cuando hay error (formularios largos como el registro). */
    reserveErrorSpace: Boolean = true,
) {
    val shape = RoundedCornerShape(12.dp)
    val alerta = FigmaInputColors.alerta
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(shape)
                .background(FigmaInputColors.fondo)
                .border(if (highlightBorder) 2.dp else 1.dp, if (highlightBorder) alerta else FigmaInputColors.borde, shape),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontFamily = Inter, fontSize = 16.sp, color = FigmaInputColors.texto),
                cursorBrush = SolidColor(Color(0xFFF2C849)),
                visualTransformation = visualTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onDone() }),
                modifier = modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
                    .padding(start = if (leading != null) 49.dp else 17.dp, end = if (trailing != null) 56.dp else 17.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) Text(placeholder, color = FigmaInputColors.tenue, fontSize = 16.sp, maxLines = 1)
                        inner()
                    }
                },
            )
            leading?.let { Box(Modifier.align(Alignment.CenterStart).padding(start = 18.dp)) { it() } }
            trailing?.let { Box(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) { it() } }
        }
        if (reserveErrorSpace || error != null) {
            Text(
                error.orEmpty(),
                color = alerta,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().alpha(if (error != null) 1f else 0f),
            )
        }
    }
}
