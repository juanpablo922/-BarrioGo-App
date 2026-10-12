package com.barriogo.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/**
 * H1 · Inicio: Saldo en caja, Entró/Salió, Fiados y Últimos movimientos (Figma exacto).
 */
@Composable
fun HomeScreen(
    onSeeAllMovements: () -> Unit,
    onGoFiados: () -> Unit,
    onSwitchProfile: () -> Unit,
    onEditCash: () -> Unit = {}
) {
    val vm = barrioViewModel { HomeViewModel(it) }
    val s by vm.state.collectAsState()
    val profile by vm.profile.collectAsState()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .padding(horizontal = 20.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(14.dp)) }

        // Header: Profile Avatar, Greeting, Role + Date, Eye Icon
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onSwitchProfile).testTag("switch_profile_badge")
                ) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(BarrioColors.Surface)
                            .border(1.5.dp, BarrioColors.YellowStroke, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            profile?.initial ?: "J",
                            color = BarrioColors.TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Hola, ${profile?.name ?: "Don Juan"}",
                            color = BarrioColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Administrador · ${Dates.longDay()}",
                            color = BarrioColors.TextHint,
                            fontSize = 13.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.barriogo.app.BarrioGoApp
                    IconButton(
                        onClick = {
                            val curr = app.container.store.darkTheme.value
                            app.container.store.setDark(!curr)
                        },
                        modifier = Modifier.testTag("toggle_theme_home")
                    ) {
                        Icon(
                            if (BarrioColors.isDark) Icons.Filled.WbSunny else Icons.Filled.DarkMode,
                            contentDescription = "Cambiar tema claro/oscuro",
                            tint = BarrioColors.Yellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { vm.toggleHide() },
                        modifier = Modifier.testTag("toggle_amounts_visibility")
                    ) {
                        Icon(
                            if (s.hideAmounts) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Ocultar montos",
                            tint = BarrioColors.Yellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Card 1: Saldo en caja hoy
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BarrioColors.CardBg)
                    .border(1.dp, BarrioColors.CardSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp)
                    .testTag("saldo_caja_card")
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Saldo en caja hoy",
                            color = BarrioColors.TextHint,
                            fontSize = 14.sp
                        )
                        IconButton(
                            onClick = onEditCash,
                            modifier = Modifier.size(28.dp).testTag("edit_cash_pencil_button")
                        ) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Editar saldo",
                                tint = BarrioColors.Yellow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        if (s.hideAmounts) Money.HIDDEN else Money.cop(s.saldoHoy),
                        color = BarrioColors.Yellow,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Apertura del día: ${if (s.hideAmounts) Money.HIDDEN else Money.cop(s.session?.openingAmount ?: 0L)}",
                        color = BarrioColors.TextHint,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Row of Two Stats Cards: Entró & Salió
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Entró
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
                            Icon(
                                Icons.Filled.ArrowUpward,
                                contentDescription = null,
                                tint = BarrioColors.PositiveGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Entró", color = BarrioColors.TextHint, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (s.hideAmounts) Money.HIDDEN else Money.cop(s.entradas),
                            color = BarrioColors.TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Salió
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
                            Icon(
                                Icons.Filled.ArrowDownward,
                                contentDescription = null,
                                tint = BarrioColors.NegativeRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Salió", color = BarrioColors.TextHint, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (s.hideAmounts) Money.HIDDEN else Money.cop(s.salidas),
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
                    .clickable(onClick = onGoFiados)
                    .padding(18.dp)
                    .testTag("fiados_summary_card")
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Total fiados pendientes",
                            color = BarrioColors.TextHint,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (s.hideAmounts) Money.HIDDEN else Money.cop(s.fiadoTotal),
                            color = BarrioColors.TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BarrioColors.Surface)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${s.fiadoClients} clientes",
                            color = BarrioColors.TextHint,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
                Text(
                    "Últimos movimientos de caja",
                    color = BarrioColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Ver todos",
                    color = BarrioColors.Yellow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onSeeAllMovements).testTag("see_all_movements_link")
                )
            }
        }

        val movementItems = s.todayMovements.take(5).map { m ->
            val isIncome = m.cashDelta() >= 0
            val prefix = if (isIncome) "+ " else "- "
            val formatted = prefix + Money.cop(m.amount)
            val subtitle = "${Dates.time(m.createdAt)} · ${m.type.lowercase().replaceFirstChar { it.uppercase() }}"
            MovementUiModel(
                title = m.label(),
                subtitle = subtitle,
                amount = formatted,
                isIncome = isIncome
            )
        }

        if (movementItems.isEmpty()) {
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
                        Text(
                            "Sin movimientos registrados hoy",
                            color = BarrioColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Toca \"+ Registrar\" abajo para tu primera venta",
                            color = BarrioColors.TextHint,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(movementItems) { m ->
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
                                    contentDescription = null,
                                    tint = if (m.isIncome) BarrioColors.PositiveGreen else BarrioColors.NegativeRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    m.title,
                                    color = BarrioColors.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    m.subtitle,
                                    color = BarrioColors.TextHint,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Text(
                            if (s.hideAmounts) Money.HIDDEN else m.amount,
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
}

data class MovementUiModel(
    val title: String,
    val subtitle: String,
    val amount: String,
    val isIncome: Boolean
)
