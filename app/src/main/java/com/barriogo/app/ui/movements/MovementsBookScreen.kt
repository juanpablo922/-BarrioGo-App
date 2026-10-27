package com.barriogo.app.ui.movements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.cashDelta
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.CabeceraFigma
import com.barriogo.app.ui.components.FilaMovimientoFigma
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.FigmaColors
import kotlin.math.abs

/**
 * M2 · Movimientos (Figma): filtros de tiempo y tipo, resumen con el neto y filas planas.
 * El lápiz de cada fila abre corregir/eliminar (lo eliminado queda tachado y ya no se edita).
 */
@Composable
fun MovementsBookScreen(onBack: () -> Unit) {
    val vm = barrioViewModel { MovementsViewModel(it) }
    val allMovements by vm.movements.collectAsState()

    var selectedTimeFilter by remember { mutableStateOf("Hoy") }
    var selectedTypeFilter by remember { mutableStateOf("Todos") }
    var editando by remember { mutableStateOf<MovementEntity?>(null) }

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
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .systemBarsPadding()
            .testTag("movements_screen")
    ) {
        Column(Modifier.fillMaxSize()) {
            CabeceraFigma("Movimientos", onBack = onBack)

            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Hoy", "Semana", "Mes").forEach { time ->
                        FilterPill(time, selectedTimeFilter == time) { selectedTimeFilter = time }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Todos", "Ventas", "Gastos", "Fiados").forEach { type ->
                        FilterPill(type, selectedTypeFilter == type) { selectedTypeFilter = type }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Resumen(timeFiltered.filter { !it.voided }, selectedTimeFilter, selectedTypeFilter)
                Spacer(Modifier.height(4.dp))
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
            ) {
                if (filteredList.isEmpty()) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(FigmaColors.Tarjeta)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("No hay movimientos para este filtro", color = FigmaColors.Crema, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("Los registros de ventas y gastos aparecerán aquí", color = FigmaColors.Cuerpo, fontSize = 13.sp)
                        }
                    }
                } else {
                    itemsIndexed(filteredList, key = { _, m -> m.id }) { i, m ->
                        FilaMovimientoFigma(
                            m,
                            divisor = i != filteredList.lastIndex,
                            modifier = Modifier.testTag("movement_card_${m.id}"),
                            nota = estado(m),
                            // Lápiz amarillo: corregir o eliminar (lo eliminado ya no se edita)
                            accion = if (m.voided) null else ({
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .clickable { editando = m }
                                        .testTag("edit_movement_${m.id}"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Edit, "Editar o eliminar", tint = FigmaColors.AmarilloBoton, modifier = Modifier.size(20.dp))
                                }
                            }),
                        )
                    }
                }
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

/** " · Eliminado 10:40" o " · Editado (antes $ 5.000)". */
private fun estado(m: MovementEntity): String = when {
    m.voided -> " · Eliminado" + (m.voidedAt?.let { " " + Dates.time(it) } ?: "")
    m.editedAt != null -> " · Editado" + (m.originalAmount?.let { " (antes " + Money.cop(it) + ")" } ?: "")
    else -> ""
}

private data class LineaResumen(val izquierda: String, val etiqueta: String, val valor: String, val color: Color)

/** "6 movimientos hoy" a la izquierda y "Neto  +$ 29.000" (o el total del tipo) a la derecha. */
@Composable
private fun Resumen(vigentes: List<MovementEntity>, tiempo: String, tipo: String) {
    val cuando = when (tiempo) {
        "Semana" -> "esta semana"
        "Mes" -> "este mes"
        else -> "hoy"
    }
    fun suma(t: String) = vigentes.filter { it.type == t }.sumOf { it.amount }
    fun cuenta(t: String) = vigentes.count { it.type == t }
    fun plural(n: Int, uno: String, varios: String) = "$n ${if (n == 1) uno else varios} $cuando"

    val r = when (tipo) {
        "Ventas" -> LineaResumen(plural(cuenta("VENTA"), "venta", "ventas"), "Total", "+" + Money.cop(suma("VENTA")), FigmaColors.Verde)
        "Gastos" -> LineaResumen(plural(cuenta("GASTO"), "gasto", "gastos"), "Total", "−" + Money.cop(suma("GASTO")), FigmaColors.Naranja)
        "Fiados" -> LineaResumen(plural(cuenta("FIADO"), "fiado", "fiados"), "Fiado", Money.cop(suma("FIADO")), FigmaColors.Amarillo)
        else -> {
            val neto = vigentes.sumOf { it.cashDelta() }
            LineaResumen(
                plural(vigentes.size, "movimiento", "movimientos"), "Neto",
                (if (neto < 0) "−" else "+") + Money.cop(abs(neto)),
                if (neto < 0) FigmaColors.Naranja else FigmaColors.Verde,
            )
        }
    }
    Row(
        Modifier.fillMaxWidth().testTag("movements_summary_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(r.izquierda, color = FigmaColors.Cuerpo, fontSize = 13.sp)
        Text("${r.etiqueta}  ${r.valor}", color = r.color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Píldora de filtro: la elegida amarilla con texto oscuro; las demás verdes con borde dorado. */
@Composable
private fun FilterPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(999.dp)
    Box(
        Modifier
            .height(32.dp)
            .clip(forma)
            .background(if (isSelected) FigmaColors.AmarilloBoton else FigmaColors.Tarjeta)
            .border(1.dp, if (isSelected) FigmaColors.AmarilloBoton else FigmaColors.Dorado, forma)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .testTag("filtro_$label"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (isSelected) FigmaColors.SobreAmarillo else FigmaColors.Crema,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}
