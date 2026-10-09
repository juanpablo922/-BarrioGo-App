package com.barriogo.app.ui.movements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.theme.BarrioColors

/**
 * M1 · Registrar movimiento (Screen 18 del prototipo Figma con soporte de Gasto).
 */
@Composable
fun RegisterMovementScreen(
    onSaved: () -> Unit,
    onGoVoice: (MovementType) -> Unit,
    onGoScan: () -> Unit,
    onBack: () -> Unit = onSaved
) {
    val vm = barrioViewModel { MovementsViewModel(it) }
    var selectedType by remember { mutableStateOf(MovementType.VENTA) }
    var expenseKind by remember { mutableStateOf("Proveedor") }
    var amountText by remember { mutableStateOf("") }
    var conceptText by remember { mutableStateOf("") }

    val isExpense = selectedType == MovementType.GASTO

    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .systemBarsPadding()
            .imePadding()
            .testTag("register_movement_screen")
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(Modifier.height(14.dp))

                // Header
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = BarrioColors.Yellow)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isExpense) "Registrar gasto" else "Registrar movimiento",
                        color = BarrioColors.Yellow,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Selector: ¿Qué vas a registrar?
                Text(
                    if (isExpense) "¿Qué tipo de gasto fue?" else "¿Qué vas a registrar?",
                    color = BarrioColors.TextHint,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(10.dp))

                if (!isExpense) {
                    // Row of 3 options: Venta, Gasto, Fiado
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TypeSelectionCard(
                            title = "Venta",
                            icon = Icons.Outlined.PointOfSale,
                            isSelected = selectedType == MovementType.VENTA,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedType = MovementType.VENTA }
                        )
                        TypeSelectionCard(
                            title = "Gasto",
                            icon = Icons.Outlined.ReceiptLong,
                            isSelected = selectedType == MovementType.GASTO,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedType = MovementType.GASTO }
                        )
                        TypeSelectionCard(
                            title = "Fiado",
                            icon = Icons.Outlined.Person,
                            isSelected = selectedType == MovementType.FIADO,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedType = MovementType.FIADO }
                        )
                    }
                } else {
                    // Expense sub-kinds: Proveedor, Servicios, Otros
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Proveedor", "Servicios", "Otros").forEach { k ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(BarrioColors.CardBg)
                                    .border(
                                        1.5.dp,
                                        if (expenseKind == k) BarrioColors.Yellow else BarrioColors.CardSubtle,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { expenseKind = k }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    k,
                                    color = if (expenseKind == k) BarrioColors.Yellow else BarrioColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (expenseKind == k) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Field 1: ¿Cuánto? / ¿Cuánto pagaste?
                Text(
                    if (isExpense) "¿Cuánto pagaste?" else "¿Cuánto?",
                    color = BarrioColors.TextHint,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    placeholder = { Text("0", color = BarrioColors.Yellow, fontSize = 28.sp, fontWeight = FontWeight.Bold) },
                    prefix = { Text("$ ", color = BarrioColors.Yellow, fontSize = 28.sp, fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("amount_input"),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = TextStyle(color = BarrioColors.Yellow, fontSize = 28.sp, fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BarrioColors.Surface,
                        unfocusedContainerColor = BarrioColors.Surface,
                        focusedBorderColor = BarrioColors.Yellow,
                        unfocusedBorderColor = BarrioColors.YellowStroke
                    )
                )

                Spacer(Modifier.height(20.dp))

                // Field 2: ¿De qué? (opcional) / ¿En qué fue el gasto?
                Text(
                    if (isExpense) "¿En qué fue el gasto?" else "¿De qué? (opcional)",
                    color = BarrioColors.TextHint,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = conceptText,
                    onValueChange = { conceptText = it },
                    placeholder = {
                        Text(
                            if (isExpense) "Ej. Postobón, recibo del gas" else "Ej. Bolsa de arroz",
                            color = BarrioColors.TextHint.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("concept_input"),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = TextStyle(color = BarrioColors.TextPrimary, fontSize = 15.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BarrioColors.Surface,
                        unfocusedContainerColor = BarrioColors.Surface,
                        focusedBorderColor = BarrioColors.Yellow,
                        unfocusedBorderColor = BarrioColors.YellowStroke
                    )
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 36.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val hasAmount = amountText.toLongOrNull() != null && amountText.toLong() > 0

                Button(
                    onClick = {
                        val amount = amountText.toLongOrNull() ?: 0L
                        vm.register(
                            type = selectedType,
                            amount = amount,
                            title = conceptText.ifBlank { if (isExpense) expenseKind else selectedType.name },
                            detail = if (isExpense) "Gasto en $expenseKind" else "",
                            payMethod = PayMethod.EFECTIVO,
                            sessionId = vm.activeSessionId.value,
                            onDone = onSaved
                        )
                    },
                    enabled = hasAmount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_movement_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BarrioColors.Yellow,
                        contentColor = BarrioColors.OnYellow,
                        disabledContainerColor = BarrioColors.Yellow.copy(alpha = 0.5f),
                        disabledContentColor = BarrioColors.OnYellow
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        if (isExpense) "Guardar gasto" else "Guardar movimiento",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    if (hasAmount) "Listo para guardar" else if (isExpense) "Elige el tipo de gasto" else "Elige el tipo y escribe el monto",
                    color = BarrioColors.TextHint,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun TypeSelectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BarrioColors.CardBg)
            .border(
                1.5.dp,
                if (isSelected) BarrioColors.Yellow else BarrioColors.CardSubtle,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) BarrioColors.Yellow else BarrioColors.TextHint,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                title,
                color = if (isSelected) BarrioColors.Yellow else BarrioColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
