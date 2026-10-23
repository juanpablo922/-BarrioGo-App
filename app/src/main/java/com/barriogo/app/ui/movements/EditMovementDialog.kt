package com.barriogo.app.ui.movements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.HighlightOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.domain.Money
import com.barriogo.app.ui.components.FigmaInput
import com.barriogo.app.ui.components.FigmaInputColors
import com.barriogo.app.ui.components.MoneyVisualTransformation
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.components.TextLinkButton
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.Inter

/** "venta", "gasto", "fiado"… para los textos de la ventana. */
internal fun MovementEntity.nombreTipo(): String = when (type) {
    "VENTA" -> "venta"
    "GASTO" -> "gasto"
    "FIADO" -> "fiado"
    "ABONO" -> "abono"
    else -> "movimiento"
}

/**
 * Ventana de vidrio para corregir el valor/concepto de un movimiento o eliminarlo.
 * Eliminar pide confirmación y no borra: el movimiento queda tachado en el historial.
 */
@Composable
fun EditMovementDialog(
    movement: MovementEntity,
    onSave: (amount: Long, title: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember { mutableStateOf(movement.amount.toString()) }
    var title by remember { mutableStateOf(movement.title) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    val amount = amountText.toLongOrNull() ?: 0L
    val tipo = movement.nombreTipo()
    val dark = BarrioColors.isDark
    val forma = RoundedCornerShape(24.dp)
    val amarillo = Color(0xFFF2C849)
    val texto = FigmaInputColors.texto

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .shadow(24.dp, forma, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.6f))
                .clip(forma)
                .background(
                    if (dark) Brush.verticalGradient(listOf(Color(0xF24B6056), Color(0xF23A4D43)))
                    else SolidColor(Color(0xFAFFFDF9))
                )
                .border(1.dp, if (dark) Color.White.copy(alpha = 0.22f) else Color(0x336B5628), forma)
                .padding(20.dp)
                .testTag("edit_movement_dialog"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (confirmarBorrado) "¿Eliminar esta $tipo?" else "Editar $tipo",
                    color = BarrioColors.Title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (dark) Color.White.copy(alpha = 0.12f) else Color(0x1A0F2E22))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.HighlightOff, "Cerrar", tint = BarrioColors.Title, modifier = Modifier.size(24.dp))
                }
            }

            if (confirmarBorrado) {
                Text(
                    "${movement.title} · ${Money.cop(movement.amount)}\n\n" +
                        "No se borra del historial: quedará tachada y marcada como eliminada, " +
                        "y ya no contará en la caja ni en los totales.",
                    color = BarrioColors.Body,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                )
                Spacer(Modifier.height(4.dp))
                PrimaryButton("Sí, eliminar", modifier = Modifier.testTag("confirm_delete_movement")) {
                    onDelete()
                    onDismiss()
                }
                TextLinkButton("Cancelar", color = BarrioColors.Gold) { confirmarBorrado = false }
            } else {
                Text("Valor", color = texto, fontSize = 15.sp)
                BasicTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit).take(9) },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = Inter, color = texto, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(amarillo),
                    visualTransformation = if (amountText.isEmpty()) VisualTransformation.None else MoneyVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().testTag("edit_amount_input"),
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FigmaInputColors.fondo)
                                .border(2.dp, amarillo, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (amountText.isEmpty()) Text("$ 0", color = texto.copy(alpha = 0.7f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            inner()
                        }
                    },
                )
                if (amount <= 0) Text("Escribe un monto mayor a cero", color = FigmaInputColors.alerta, fontSize = 14.sp)

                Text("Concepto", color = texto, fontSize = 15.sp)
                FigmaInput(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Ej. Bolsa de arroz",
                    imeAction = ImeAction.Done,
                    reserveErrorSpace = false,
                    modifier = Modifier.testTag("edit_title_input"),
                )

                Spacer(Modifier.height(4.dp))
                PrimaryButton(
                    "Guardar cambios",
                    enabled = amount > 0 && (amount != movement.amount || title.trim() != movement.title),
                    modifier = Modifier.testTag("save_edit_movement"),
                ) {
                    onSave(amount, title.trim().ifBlank { movement.title })
                    onDismiss()
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { confirmarBorrado = true }
                        .testTag("delete_movement_button"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val rojo = if (dark) Color(0xFFFF8A80) else BarrioColors.Error
                    Icon(Icons.Outlined.DeleteOutline, null, tint = rojo, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminar $tipo", color = rojo, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
