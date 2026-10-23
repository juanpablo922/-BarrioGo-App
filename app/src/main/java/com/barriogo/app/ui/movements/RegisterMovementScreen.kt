package com.barriogo.app.ui.movements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.FigmaInput
import com.barriogo.app.ui.components.FigmaInputColors
import com.barriogo.app.ui.components.MoneyVisualTransformation
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.Inter

/** Verde de "Venta" / "Todo listo" del Figma. */
private val VerdeVenta = Color(0xFF6FCF97)
private val Amarillo = Color(0xFFF2C849)

/**
 * M1 · Registrar movimiento > A mano (Figma: tarjetas Venta ↗ / Gasto ↘ / Fiado, campo "¿Cuánto?"
 * grande en blanco y "Guardar movimiento" con su mensaje de estado debajo).
 */
@Composable
fun RegisterMovementScreen(
    onSaved: () -> Unit,
    onGoVoice: (MovementType) -> Unit,
    onGoScan: () -> Unit,
    onGoExpense: (() -> Unit)? = null,
    onBack: () -> Unit = onSaved
) {
    val vm = barrioViewModel { MovementsViewModel(it) }
    var selectedType by remember { mutableStateOf(MovementType.VENTA) }
    var expenseKind by remember { mutableStateOf("Proveedor") }
    var amountText by remember { mutableStateOf("") }
    var conceptText by remember { mutableStateOf("") }
    // El aviso "Escribe un monto mayor a cero" sale solo después de tocar el campo
    var amountTouched by remember { mutableStateOf(false) }

    val isExpense = selectedType == MovementType.GASTO
    val hasAmount = amountText.toLongOrNull()?.let { it > 0 } == true
    val texto = FigmaInputColors.texto

    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .statusBarsPadding()
            .imePadding()
            .testTag("register_movement_screen")
    ) {
      Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            // Header: flecha y título en amarillo
            Row(
                Modifier.fillMaxWidth().padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .offset(x = (-12).dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack)
                        .testTag("back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Amarillo, modifier = Modifier.size(24.dp))
                }
                Text(
                    if (isExpense) "Registrar gasto" else "Registrar movimiento",
                    color = Amarillo,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(x = (-4).dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Etiqueta(if (isExpense) "¿Qué tipo de gasto fue?" else "¿Qué vas a registrar?")

            if (!isExpense) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeSelectionCard(
                        title = "Venta",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconTint = VerdeVenta,
                        isSelected = selectedType == MovementType.VENTA,
                        modifier = Modifier.weight(1f).testTag("type_venta"),
                        onClick = { selectedType = MovementType.VENTA }
                    )
                    TypeSelectionCard(
                        title = "Gasto",
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        iconTint = Amarillo,
                        isSelected = selectedType == MovementType.GASTO,
                        modifier = Modifier.weight(1f).testTag("type_gasto"),
                        // Los gastos se registran en G2 (con sus validaciones y la caja abierta).
                        onClick = { onGoExpense?.invoke() ?: run { selectedType = MovementType.GASTO } }
                    )
                    TypeSelectionCard(
                        title = "Fiado",
                        icon = Icons.Filled.Person,
                        iconTint = texto,
                        isSelected = selectedType == MovementType.FIADO,
                        modifier = Modifier.weight(1f).testTag("type_fiado"),
                        onClick = { selectedType = MovementType.FIADO }
                    )
                }
            } else {
                // Tipos de gasto: Proveedor, Servicios, Otros (mismo estilo de tarjeta)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Proveedor", "Servicios", "Otros").forEach { k ->
                        val elegido = expenseKind == k
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FigmaInputColors.fondo)
                                .border(if (elegido) 2.dp else 1.dp, if (elegido) Amarillo else FigmaInputColors.borde, RoundedCornerShape(12.dp))
                                .clickable { expenseKind = k },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(k, color = texto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ¿Cuánto?: caja con borde amarillo y el monto grande en blanco
            Etiqueta(if (isExpense) "¿Cuánto pagaste?" else "¿Cuánto?")
            BasicTextField(
                value = amountText,
                onValueChange = {
                    amountTouched = true
                    amountText = it.filter { c -> c.isDigit() }.take(9)
                },
                singleLine = true,
                textStyle = TextStyle(fontFamily = Inter, color = texto, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(Amarillo),
                visualTransformation = if (amountText.isEmpty()) VisualTransformation.None else MoneyVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag("amount_input"),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(FigmaInputColors.fondo)
                            .border(2.dp, Amarillo, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (amountText.isEmpty()) {
                            Text("$ 0", color = texto.copy(alpha = 0.7f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        inner()
                    }
                }
            )
            // Espacio reservado para el aviso, así el formulario no salta
            Text(
                if (amountTouched && !hasAmount) "Escribe un monto mayor a cero" else "",
                color = FigmaInputColors.alerta,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp).testTag("amount_error")
            )

            Spacer(Modifier.height(12.dp))

            // ¿De qué? (opcional)
            Etiqueta(if (isExpense) "¿En qué fue el gasto?" else "¿De qué? (opcional)")
            FigmaInput(
                value = conceptText,
                onValueChange = { conceptText = it },
                placeholder = if (isExpense) "Ej. Postobón, recibo del gas" else "Ej. Bolsa de arroz",
                imeAction = ImeAction.Done,
                reserveErrorSpace = false,
                modifier = Modifier.testTag("concept_input")
            )

            Spacer(Modifier.height(24.dp))
        }

            // Pie fijo: Guardar + mensaje de estado
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(
                    if (isExpense) "Guardar gasto" else "Guardar movimiento",
                    enabled = hasAmount,
                    modifier = Modifier.testTag("save_movement_button"),
                ) {
                    val amount = amountText.toLongOrNull() ?: 0L
                    vm.register(
                        type = selectedType,
                        amount = amount,
                        title = conceptText.ifBlank { if (isExpense) expenseKind else selectedType.name },
                        detail = if (isExpense) "Gasto en $expenseKind" else "",
                        payMethod = PayMethod.EFECTIVO,
                        onDone = onSaved
                    )
                }

                Text(
                    if (hasAmount) "Todo listo, ya puedes guardar" else "Falta el monto",
                    color = if (hasAmount) VerdeVenta else Amarillo,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("save_status")
                )
            }
      }
    }
}

/** Etiqueta de campo en blanco encendido. */
@Composable
private fun Etiqueta(texto: String) {
    Text(
        texto,
        color = FigmaInputColors.texto,
        fontSize = 15.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

/** Tarjeta Venta / Gasto / Fiado: ícono de color arriba y nombre en blanco SemiBold. */
@Composable
private fun TypeSelectionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .height(80.dp)
            .clip(forma)
            .background(FigmaInputColors.fondo)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) Amarillo else FigmaInputColors.borde, forma)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, color = FigmaInputColors.texto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
