package com.barriogo.app.ui.movements

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.cashDelta
import com.barriogo.app.domain.label
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.theme.BarrioColors

data class MovementItem(
    val movement: com.barriogo.app.data.MovementEntity,
    val title: String,
    val timeKind: String,
    val amount: String,
    val type: String, // "Venta", "Gasto", "Fiado"
    val isPositive: Boolean
)

/**
 * M2 · Movimientos (Screen 15 del prototipo Figma con filtros de tiempo y tipo).
 */
@Composable
fun MovementsBookScreen(onBack: () -> Unit) {
    val vm = barrioViewModel { MovementsViewModel(it) }
    val allMovements by vm.movements.collectAsState()

    var selectedTimeFilter by remember { mutableStateOf("Hoy") }
    var selectedTypeFilter by remember { mutableStateOf("Todos") }
    var editando by remember { mutableStateOf<com.barriogo.app.data.MovementEntity?>(null) }

    val now = remember { System.currentTimeMillis() }

    val timeFiltered = remember(allMovements, selectedTimeFilter) {
        allMovements.filter { m ->
            when (selectedTimeFilter) {
                "Hoy" -> Dates.localDate(m.createdAt) == Dates.localDate(now)
                "Semana" -> m.createdAt >= Dates.startOfDaysAgo(7)
                "Mes" -> m.createdAt >= Dates.startOfDaysAgo(30)
                else -> true
            }
        }
    }

    val filteredList = remember(timeFiltered, selectedTypeFilter) {
        timeFiltered.filter { m ->
            when (selectedTypeFilter) {
                "Ventas" -> m.type == "VENTA"
                "Gastos" -> m.type == "GASTO"
                "Fiados" -> m.type == "FIADO"
                else -> true
            }
        }.map { m ->
            val isIncome = m.cashDelta() >= 0
            val prefix = if (isIncome) "+ " else "- "
            val formatted = if (m.type == "FIADO") Money.cop(m.amount) else prefix + Money.cop(m.amount)
            val typeLabel = when (m.type) {
                "VENTA" -> "Venta"
                "GASTO" -> "Gasto"
                "FIADO" -> "Fiado"
                "ABONO" -> "Abono"
                else -> m.type
            }
            val estado = when {
                m.voided -> " · Eliminado" + (m.voidedAt?.let { " " + Dates.time(it) } ?: "")
                m.editedAt != null -> " · Editado" + (m.originalAmount?.let { " (antes " + Money.cop(it) + ")" } ?: "")
                else -> ""
            }
            MovementItem(
                movement = m,
                title = m.label(),
                timeKind = "${Dates.time(m.createdAt)} · $typeLabel$estado",
                amount = formatted,
                type = typeLabel,
                isPositive = isIncome
            )
        }
    }

    val summaryText = remember(timeFiltered, selectedTypeFilter) {
        val vigentes = timeFiltered.filter { !it.voided }
        when (selectedTypeFilter) {
            "Ventas" -> {
                val count = vigentes.count { it.type == "VENTA" }
                val total = vigentes.filter { it.type == "VENTA" }.sumOf { it.amount }
                "$count ventas  ·  Total: + ${Money.cop(total)}"
            }
            "Gastos" -> {
                val count = vigentes.count { it.type == "GASTO" }
                val total = vigentes.filter { it.type == "GASTO" }.sumOf { it.amount }
                "$count gastos  ·  Total: - ${Money.cop(total)}"
            }
            "Fiados" -> {
                val count = vigentes.count { it.type == "FIADO" }
                val total = vigentes.filter { it.type == "FIADO" }.sumOf { it.amount }
                "$count fiados  ·  Te deben: ${Money.cop(total)}"
            }
            else -> {
                val count = vigentes.size
                val neto = timeFiltered.sumOf { it.cashDelta() }
                "$count movimientos  ·  Neto: ${Money.cop(neto)}"
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .systemBarsPadding()
            .testTag("movements_screen")
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(14.dp))

            // Header: Back Arrow + Title
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("movements_back_arrow")) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = BarrioColors.Yellow,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Movimientos",
                    color = BarrioColors.Yellow,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("movements_title")
                )
            }

            Spacer(Modifier.height(16.dp))

            // Row 1: Time Filter Pills
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("Hoy", "Semana", "Mes").forEach { time ->
                    FilterPill(
                        label = time,
                        isSelected = selectedTimeFilter == time,
                        onClick = { selectedTimeFilter = time }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Row 2: Type Filter Pills
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("Todos", "Ventas", "Gastos", "Fiados").forEach { type ->
                    FilterPill(
                        label = type,
                        isSelected = selectedTypeFilter == type,
                        onClick = { selectedTypeFilter = type }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Summary Header
            Text(
                summaryText,
                color = BarrioColors.TextHint,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth().testTag("movements_summary_header")
            )

            Spacer(Modifier.height(14.dp))

            // List of Movement Cards
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(BarrioColors.CardBg)
                                .border(1.dp, BarrioColors.CardSubtle, RoundedCornerShape(16.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "No hay movimientos para este filtro",
                                    color = BarrioColors.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Los registros de ventas y gastos aparecerán aquí",
                                    color = BarrioColors.TextHint,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    items(filteredList, key = { it.movement.id }) { item ->
                        val anulado = item.movement.voided
                        val tachado = if (anulado) TextDecoration.LineThrough else TextDecoration.None
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (anulado) 0.5f else 1f)
                                .testTag("movement_card_${item.movement.id}")
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
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (item.type) {
                                                    "Venta" -> BarrioColors.PositiveGreen.copy(alpha = 0.2f)
                                                    "Gasto" -> BarrioColors.NegativeRed.copy(alpha = 0.2f)
                                                    else -> BarrioColors.Yellow.copy(alpha = 0.2f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            when (item.type) {
                                                "Venta" -> Icons.Filled.ArrowUpward
                                                "Gasto" -> Icons.Filled.ArrowDownward
                                                else -> Icons.Outlined.Person
                                            },
                                            contentDescription = null,
                                            tint = when (item.type) {
                                                "Venta" -> BarrioColors.PositiveGreen
                                                "Gasto" -> BarrioColors.NegativeRed
                                                else -> BarrioColors.Yellow
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            item.title,
                                            color = BarrioColors.TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textDecoration = tachado
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            item.timeKind,
                                            color = if (anulado) BarrioColors.Error else BarrioColors.TextHint,
                                            fontSize = 12.sp,
                                            fontWeight = if (anulado) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }

                                Text(
                                    item.amount,
                                    color = when (item.type) {
                                        "Venta" -> BarrioColors.PositiveGreen
                                        "Gasto" -> BarrioColors.NegativeRed
                                        else -> BarrioColors.Yellow
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = tachado
                                )

                                // Lápiz amarillo: corregir o eliminar (lo eliminado ya no se edita)
                                if (!anulado) {
                                    Box(
                                        Modifier
                                            .padding(start = 6.dp)
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .clickable { editando = item.movement }
                                            .testTag("edit_movement_${item.movement.id}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Edit, "Editar o eliminar", tint = Color(0xFFF2C849), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(90.dp)) }
            }
        }

        editando?.let { m ->
            EditMovementDialog(
                movement = m,
                onSave = { monto, concepto -> vm.editMovement(m.id, monto, concepto) },
                onDelete = { vm.voidMovement(m.id) },
                onDismiss = { editando = null },
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) BarrioColors.Yellow else BarrioColors.Surface)
            .border(
                1.dp,
                if (isSelected) BarrioColors.Yellow else BarrioColors.YellowStroke.copy(alpha = 0.4f),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = if (isSelected) BarrioColors.OnYellow else BarrioColors.TextHint,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
