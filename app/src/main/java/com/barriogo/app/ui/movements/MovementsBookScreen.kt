package com.barriogo.app.ui.movements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.cashDelta
import com.barriogo.app.domain.kind
import com.barriogo.app.domain.label
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.BarrioTopBar
import com.barriogo.app.ui.components.StatusChip
import com.barriogo.app.ui.theme.Accent
import com.barriogo.app.ui.theme.NegativeCoral
import com.barriogo.app.ui.theme.OnAccent
import com.barriogo.app.ui.theme.PositiveGreen
import com.barriogo.app.ui.theme.SurfaceDark
import com.barriogo.app.ui.theme.SurfaceDarkAlt

private enum class Filter(val label: String, val type: MovementType?) {
    TODOS("Todos", null), VENTAS("Ventas", MovementType.VENTA), GASTOS("Gastos", MovementType.GASTO),
    FIADOS("Fiados", MovementType.FIADO), ABONOS("Abonos", MovementType.ABONO),
}

/** M2 · Libro de movimientos: historial completo con filtros. Los movimientos se anulan, no se borran. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovementsBookScreen(onBack: () -> Unit) {
    val vm = barrioViewModel { MovementsViewModel(it) }
    val all by vm.movements.collectAsState()
    var filter by remember { mutableStateOf(Filter.TODOS) }
    var toVoid by remember { mutableStateOf<Long?>(null) }

    val filtered = remember(all, filter) { if (filter.type == null) all else all.filter { it.kind == filter.type } }
    val grouped = remember(filtered) { filtered.groupBy { Dates.day(it.createdAt) } }

    Column(Modifier.fillMaxSize()) {
        BarrioTopBar(title = "Movimientos", onBack = onBack)
        LazyRow(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Filter.values().toList()) { f ->
                val selected = f == filter
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(if (selected) Accent else SurfaceDark)
                        .clickable { filter = f }.padding(horizontal = 14.dp, vertical = 8.dp),
                ) { Text(f.label, color = if (selected) OnAccent else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge) }
            }
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay movimientos en esta categoría.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                grouped.forEach { (day, items) ->
                    item {
                        Text(
                            day, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                        )
                    }
                    items(items, key = { it.id }) { m ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SurfaceDarkAlt)
                                .combinedClickable(onClick = {}, onLongClick = { if (!m.voided) toVoid = m.id })
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusChip(m.kind.name, colorFor(m.kind))
                                    Text(
                                        m.label(), style = MaterialTheme.typography.bodyLarge,
                                        textDecoration = if (m.voided) TextDecoration.LineThrough else null,
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                                Text(
                                    "${Dates.time(m.createdAt)} · ${if (m.voided) "Anulado" else m.profileName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            val delta = m.cashDelta()
                            Text(
                                Money.signed(if (delta != 0L) delta else m.amount),
                                color = if (m.voided) MaterialTheme.colorScheme.onSurfaceVariant else if (delta < 0) NegativeCoral else PositiveGreen,
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (m.voided) TextDecoration.LineThrough else null,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (toVoid != null) {
        AlertDialog(
            onDismissRequest = { toVoid = null },
            title = { Text("¿Anular este movimiento?") },
            text = { Text("Queda marcado como anulado y no se borra del historial. La caja y los saldos se actualizan.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { vm.voidMovement(toVoid!!); toVoid = null }) {
                    Text("Anular", color = NegativeCoral)
                }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { toVoid = null }) { Text("Cancelar") } },
        )
    }
}

private fun colorFor(t: MovementType) = when (t) {
    MovementType.VENTA -> PositiveGreen
    MovementType.GASTO -> NegativeCoral
    MovementType.FIADO -> Accent
    MovementType.ABONO -> com.barriogo.app.ui.theme.InfoBlue
    MovementType.AJUSTE -> com.barriogo.app.ui.theme.TextMuted
}
