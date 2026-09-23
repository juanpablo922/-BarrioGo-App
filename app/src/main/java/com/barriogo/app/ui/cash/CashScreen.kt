package com.barriogo.app.ui.cash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.cashDelta
import com.barriogo.app.domain.label
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AmountField
import com.barriogo.app.ui.components.Card
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.components.SecondaryButton
import com.barriogo.app.ui.components.SectionHeader
import com.barriogo.app.ui.theme.NegativeCoral
import com.barriogo.app.ui.theme.PositiveGreen
import com.barriogo.app.ui.theme.SurfaceDarkAlt

/** C1 · Caja: saldo del día, apertura, ventas/gastos y accesos a arqueo y cierre. */
@Composable
fun CashScreen(onOpenCash: () -> Unit, onSeeAllMovements: () -> Unit) {
    val vm = barrioViewModel { CashViewModel(it) }
    val s by vm.state.collectAsState()
    var showArqueo by remember { mutableStateOf(false) }
    var showCierre by remember { mutableStateOf(false) }
    var showEditar by remember { mutableStateOf(false) }

    if (s.session == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("La caja está cerrada", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Ábrela para empezar a registrar ventas, gastos y fiados de hoy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )
            PrimaryButton("Abrir caja") { onOpenCash() }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Spacer(Modifier.height(12.dp)) }
        item { Text("Caja", style = MaterialTheme.typography.headlineMedium) }
        item {
            Card {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Dinero en caja hoy", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.cop(s.saldo), style = com.barriogo.app.ui.theme.MoneyDisplay)
                        Row {
                            Text(
                                "Apertura del día: ${Money.cop(s.session?.openingAmount ?: 0)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (s.editsLeft > 0) {
                                Text(
                                    "  ·  Editar",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable { showEditar = true },
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f), color = SurfaceDarkAlt) {
                    Text("Ventas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Money.cop(s.entradas), style = MaterialTheme.typography.titleLarge, color = PositiveGreen)
                }
                Card(modifier = Modifier.weight(1f), color = SurfaceDarkAlt) {
                    Text("Gastos", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("-" + Money.cop(s.salidas), style = MaterialTheme.typography.titleLarge, color = NegativeCoral)
                }
            }
        }
        item {
            Card(color = SurfaceDarkAlt) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total fiados pendientes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Money.cop(s.fiadoTotal), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton("Arqueo de caja", modifier = Modifier.weight(1f)) { showArqueo = true }
                PrimaryButton("Cerrar caja", modifier = Modifier.weight(1f)) { showCierre = true }
            }
        }
        item { SectionHeader("Últimos movimientos de caja", action = "Ver todos", onAction = onSeeAllMovements) }
        items(s.todayMovements.take(6)) { m ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(m.label(), style = MaterialTheme.typography.bodyLarge)
                    Text(Dates.time(m.createdAt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val delta = m.cashDelta()
                Text(Money.signed(delta), color = if (delta < 0) NegativeCoral else PositiveGreen, style = MaterialTheme.typography.titleMedium)
            }
        }
        item { Spacer(Modifier.height(90.dp)) }
    }

    if (showEditar) {
        var newAmount by remember { mutableLongStateOf(s.session?.openingAmount ?: 0L) }
        AlertDialog(
            onDismissRequest = { showEditar = false },
            title = { Text("Editar dinero en caja") },
            text = {
                Column {
                    Text("Te quedan ${s.editsLeft} cambios hoy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = newAmount, onValueChange = { newAmount = it })
                }
            },
            confirmButton = {
                PrimaryButton("Guardar", modifier = Modifier.padding(8.dp)) {
                    vm.editOpening(newAmount) { showEditar = false }
                }
            },
            dismissButton = { SecondaryButton("Cancelar", modifier = Modifier.padding(8.dp)) { showEditar = false } },
        )
    }
    if (showArqueo) {
        CountDialog(
            title = "Arqueo de caja", subtitle = "Cuenta el dinero físico y compáralo con lo esperado.",
            expected = s.saldo, confirmLabel = "Guardar arqueo",
            onDismiss = { showArqueo = false },
            onConfirm = { counted, note -> vm.saveArqueo(counted, note) { showArqueo = false } },
        )
    }
    if (showCierre) {
        CountDialog(
            title = "Cerrar caja", subtitle = "Este es el resumen final del día.",
            expected = s.saldo, confirmLabel = "Cerrar caja",
            onDismiss = { showCierre = false },
            onConfirm = { counted, note -> vm.closeCash(counted, note) { showCierre = false } },
        )
    }
}

@Composable
private fun CountDialog(title: String, subtitle: String, expected: Long, confirmLabel: String, onDismiss: () -> Unit, onConfirm: (Long, String) -> Unit) {
    var counted by remember { mutableLongStateOf(expected) }
    var note by remember { mutableStateOf("") }
    val diff = counted - expected
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text("Esperado: ${Money.cop(expected)}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                AmountField(value = counted, onValueChange = { counted = it }, label = "Contado")
                if (diff != 0L) {
                    Text(
                        if (diff > 0) "Sobran ${Money.cop(diff)}" else "Faltan ${Money.cop(-diff)}",
                        color = if (diff > 0) PositiveGreen else NegativeCoral,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Nota (opcional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { PrimaryButton(confirmLabel, modifier = Modifier.padding(8.dp)) { onConfirm(counted, note) } },
        dismissButton = { SecondaryButton("Cancelar", modifier = Modifier.padding(8.dp)) { onDismiss() } },
    )
}
