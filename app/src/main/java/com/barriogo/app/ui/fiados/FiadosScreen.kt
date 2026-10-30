package com.barriogo.app.ui.fiados

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.domain.FiadoPendiente
import com.barriogo.app.domain.Fiados
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.kind
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AvisoDialogo
import com.barriogo.app.ui.components.BotonFigma
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.barriogo.app.ui.components.CabeceraFigma
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.FigmaColors

/**
 * F1 · Fiados: cuánto te deben en total, "+ Nuevo fiado", buscador y la lista de clientes que deben
 * (del que más debe al que menos). Tocar un cliente abre la hoja "Fiado pendiente": marcar como
 * pagado (todo en efectivo a la caja; los fiados quedan "pagado", no se borran) o ir a la ficha
 * para editar o anular un fiado registrado por error.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiadosScreen(
    onNewFiado: () -> Unit,
    onOpenClient: (Long) -> Unit,
    onBack: () -> Unit = {},
) {
    val vm = barrioViewModel { FiadosViewModel(it) }
    val pendientes by vm.pendientes.collectAsState()
    val movimientos by vm.movimientos.collectAsState()
    val ocultar by vm.ocultarMontos.collectAsState()
    val error by vm.error.collectAsState()
    var busqueda by rememberSaveable { mutableStateOf("") }
    // Hoja abierta, confirmación pendiente y aviso verde de abajo
    var abierto by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmar by rememberSaveable { mutableStateOf<String?>(null) } // "pagado"
    var aviso by rememberSaveable { mutableStateOf<String?>(null) }
    val hoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val alcance = rememberCoroutineScope()
    val cerrarHoja: () -> Unit = {
        alcance.launch { hoja.hide() }.invokeOnCompletion { if (!hoja.isVisible) abierto = null }
    }

    LaunchedEffect(aviso) {
        if (aviso != null) { delay(3500); aviso = null }
    }

    val todos = pendientes.orEmpty()
    val lista = Fiados.filtrar(todos, busqueda)
    val monto: (Long) -> String = { if (ocultar) Money.HIDDEN else Money.cop(it) }

    Box(Modifier.fillMaxSize().background(BarrioColors.Background).statusBarsPadding().testTag("fiados_screen")) {
        Column(Modifier.fillMaxSize()) {
            CabeceraFigma("Fiados", onBack = onBack)
            Column(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Resumen(monto(todos.sumOf { it.saldo }), Fiados.resumenClientes(todos.size))
                BotonFigma("+ Nuevo fiado", Modifier.testTag("new_fiado_button"), esquinas = 14, tamanoTexto = 15, onClick = onNewFiado)
                Buscador(busqueda) { busqueda = it }
            }
            when {
                pendientes == null -> Unit
                todos.isEmpty() -> Vacio("Nadie te debe por ahora", "Cuando registres un fiado, el cliente aparecerá aquí con lo que te debe.")
                lista.isEmpty() -> Vacio("Sin resultados", "No hay clientes con fiado que coincidan con \"${busqueda.trim()}\".")
                else -> LazyColumn(
                    Modifier.fillMaxSize().testTag("fiados_list"),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 40.dp),
                ) {
                    items(lista, key = { it.cliente.id }) { f ->
                        FilaFiado(f, monto(f.saldo), ultima = f == lista.last()) { abierto = f.cliente.id }
                    }
                }
            }
        }

        // "Marta Ramírez pagó. Entraron $ 12.000 a tu caja."
        aviso?.let {
            Text(
                it,
                color = FigmaColors.Verde, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FigmaColors.Tarjeta)
                    .border(1.dp, FigmaColors.Verde.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("fiado_aviso"),
            )
        }
    }

    val cuenta = abierto?.let { id -> todos.firstOrNull { it.cliente.id == id } }
    // Ya pagó o se anuló: sale de la lista y la hoja se cierra sola
    LaunchedEffect(abierto, cuenta == null) {
        if (abierto != null && cuenta == null && pendientes != null) { abierto = null; confirmar = null }
    }
    // En el Figma la confirmación de pago sale sobre la lista, sin la hoja detrás
    if (cuenta != null && confirmar != "pagado") {
        ModalBottomSheet(
            onDismissRequest = { abierto = null },
            sheetState = hoja,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = FigmaColors.Tarjeta,
            dragHandle = {
                Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 32.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(FigmaColors.Dorado))
            },
        ) {
            HojaFiado(
                cuenta = cuenta,
                deuda = Fiados.deudaActual(cuenta.cliente.id, movimientos),
                monto = monto,
                onVerCliente = { val id = cuenta.cliente.id; cerrarHoja(); onOpenClient(id) },
                // Primero baja la hoja y luego sale la confirmación sobre la lista
                onPagado = { alcance.launch { hoja.hide() }.invokeOnCompletion { confirmar = "pagado" } },
            )
        }
    }

    if (cuenta != null) when (confirmar) {
        "pagado" -> ConfirmarPago(
            nombre = cuenta.cliente.name,
            saldo = cuenta.saldo,
            onSi = {
                confirmar = null
                val nombre = cuenta.cliente.name
                vm.marcarPagado(cuenta.cliente.id) { entro -> aviso = Fiados.mensajePagado(nombre, entro) }
                abierto = null
            },
            onTodaviaNo = { confirmar = null; abierto = null },
        )
    }

    error?.let {
        AvisoDialogo(titulo = "No se pudo registrar el pago", mensaje = it, principal = "Entendido", onPrincipal = vm::cerrarError)
    }
}

/** Figma "¿Marta Ramírez te pagó los $12.000?": "Sí, me pagó" verde y "Todavía no" con borde amarillo. */
@Composable
private fun ConfirmarPago(nombre: String, saldo: Long, onSi: () -> Unit, onTodaviaNo: () -> Unit) {
    val forma = RoundedCornerShape(20.dp)
    Dialog(onDismissRequest = onTodaviaNo, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 32.dp)
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .clip(forma)
                .background(FigmaColors.Tarjeta)
                .border(1.dp, FigmaColors.Dorado, forma)
                .padding(20.dp)
                .testTag("confirmar_pago_dialogo"),
        ) {
            Text("¿$nombre te pagó los ${Money.cop(saldo)}?", color = FigmaColors.Crema, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Se registrará como una entrada de ${Money.cop(saldo)} a tu caja y el fiado queda saldado.",
                color = FigmaColors.Crema, fontSize = 14.sp, lineHeight = 19.sp,
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FigmaColors.Verde)
                    .clickable(onClick = onSi)
                    .testTag("si_me_pago_button"),
                contentAlignment = Alignment.Center,
            ) { Text("Sí, me pagó", color = FigmaColors.SobreAmarillo, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, FigmaColors.Amarillo, RoundedCornerShape(12.dp))
                    .clickable(onClick = onTodaviaNo)
                    .testTag("todavia_no_button"),
                contentAlignment = Alignment.Center,
            ) { Text("Todavía no", color = FigmaColors.Amarillo, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

/** Hoja "Fiado pendiente": nombre, saldo amarillo, lo que se llevó cada día y las dos acciones. */
@Composable
private fun HojaFiado(
    cuenta: FiadoPendiente,
    deuda: List<MovementEntity>,
    monto: (Long) -> String,
    onVerCliente: () -> Unit,
    onPagado: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
            .testTag("hoja_fiado"),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Fiado pendiente", color = FigmaColors.Dorado, fontSize = 13.sp, modifier = Modifier.weight(1f))
            // No está en el Figma: deja llegar a la ficha (historial y abonos parciales)
            Text(
                "Ver cliente", color = FigmaColors.Amarillo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onVerCliente).padding(4.dp).testTag("hoja_ver_cliente"),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(cuenta.cliente.name, color = FigmaColors.Crema, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(monto(cuenta.saldo), color = FigmaColors.Amarillo, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            deuda.forEach { m ->
                val abono = m.kind == MovementType.ABONO
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${Fiados.fechaCorta(m.createdAt)} · ${Fiados.conceptoCorto(m)}",
                        color = FigmaColors.Cuerpo, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    Text(
                        (if (abono) "−" else "") + monto(m.amount),
                        color = if (abono) FigmaColors.Verde else FigmaColors.Crema, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(FigmaColors.Verde)
                .clickable(onClick = onPagado)
                .testTag("marcar_pagado_button"),
            contentAlignment = Alignment.Center,
        ) { Text("Marcar como pagado", color = FigmaColors.SobreAmarillo, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, FigmaColors.Amarillo, RoundedCornerShape(12.dp))
                .clickable(onClick = onVerCliente)
                .testTag("editar_fiado_button"),
            contentAlignment = Alignment.Center,
        ) { Text("Editar o anular un fiado", color = FigmaColors.Amarillo, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
    }
}

/** "Resumen fiados": tarjeta #22593F de esquinas 16. */
@Composable
private fun Resumen(total: String, clientes: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FigmaColors.Tarjeta)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("fiados_summary"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Te deben en total", color = FigmaColors.Dorado, fontSize = 13.sp)
        Text(total, color = FigmaColors.Crema, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("fiados_total"))
        Text(clientes, color = FigmaColors.Cuerpo, fontSize = 13.sp)
    }
}

/** "Buscador · Lista": 56 de alto, borde dorado de 1 y esquinas 12. */
@Composable
private fun Buscador(texto: String, onCambio: (String) -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    BasicTextField(
        value = texto,
        onValueChange = onCambio,
        singleLine = true,
        textStyle = TextStyle(color = FigmaColors.Crema, fontSize = 16.sp),
        cursorBrush = SolidColor(FigmaColors.Amarillo),
        modifier = Modifier.fillMaxWidth().testTag("search_fiado_input"),
        decorationBox = { campo ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(forma)
                    .background(FigmaColors.Tarjeta)
                    .border(1.dp, FigmaColors.Dorado, forma)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Search, null, tint = FigmaColors.Dorado, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (texto.isEmpty()) {
                        Text("Buscar por nombre o teléfono", color = if (BarrioColors.isDark) Color(0x99E6E2DC) else FigmaColors.Tenue, fontSize = 16.sp)
                    }
                    campo()
                }
            }
        },
    )
}

/** "Fiado · Lista": 100 de alto, avatar de 40, nombre, desde cuándo y saldo amarillo; línea #22593F abajo. */
@Composable
private fun FilaFiado(f: FiadoPendiente, saldo: String, ultima: Boolean, onClick: () -> Unit) {
    val linea = FigmaColors.Tarjeta
    Row(
        Modifier
            .fillMaxWidth()
            .height(100.dp)
            .then(if (ultima) Modifier else Modifier.drawBehind {
                drawLine(linea, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), strokeWidth = 1.dp.toPx())
            })
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
            .testTag("fiado_${f.cliente.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(FigmaColors.Tarjeta), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Person, null, tint = FigmaColors.Dorado, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(f.cliente.name, color = FigmaColors.Crema, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (Fiados.pasoElLimite(f)) "Pasó su límite de ${Money.cop(f.cliente.creditLimit)}" else Fiados.subtitulo(f),
                color = if (Fiados.pasoElLimite(f)) FigmaColors.Naranja else FigmaColors.Dorado,
                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Text(saldo, color = FigmaColors.Amarillo, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Vacio(titulo: String, mensaje: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 64.dp).testTag("fiados_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(titulo, color = FigmaColors.Crema, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(mensaje, color = FigmaColors.Cuerpo, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}
