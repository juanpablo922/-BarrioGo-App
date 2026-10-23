package com.barriogo.app.ui.expenses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.data.movimientos.GastosRepository
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.ExpenseKind
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.PeriodoGastos
import com.barriogo.app.domain.ReglasGasto
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AvisoDialogo
import com.barriogo.app.ui.components.AvisoFlotante
import com.barriogo.app.ui.components.CabeceraFigma
import com.barriogo.app.ui.components.LoadingOverlay
import com.barriogo.app.ui.components.MensajeFlotante
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.FigmaColors
import java.time.LocalDate

/**
 * G1 · Gastos: total del periodo (Hoy / Semana / Mes), lista, detalle en hoja inferior,
 * editar y anular (con confirmación). Estados del Figma: Lista, Detalle, Anular, Anulado y Vacío.
 */
@Composable
fun ExpensesScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    /** Mensaje que deja G2 al guardar ("Gasto guardado…"). */
    avisoInicial: String? = null,
) {
    val vm = barrioViewModel { GastosViewModel(it) }
    val s by vm.state.collectAsState()
    val ocupado by vm.ocupado.collectAsState()
    val error by vm.error.collectAsState()

    var detalleId by rememberSaveable { mutableStateOf<Long?>(null) }
    var anularId by rememberSaveable { mutableStateOf<Long?>(null) }
    var mensaje by remember { mutableStateOf(avisoInicial?.let { MensajeFlotante(it) }) }
    val detalle = s.lista.firstOrNull { it.id == detalleId }
    val anular = s.lista.firstOrNull { it.id == anularId }

    BackHandler(enabled = detalle != null) { detalleId = null }

    Box(Modifier.fillMaxSize().background(BarrioColors.Background).statusBarsPadding().testTag("expenses_screen")) {
        Column(Modifier.fillMaxSize()) {
            CabeceraFigma("Gastos", onBack = onBack)
            Resumen(s.periodo, s.total, s.activos.size)
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodoGastos.entries.forEach { p -> Filtro(p.etiqueta, p == s.periodo) { vm.elegirPeriodo(p) } }
            }
            if (s.cargado && s.lista.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (s.periodo == PeriodoGastos.HOY) "Todavía no has registrado gastos" else "No hay gastos en este periodo",
                        color = FigmaColors.Crema, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("expenses_empty"),
                    )
                    Text(
                        "Cada compra a proveedor, servicio o transporte que registres aparecerá aquí.",
                        color = FigmaColors.Cuerpo, fontSize = 14.sp, textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                    items(s.lista, key = { it.id }) { g ->
                        FilaGasto(g, conFecha = s.periodo != PeriodoGastos.HOY, ultima = g == s.lista.last()) { detalleId = g.id }
                    }
                    item { Spacer(Modifier.height(112.dp)) }
                }
            }
        }

        AvisoFlotante(mensaje, onTerminar = { mensaje = null })

        if (detalle != null) {
            HojaDetalle(
                g = detalle,
                editable = s.editable(detalle),
                onCerrar = { detalleId = null },
                onEditar = {
                    detalleId = null
                    onEdit(detalle.id)
                },
                onAnular = { anularId = detalle.id },
            )
        }
        if (anular != null) {
            AvisoDialogo(
                titulo = "¿Anular este gasto?",
                mensaje = "${anular.title} por ${Money.cop(anular.amount)} quedará tachado y dejará de restar en tu caja. Seguirá visible en el historial.",
                principal = "Anular",
                onPrincipal = {
                    anularId = null
                    vm.anular(anular.id) {
                        detalleId = null
                        mensaje = MensajeFlotante("Gasto anulado. Tu caja se actualizó.")
                    }
                },
                secundario = "Conservar",
                onSecundario = { anularId = null },
                principalPeligro = true,
            )
        }
        error?.let { AvisoDialogo(titulo = "No se pudo completar", mensaje = it, principal = "Entendido", onPrincipal = vm::cerrarError) }
        ocupado?.let { LoadingOverlay(it) }
    }
}

@Composable
private fun Resumen(periodo: PeriodoGastos, total: Long, cantidad: Int) {
    Column(
        Modifier
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FigmaColors.Tarjeta)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("expenses_summary"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            when (periodo) {
                PeriodoGastos.HOY -> "Gastos de hoy"
                PeriodoGastos.SEMANA -> "Gastos de la semana"
                PeriodoGastos.MES -> "Gastos del mes"
            },
            color = FigmaColors.Dorado, fontSize = 13.sp,
        )
        Text(Money.cop(total), color = FigmaColors.Naranja, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("expenses_total"))
        Text(
            when (cantidad) {
                0 -> if (periodo == PeriodoGastos.HOY) "Sin gastos hoy" else "Sin gastos"
                1 -> "1 gasto registrado"
                else -> "$cantidad gastos registrados"
            },
            color = FigmaColors.Cuerpo, fontSize = 13.sp,
        )
    }
}

