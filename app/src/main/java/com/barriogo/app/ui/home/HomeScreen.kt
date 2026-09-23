package com.barriogo.app.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.cashDelta
import com.barriogo.app.domain.kind
import com.barriogo.app.domain.label
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.Card
import com.barriogo.app.ui.components.SectionHeader
import com.barriogo.app.ui.theme.Accent
import com.barriogo.app.ui.theme.NegativeCoral
import com.barriogo.app.ui.theme.PositiveGreen
import com.barriogo.app.ui.theme.SurfaceDarkAlt

/** H1 · Inicio: saldo del día, entradas/salidas, fiados pendientes y últimos movimientos. */
@Composable
fun HomeScreen(onSeeAllMovements: () -> Unit, onGoFiados: () -> Unit, onSwitchProfile: () -> Unit) {
    val vm = barrioViewModel { HomeViewModel(it) }
    val s by vm.state.collectAsState()
    val profile by vm.profile.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(12.dp)) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Accent),
                        contentAlignment = Alignment.Center,
                    ) { Text(profile?.initial ?: "?", color = MaterialTheme.colorScheme.background, style = MaterialTheme.typography.titleMedium) }
                    Column(Modifier.padding(start = 10.dp)) {
                        Text("Hola, ${profile?.name ?: ""}", style = MaterialTheme.typography.titleMedium)
                        Text(Dates.longDay(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row {
                    IconButton(onClick = { vm.toggleHide() }) {
                        Icon(if (s.hideAmounts) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = "Ocultar montos")
                    }
                    IconButton(onClick = onSwitchProfile) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = "Cambiar de perfil")
                    }
                }
            }
        }
        item {
            Card {
                Text("Saldo en caja hoy", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (s.hideAmounts) Money.HIDDEN else Money.cop(s.saldoHoy),
                    style = com.barriogo.app.ui.theme.MoneyDisplay,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                Text(
                    "Apertura del día: ${if (s.hideAmounts) Money.HIDDEN else Money.cop(s.session?.openingAmount ?: 0)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat(Modifier.weight(1f), "Entró", s.entradas, PositiveGreen, Icons.Filled.ArrowUpward, s.hideAmounts)
                MiniStat(Modifier.weight(1f), "Salió", s.salidas, NegativeCoral, Icons.Filled.ArrowDownward, s.hideAmounts)
            }
        }
        item {
            Card(color = SurfaceDarkAlt, modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp)).clickable(onClick = onGoFiados)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total fiados pendientes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (s.hideAmounts) Money.HIDDEN else Money.cop(s.fiadoTotal), style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        "${s.fiadoClients} clientes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
        }
        item { SectionHeader("Últimos movimientos", action = "Ver todos", onAction = onSeeAllMovements) }
        if (s.todayMovements.isEmpty()) {
            item {
                Text(
                    "Todavía no hay movimientos hoy.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        items(s.todayMovements.take(6)) { m ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(m.label(), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${Dates.time(m.createdAt)}  ·  ${m.detail.ifBlank { m.kind.name.lowercase().replaceFirstChar { c -> c.uppercase() } }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val delta = m.cashDelta()
                Text(
                    if (s.hideAmounts) Money.HIDDEN else Money.signed(if (delta != 0L) delta else m.amount),
                    color = if (delta < 0) NegativeCoral else PositiveGreen,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        item { Spacer(Modifier.height(90.dp)) }
    }
}

@Composable
private fun MiniStat(modifier: Modifier, label: String, amount: Long, color: androidx.compose.ui.graphics.Color, icon: androidx.compose.ui.graphics.vector.ImageVector, hidden: Boolean) {
    Card(modifier = modifier, color = SurfaceDarkAlt) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
        }
        Text(if (hidden) Money.HIDDEN else Money.cop(amount), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 2.dp))
    }
}
