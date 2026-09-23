package com.barriogo.app.ui.fiados

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.Money
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.Card
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.theme.Accent
import com.barriogo.app.ui.theme.OnAccent
import com.barriogo.app.ui.theme.SurfaceDarkAlt

/** F1 · Fiados: total pendiente y lista de clientes con deuda; buscar y registrar abonos. */
@Composable
fun FiadosScreen(onNewFiado: () -> Unit, onOpenClient: (Long) -> Unit) {
    val vm = barrioViewModel { FiadosViewModel(it) }
    val debts by vm.debts.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(debts, query) {
        if (query.isBlank()) debts else debts.filter { it.client.name.contains(query, ignoreCase = true) || it.client.phone.contains(query) }
    }
    val total = debts.sumOf { it.balance }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(12.dp)) }
        item { Text("Fiados", style = MaterialTheme.typography.headlineMedium) }
        item {
            Card {
                Text("Te deben en total", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Money.cop(total), style = com.barriogo.app.ui.theme.MoneyDisplay)
                Text("${debts.size} clientes con fiado pendiente", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { PrimaryButton("+ Nuevo fiado") { onNewFiado() } }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Buscar por nombre o teléfono") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    if (debts.isEmpty()) "Todavía no hay clientes con fiado." else "Sin resultados.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(filtered, key = { it.client.id }) { d ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SurfaceDarkAlt)
                    .clickable { onOpenClient(d.client.id) }.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                        Text(d.client.name.firstOrNull()?.uppercase() ?: "?", color = OnAccent, style = MaterialTheme.typography.titleMedium)
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(d.client.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Desde el ${d.lastMovement?.let { Dates.day(it.createdAt) } ?: ""}",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(Money.cop(d.balance), style = MaterialTheme.typography.titleMedium)
            }
        }
        item { Spacer(Modifier.height(90.dp)) }
    }
}