@Composable
private fun Filtro(texto: String, elegido: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(999.dp)
    Text(
        texto,
        color = if (elegido) FigmaColors.SobreAmarillo else FigmaColors.Crema,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(forma)
            .background(if (elegido) FigmaColors.AmarilloBoton else FigmaColors.Tarjeta)
            .then(if (elegido) Modifier else Modifier.border(1.dp, FigmaColors.Dorado, forma))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("filtro_$texto"),
    )
}

internal fun iconoGasto(tipo: ExpenseKind): ImageVector = when (tipo) {
    ExpenseKind.PROVEEDOR -> Icons.AutoMirrored.Outlined.TrendingUp
    ExpenseKind.SERVICIOS -> Icons.AutoMirrored.Outlined.ReceiptLong
    ExpenseKind.OTROS -> Icons.Outlined.MoreHoriz
}

@Composable
private fun FilaGasto(g: MovementEntity, conFecha: Boolean, ultima: Boolean, onClick: () -> Unit) {
    val tipo = ReglasGasto.tipoDe(g.category, g.detail)
    val tachado = if (g.voided) TextDecoration.LineThrough else null
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("gasto_${g.id}")) {
        Row(
            Modifier.fillMaxWidth().height(100.dp).padding(vertical = 14.dp).alpha(if (g.voided) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(FigmaColors.Tarjeta), contentAlignment = Alignment.Center) {
                Icon(iconoGasto(tipo), null, tint = FigmaColors.Naranja, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(g.title, color = FigmaColors.Crema, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textDecoration = tachado, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    buildString {
                        if (conFecha) append(Dates.day(g.createdAt)).append(" · ")
                        append(Dates.time(g.createdAt)).append(" · ")
                        append(if (g.voided) "Anulado" else tipo.label)
                        if (g.editedAt != null && !g.voided) append(" · Editado")
                    },
                    color = FigmaColors.Dorado, fontSize = 12.sp,
                )
            }
            Text("−" + Money.cop(g.amount), color = FigmaColors.Naranja, fontSize = 15.sp, fontWeight = FontWeight.Bold, textDecoration = tachado)
        }
        if (!ultima) Box(Modifier.fillMaxWidth().height(1.dp).background(FigmaColors.Tarjeta))
    }
}

/** "Hoja · Detalle": velo negro al 55 % y hoja #22593F con asa, datos y acciones. */
@Composable
private fun HojaDetalle(g: MovementEntity, editable: Boolean, onCerrar: () -> Unit, onEditar: () -> Unit, onAnular: () -> Unit) {
    val tipo = ReglasGasto.tipoDe(g.category, g.detail)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCerrar,
        ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(FigmaColors.Tarjeta)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp)
                .testTag("expense_detail"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(FigmaColors.Dorado).align(Alignment.CenterHorizontally))
            Text("Gasto · ${tipo.label}", color = FigmaColors.Dorado, fontSize = 13.sp)
            Text(g.title, color = FigmaColors.Crema, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "−" + Money.cop(g.amount), color = FigmaColors.Naranja, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                textDecoration = if (g.voided) TextDecoration.LineThrough else null,
            )
            val dia = if (Dates.localDate(g.createdAt) == LocalDate.now()) "Hoy" else Dates.day(g.createdAt)
            Text("$dia ${Dates.time(g.createdAt)} · Registrado por ${g.profileName}", color = FigmaColors.Cuerpo, fontSize = 13.sp)
            g.originalAmount?.let { Text("Valor original: ${Money.cop(it)}", color = FigmaColors.Tenue, fontSize = 13.sp) }
            when {
                g.voided -> Text("Anulado: ya no resta en la caja.", color = FigmaColors.Naranja, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                !editable -> Text(GastosRepository.CAJA_CERRADA, color = FigmaColors.Tenue, fontSize = 14.sp)
                else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BotonHoja("Editar", relleno = true, onClick = onEditar)
                    BotonHoja("Anular gasto", relleno = false, onClick = onAnular)
                }
            }
        }
    }
}

@Composable
private fun BotonHoja(texto: String, relleno: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(forma)
            .then(if (relleno) Modifier.background(FigmaColors.AmarilloBoton) else Modifier.border(1.dp, FigmaColors.Naranja, forma))
            .clickable(onClick = onClick)
            .testTag("detalle_$texto"),
        contentAlignment = Alignment.Center,
    ) { Text(texto, color = if (relleno) FigmaColors.SobreAmarillo else FigmaColors.Naranja, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}
