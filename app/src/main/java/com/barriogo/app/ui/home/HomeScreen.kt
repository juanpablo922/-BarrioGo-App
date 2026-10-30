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
import com.barriogo.app.domain.AvisoCierre
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.EstadoCaja
import com.barriogo.app.ui.cash.CashViewModel
import com.barriogo.app.ui.cash.TarjetaCifra
import com.barriogo.app.ui.cash.TarjetaSaldo
import com.barriogo.app.ui.cash.FranjaAviso
import com.barriogo.app.ui.components.FilaMovimientoFigma
import com.barriogo.app.ui.components.BotonFigma
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.cashDelta
import com.barriogo.app.domain.label
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.FigmaColors
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp

/**
 * H1 · Inicio: Saldo en caja, Entró/Salió, Fiados y Últimos movimientos (Figma exacto).
 */
@Composable
fun HomeScreen(
    onSeeAllMovements: () -> Unit,
    onGoFiados: () -> Unit,
    onSwitchProfile: () -> Unit,
    onEditCash: () -> Unit = {},
    onOpenCash: () -> Unit = {},
    onCloseCash: () -> Unit = {},
    onGoExpenses: () -> Unit = {},
) {
    val vm = barrioViewModel { HomeViewModel(it) }
    val s by vm.state.collectAsState()
    val profile by vm.profile.collectAsState()
    // Saldo, entradas y salidas salen de la caja abierta (no de "lo de hoy"), igual que en Caja y en el cierre.
    val caja by barrioViewModel { CashViewModel(it) }.state.collectAsState()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .padding(horizontal = 16.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(14.dp)) }

        // Encabezado: círculo amarillo con la inicial, saludo, rol + fecha y el ojo en un círculo
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onSwitchProfile).testTag("switch_profile_badge")
                ) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(FigmaColors.AmarilloBoton),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(profile?.initial ?: "J", color = FigmaColors.SobreAmarillo, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Hola, ${profile?.name ?: "Don Juan"}",
                            color = FigmaColors.Crema,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${if (profile?.isAdmin == false) "Vendedor" else "Administrador"} · ${Dates.longDay()}",
                            color = FigmaColors.Dorado,
                            fontSize = 13.sp,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.barriogo.app.BarrioGoApp
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(FigmaColors.Tarjeta)
                        .clickable { app.container.store.setDark(!app.container.store.darkTheme.value) }
                        .testTag("toggle_theme_home"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (BarrioColors.isDark) Icons.Filled.WbSunny else Icons.Filled.DarkMode,
                        contentDescription = "Cambiar tema claro/oscuro",
                        tint = FigmaColors.Amarillo,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(FigmaColors.Tarjeta)
                        .clickable { vm.toggleHide() }
                        .testTag("toggle_amounts_visibility"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (s.hideAmounts) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = "Ocultar montos",
                        tint = FigmaColors.Amarillo,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Avisos de la caja: hora de cierre, caja de ayer abierta o caja sin abrir
        caja.textoAviso?.let { texto ->
            item { FranjaAviso(texto, vencido = caja.aviso is AvisoCierre.Vencido, esAdmin = caja.esAdmin, onCerrar = onCloseCash) }
        }
        if (caja.cargado && caja.estado == EstadoCaja.ABIERTA_DIA_ANTERIOR && caja.session != null) {
            item {
                FranjaAviso(
                    "La caja del ${Dates.day(caja.session!!.openedAt)} sigue abierta. Ciérrala para abrir la de hoy.",
                    vencido = true, esAdmin = caja.esAdmin, onCerrar = onCloseCash,
                )
            }
        }
        if (caja.cargado && caja.estado == EstadoCaja.SIN_ABRIR) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BarrioColors.CardBg)
                        .border(1.dp, BarrioColors.YellowStroke, RoundedCornerShape(20.dp)).padding(20.dp).testTag("open_cash_card"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Abre la caja del día", color = BarrioColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Sin la apertura no se pueden registrar ventas, gastos ni fiados.", color = BarrioColors.TextHint, fontSize = 14.sp)
                    BotonFigma("Abrir caja", modifier = Modifier.testTag("open_cash_home"), onClick = onOpenCash)
                }
            }
        }

        // Card 1: Saldo en caja hoy (H1; con la apertura bloqueada: candado y "Sin más cambios hoy")
        item {
            TarjetaSaldo(
                etiqueta = "Saldo en caja hoy",
                saldo = if (s.hideAmounts) Money.HIDDEN else Money.cop(caja.saldo),
                apertura = "Apertura del día: ${if (s.hideAmounts) Money.HIDDEN else Money.cop(caja.session?.openingAmount ?: 0L)}",
                cambiosRestantes = caja.session?.let { caja.editsLeft },
                onEditar = onEditCash,
                modifier = Modifier.testTag("saldo_caja_card"),
            )
        }

        // Entró y Salió (Salió es un enlace a G1 · Gastos)
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaCifra(
                    "Entró", if (s.hideAmounts) Money.HIDDEN else Money.cop(caja.entradas),
                    Icons.AutoMirrored.Filled.TrendingUp, FigmaColors.Verde, Modifier.weight(1f),
                )
                TarjetaCifra(
                    "Salió", if (s.hideAmounts) Money.HIDDEN else Money.cop(caja.salidas),
                    Icons.AutoMirrored.Filled.TrendingDown, FigmaColors.Naranja, Modifier.weight(1f).testTag("salio_card"),
                    onClick = onGoExpenses,
                )
            }
        }

        // Total fiados pendientes: tarjeta #22593F con la píldora "4 clientes" (abre F1 · Fiados)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(FigmaColors.Tarjeta)
                    .clickable(onClick = onGoFiados)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("fiados_summary_card"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Total fiados pendientes", color = FigmaColors.Dorado, fontSize = 13.sp)
                    Text(
                        if (s.hideAmounts) Money.HIDDEN else Money.cop(s.fiadoTotal),
                        color = FigmaColors.Crema,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    if (s.fiadoClients == 1) "1 cliente" else "${s.fiadoClients} clientes",
                    color = FigmaColors.Crema,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(BarrioColors.Background).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        // Últimos movimientos de caja: título dorado, "Ver todos" amarillo y filas planas con divisor (igual que Caja)
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Últimos movimientos de caja", color = FigmaColors.Dorado, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Ver todos",
                    color = FigmaColors.Amarillo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onSeeAllMovements).testTag("see_all_movements_link")
                )
            }
        }

        val ultimos = s.todayMovements.take(5)
        if (ultimos.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(FigmaColors.Tarjeta).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Sin movimientos registrados hoy", color = FigmaColors.Crema, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Toca \"+ Registrar\" abajo para tu primera venta", color = FigmaColors.Cuerpo, fontSize = 13.sp)
                }
            }
        } else {
            item {
                // Sin el espacio de 16 entre filas: van pegadas, separadas solo por el divisor
                Column(Modifier.fillMaxWidth()) {
                    ultimos.forEach { m ->
                        FilaMovimientoFigma(m, divisor = m != ultimos.last(), ocultarMonto = s.hideAmounts)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(90.dp)) }
    }
}
