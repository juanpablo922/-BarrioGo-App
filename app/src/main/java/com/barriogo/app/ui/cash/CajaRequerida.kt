package com.barriogo.app.ui.cash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.domain.EstadoCaja
import com.barriogo.app.domain.ReglasCaja
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.BotonFigma
import com.barriogo.app.ui.components.CabeceraFigma
import com.barriogo.app.ui.components.TextLinkButton
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.FigmaColors

/**
 * Envuelve las pantallas de registro (ventas, gastos, fiados, voz, escáner): sin la caja de HOY
 * abierta no se registra nada y se ofrece abrirla (o cerrar la de un día anterior).
 */
@Composable
fun CajaRequerida(
    titulo: String,
    onBack: () -> Unit,
    onAbrirCaja: () -> Unit,
    onCerrarCaja: () -> Unit,
    contenido: @Composable () -> Unit,
) {
    val vm = barrioViewModel { CashViewModel(it) }
    val s by vm.state.collectAsState()
    if (!s.cargado) return
    if (ReglasCaja.permiteRegistrar(s.estado)) {
        contenido()
        return
    }
    val (mensaje, detalle, boton, accion) = when (s.estado) {
        EstadoCaja.ABIERTA_DIA_ANTERIOR -> Cuatro(
            "La caja de un día anterior sigue abierta",
            "Ciérrala con el conteo del efectivo y abre la caja de hoy para seguir registrando.",
            "Cerrar esa caja",
            onCerrarCaja,
        )
        EstadoCaja.CERRADA_HOY -> Cuatro(
            "La caja de hoy ya está cerrada",
            "No se pueden registrar ventas, gastos ni fiados hasta abrir una nueva caja mañana.",
            null,
            {},
        )
        else -> Cuatro(
            "Primero abre la caja del día",
            "Sin la apertura no se pueden registrar ventas, gastos ni fiados. Escribe con cuánto dinero empiezas.",
            "Abrir caja",
            onAbrirCaja,
        )
    }
    Column(Modifier.fillMaxSize().background(BarrioColors.Background).statusBarsPadding().testTag("caja_requerida")) {
        CabeceraFigma(titulo, onBack = onBack)
        Column(
            Modifier.weight(1f).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.PointOfSale, null, tint = FigmaColors.Amarillo, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text(mensaje, color = FigmaColors.Crema, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(detalle, color = FigmaColors.Cuerpo, fontSize = 15.sp, textAlign = TextAlign.Center)
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (boton != null) BotonFigma(boton, modifier = Modifier.testTag("caja_requerida_accion"), onClick = accion)
            TextLinkButton("Volver", color = FigmaColors.Dorado, onClick = onBack)
        }
    }
}

private data class Cuatro(val mensaje: String, val detalle: String, val boton: String?, val accion: () -> Unit)
