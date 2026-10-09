package com.barriogo.app.ui.cash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.cashDelta
import com.barriogo.app.domain.label
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.theme.BarrioColors

/**
 * C1 · Caja (Screen 13 del prototipo Figma con arqueo y edición de dinero en caja).
 */
@Composable
fun CashScreen(onOpenCash: () -> Unit, onSeeAllMovements: () -> Unit) {
    val vm = barrioViewModel { CashViewModel(it) }
    val s by vm.state.collectAsState()

    var showEditCashModal by remember { mutableStateOf(false) }
    var showArqueoScreen by remember { mutableStateOf(false) }
    val displayMoney = s.saldo
    var editAmountText by remember { mutableStateOf("") }

    // Arqueo state
    var realCountText by remember { mutableStateOf("") }
    var observationText by remember { mutableStateOf("") }

    if (showArqueoScreen) {
        ArqueoView(
            expectedAmount = displayMoney,
            realCount = realCountText.ifBlank { Money.group(displayMoney) },
            onRealCountChange = { realCountText = it },
            observation = observationText,
            onObservationChange = { observationText = it },
            onBack = { showArqueoScreen = false },
            onSave = {
                val counted = realCountText.replace(".", "").replace(" ", "").toLongOrNull() ?: displayMoney
                vm.saveArqueo(counted, observationText) { showArqueoScreen = false }
            }
        )
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .testTag("cash_screen")
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(14.dp)) }

            // Top Bar: "Caja" on left, profile avatar on right
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Caja",
                        color = BarrioColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("cash_title")
                    )

                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BarrioColors.Surface)
                            .border(1.5.dp, BarrioColors.YellowStroke, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("J", color = BarrioColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Card 1: Dinero en caja
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(BarrioColors.CardBg)
                        .border(1.dp, BarrioColors.CardSubtle, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dinero en caja", color = BarrioColors.TextHint, fontSize = 14.sp)
                            IconButton(
                                onClick = { showEditCashModal = true },
                                modifier = Modifier.size(28.dp).testTag("edit_cash_button")
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = "Editar dinero",
                                    tint = BarrioColors.Yellow,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            Money.cop(displayMoney),
                            color = BarrioColors.Yellow,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Apertura del día: ${Money.cop(s.session?.openingAmount ?: 0L)}",
                            color = BarrioColors.TextHint,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Row of Two Stats Cards: Ventas & Gastos
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Ventas
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(BarrioColors.CardBg)
                            .border(1.dp, BarrioColors.CardSubtle, RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ArrowUpward, null, tint = BarrioColors.PositiveGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ventas", color = BarrioColors.TextHint, fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                Money.cop(s.entradas),
                                color = BarrioColors.TextPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Gastos
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(BarrioColors.CardBg)
                            .border(1.dp, BarrioColors.CardSubtle, RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ArrowDownward, null, tint = BarrioColors.NegativeRed, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Gastos", color = BarrioColors.TextHint, fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (s.salidas > 0) "- " + Money.cop(s.salidas) else "$ 0",
                                color = BarrioColors.TextPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Card 2: Total fiados pendientes
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(BarrioColors.CardBg)
                        .border(1.dp, BarrioColors.CardSubtle, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total fiados pendientes", color = BarrioColors.TextHint, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(Money.cop(s.fiadoTotal), color = BarrioColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(BarrioColors.Surface)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Fiados", color = BarrioColors.TextHint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Action Buttons: Cerrar caja & Arqueo de caja
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Cerrar caja
                    Button(
                        onClick = {
                            vm.closeCash(s.saldo, "Cierre de turno") { }
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("close_cash_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BarrioColors.Surface,
                            contentColor = BarrioColors.Yellow
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BarrioColors.YellowStroke)
                    ) {
                        Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cerrar caja", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Arqueo de caja
                    Button(
                        onClick = { showArqueoScreen = true },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("arqueo_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BarrioColors.Surface,
                            contentColor = BarrioColors.Yellow
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BarrioColors.YellowStroke)
                    ) {
                        Icon(Icons.Outlined.Calculate, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Arqueo de caja", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Section: Últimos movimientos de caja
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Últimos movimientos de caja", color = BarrioColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Ver todos",
                        color = BarrioColors.Yellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onSeeAllMovements)
                    )
                }
            }

            val cashItems = s.todayMovements.take(5).map { m ->
                val isIncome = m.cashDelta() >= 0
                val prefix = if (isIncome) "+ " else "- "
                MovementCashModel(
                    title = m.label(),
                    subtitle = "${Dates.time(m.createdAt)} · ${m.type.lowercase().replaceFirstChar { it.uppercase() }}",
                    amount = prefix + Money.cop(m.amount),
                    isIncome = isIncome
                )
            }

            if (cashItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(BarrioColors.CardBg)
                            .border(1.dp, BarrioColors.CardSubtle, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sin movimientos registrados en caja hoy", color = BarrioColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("Los ingresos y egresos aparecerán aquí", color = BarrioColors.TextHint, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(cashItems) { m ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(BarrioColors.CardBg)
                            .border(1.dp, BarrioColors.CardSubtle, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (m.isIncome) BarrioColors.PositiveGreen.copy(alpha = 0.2f) else BarrioColors.NegativeRed.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (m.isIncome) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                        null,
                                        tint = if (m.isIncome) BarrioColors.PositiveGreen else BarrioColors.NegativeRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(m.title, color = BarrioColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(2.dp))
                                    Text(m.subtitle, color = BarrioColors.TextHint, fontSize = 12.sp)
                                }
                            }

                            Text(
                                m.amount,
                                color = if (m.isIncome) BarrioColors.PositiveGreen else BarrioColors.NegativeRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(90.dp)) }
        }

        // Bottom Sheet: Editar dinero en caja (Screen 12 del video)
        if (showEditCashModal) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { showEditCashModal = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(BarrioColors.Background)
                        .border(1.dp, BarrioColors.YellowStroke, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .clickable(enabled = false) {}
                        .padding(24.dp)
                        .testTag("edit_cash_modal")
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Editar dinero en caja", color = BarrioColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showEditCashModal = false }) {
                                Icon(Icons.Outlined.Close, null, tint = BarrioColors.TextHint)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Monto actual
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Monto actual", color = BarrioColors.TextHint, fontSize = 13.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    Money.cop(displayMoney),
                                    color = BarrioColors.Yellow,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BarrioColors.Surface)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("En caja", color = BarrioColors.TextHint, fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Nuevo monto
                        Text("Nuevo monto", color = BarrioColors.TextHint, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = editAmountText,
                            onValueChange = { editAmountText = it },
                            prefix = { Text("$ ", color = BarrioColors.Yellow, fontWeight = FontWeight.Bold) },
                            trailingIcon = { Icon(Icons.Outlined.Edit, null, tint = BarrioColors.Yellow) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_cash_input"),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = TextStyle(color = BarrioColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = BarrioColors.Surface,
                                unfocusedContainerColor = BarrioColors.Surface,
                                focusedBorderColor = BarrioColors.Yellow,
                                unfocusedBorderColor = BarrioColors.YellowStroke
                            )
                        )

                        Spacer(Modifier.height(14.dp))

                        Text(
                            "ⓘ Este ajuste se registrará en el libro de movimientos como corrección de caja.",
                            color = BarrioColors.TextHint,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val clean = editAmountText.replace(".", "").replace(" ", "").toLongOrNull()
                                if (clean != null && clean > 0) {
                                    vm.editOpening(clean) { }
                                }
                                showEditCashModal = false
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_cash_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BarrioColors.Yellow, contentColor = BarrioColors.OnYellow)
                        ) {
                            Text("Guardar cambios", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ArqueoView(
    expectedAmount: Long,
    realCount: String,
    onRealCountChange: (String) -> Unit,
    observation: String,
    onObservationChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .systemBarsPadding()
            .padding(20.dp)
            .testTag("arqueo_screen")
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = BarrioColors.Yellow)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Arqueo de caja", color = BarrioColors.Yellow, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(20.dp))

                // Saldo esperado por el sistema
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BarrioColors.CardBg)
                        .padding(18.dp)
                ) {
                    Column {
                        Text("Saldo esperado por el sistema", color = BarrioColors.TextHint, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$ ${String.format("%,d", expectedAmount).replace(',', '.')}",
                            color = BarrioColors.Yellow,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text("Conteo de efectivo real", color = BarrioColors.TextHint, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = realCount,
                    onValueChange = onRealCountChange,
                    prefix = { Text("$ ", color = BarrioColors.Yellow, fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("real_cash_count_input"),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = TextStyle(color = BarrioColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BarrioColors.Surface,
                        unfocusedContainerColor = BarrioColors.Surface,
                        focusedBorderColor = BarrioColors.Yellow,
                        unfocusedBorderColor = BarrioColors.YellowStroke
                    )
                )

                Spacer(Modifier.height(14.dp))

                // Faltante badge
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BarrioColors.NegativeRed.copy(alpha = 0.15f))
                        .padding(14.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Faltante", color = BarrioColors.NegativeRed, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("-$ 8.000", color = BarrioColors.NegativeRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text("Observaciones", color = BarrioColors.TextHint, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = observation,
                    onValueChange = onObservationChange,
                    modifier = Modifier.fillMaxWidth().testTag("observation_input"),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = TextStyle(color = BarrioColors.TextPrimary, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BarrioColors.Surface,
                        unfocusedContainerColor = BarrioColors.Surface,
                        focusedBorderColor = BarrioColors.Yellow,
                        unfocusedBorderColor = BarrioColors.YellowStroke
                    )
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    "Realizado por: Don Juan · 15 sep 2026, 6:30 PM",
                    color = BarrioColors.TextHint,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_arqueo_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BarrioColors.Yellow, contentColor = BarrioColors.OnYellow)
            ) {
                Text("Guardar arqueo", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

data class MovementCashModel(
    val title: String,
    val subtitle: String,
    val amount: String,
    val isIncome: Boolean
)
