package com.barriogo.app.ui.expenses

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.kind
import com.barriogo.app.domain.label
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.BarrioTopBar
import com.barriogo.app.ui.components.Card
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.movements.MovementsViewModel
import com.barriogo.app.ui.theme.NegativeCoral
import com.barriogo.app.ui.theme.SurfaceDarkAlt

/** G1 · Gastos: total del día por tipo y el historial de gastos registrados. */
@Composable
fun ExpensesScreen(onBack: () -> Unit, onNewExpense: () -> Unit) {
    val vm = barrioViewModel { MovementsViewModel(it) }
    val all by vm.movements.collectAsState()
    val gastos = all.filter { !it.voided && it.kind == MovementType.GASTO }
    val byKind = gastos.groupBy { it.detail.ifBlank { "Otros" } }

    Column(Modifier.fillMaxSize()) {
        BarrioTopBar("Gastos", onBack = onBack)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Spacer(Modifier.height(4.dp)) }
            item {
                Card {
                    Text("Total en gastos", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Money.cop(gastos.sumOf { it.amount }), style = com.barriogo.app.ui.theme.MoneyDisplay, color = NegativeCoral)
                }
            }
            item { PrimaryButton("+ Registrar gasto") { onNewExpense() } }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    byKind.entries.take(3).forEach { (k, ms) ->
                        Card(modifier = Modifier.weight(1f), color = SurfaceDarkAlt) {
                            Text(k, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.cop(ms.sumOf { it.amount }), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            if (gastos.isEmpty()) {
                item { Text("Todavía no hay gastos registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(gastos.sortedByDescending { it.createdAt }) { m ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(m.label(), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${Dates.day(m.createdAt)} · ${m.detail}",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("-" + Money.cop(m.amount), color = NegativeCoral, style = MaterialTheme.typography.titleMedium)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
