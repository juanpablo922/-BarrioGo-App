package com.barriogo.app.ui.fiados

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.domain.Fiados
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.PayMethod
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AvisoDialogo
import com.barriogo.app.ui.components.BotonFigma
import com.barriogo.app.ui.components.CabeceraFigma
import com.barriogo.app.ui.components.MoneyVisualTransformation
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.FigmaColors

/**
 * Figma "Fiado · Registrar pago" y "Fiado · Saldo actualizado": cuánto abona, cómo pagó
 * (efectivo entra a la caja; transferencia no) y el saldo que queda. "Cambiar" deja elegir
 * a otro cliente que deba. Con [fiadoId] (id en Firestore) el abono va a ese solo fiado y no
 * puede pasar de lo que ese fiado debe.
 */
@Composable
fun RegisterPaymentScreen(
    clientId: Long,
    onBack: () -> Unit,
    onVolverAFiados: () -> Unit,
    fiadoId: String? = null,
) {
    val vm = barrioViewModel { FiadosViewModel(it) }
    val cuentas by vm.cuentas.collectAsState()
    val todos by vm.movimientos.collectAsState()
    val error by vm.error.collectAsState()

    var clienteId by rememberSaveable { mutableStateOf(clientId) }
    // Fiado al que va el abono; null = a toda la deuda
    var fiadoElegido by rememberSaveable { mutableStateOf(fiadoId) }
    var eligiendo by rememberSaveable { mutableStateOf(false) }
    var montoTexto by rememberSaveable { mutableStateOf("") }
    var metodo by rememberSaveable { mutableStateOf(PayMethod.EFECTIVO) }
    var guardando by rememberSaveable { mutableStateOf(false) }
    // Lo que se muestra en "Saldo actualizado"
    var listo by rememberSaveable { mutableStateOf(false) }
    var montoGuardado by rememberSaveable { mutableLongStateOf(0L) }
    var saldoAntes by rememberSaveable { mutableLongStateOf(0L) }

    val cuenta = cuentas?.firstOrNull { it.cliente.id == clienteId }
    val fiado = fiadoElegido?.let { id -> todos.firstOrNull { it.remoteId == id && it.clientId == clienteId && !it.voided } }
    val porPagarFiado = fiado?.let { Fiados.pagosPorFiado(clienteId, todos)[it.id]?.porPagar } ?: 0L
    val atras: () -> Unit = {
        when {
            listo -> onVolverAFiados()
            eligiendo -> eligiendo = false
            else -> onBack()
        }
    }
    BackHandler(enabled = listo || eligiendo, onBack = atras)

    Box(Modifier.fillMaxSize().background(BarrioColors.Background).statusBarsPadding().imePadding().testTag("register_payment_screen")) {
        Column(Modifier.fillMaxSize()) {
            CabeceraFigma(
                when {
                    listo -> "Saldo actualizado"
                    eligiendo -> "¿Quién va a pagar?"
                    else -> "Registrar pago"
                },
                onBack = atras,
            )
            when {
                listo -> PagoRegistrado(
                    nombre = cuenta?.cliente?.name.orEmpty(),
                    monto = montoGuardado,
                    antes = saldoAntes,
                    historial = Fiados.movimientosDe(clienteId, todos, conAnulados = true),
                    onVolver = onVolverAFiados,
                )
                eligiendo -> ElegirDeudor(
                    deudores = cuentas.orEmpty().filter { it.saldo > 0 }.sortedByDescending { it.saldo },
                    onElegir = { id ->
                        if (id != clienteId) { montoTexto = ""; fiadoElegido = null }
                        clienteId = id
                        eligiendo = false
                    },
                )
                else -> FormularioPago(
                    nombre = cuenta?.cliente?.name.orEmpty(),
                    saldo = cuenta?.saldo ?: 0L,
                    fiado = fiado,
                    porPagarFiado = porPagarFiado,
                    onTodaLaDeuda = { fiadoElegido = null },
                    montoTexto = montoTexto,
                    onMonto = { montoTexto = it.filter(Char::isDigit).take(9).trimStart('0') },
                    metodo = metodo,
                    onMetodo = { metodo = it },
                    guardando = guardando,
                    onCambiar = { eligiendo = true },
                    onGuardar = {
                        val monto = montoTexto.toLongOrNull() ?: 0L
                        val saldo = cuenta?.saldo ?: 0L
                        val problema = if (fiado != null) Fiados.problemaAbonoAFiado(monto, porPagarFiado) else Fiados.problemaAbono(monto, saldo)
                        if (problema != null || guardando) return@FormularioPago
                        guardando = true
                        saldoAntes = saldo
                        vm.guardarPago(clienteId, monto, metodo, fiado?.remoteId) {
                            montoGuardado = monto
                            guardando = false
                            listo = true
                        }
                    },
                )
            }
        }
        error?.let {
            AvisoDialogo(titulo = "No se pudo registrar el pago", mensaje = it, principal = "Entendido", onPrincipal = {
                guardando = false
                vm.cerrarError()
            })
        }
    }
}

@Composable
private fun FormularioPago(
    nombre: String,
    saldo: Long,
    /** Fiado al que va el abono, o null si es a toda la deuda. */
    fiado: MovementEntity?,
    porPagarFiado: Long,
    onTodaLaDeuda: () -> Unit,
    montoTexto: String,
    onMonto: (String) -> Unit,
    metodo: PayMethod,
    onMetodo: (PayMethod) -> Unit,
    guardando: Boolean,
    onCambiar: () -> Unit,
    onGuardar: () -> Unit,
) {
    val monto = montoTexto.toLongOrNull() ?: 0L
    val aUnFiado = fiado != null
    val problema = if (aUnFiado) Fiados.problemaAbonoAFiado(monto, porPagarFiado) else Fiados.problemaAbono(monto, saldo)
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 8.dp)) {
            // Cliente: nombre, "Debe $ 50.000" y "Cambiar"
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FigmaColors.Tarjeta)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("pago_cliente"),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(nombre, color = FigmaColors.Crema, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Debe ${Money.cop(saldo)}", color = FigmaColors.Dorado, fontSize = 12.sp)
                // Con un fiado elegido no se cambia de cliente: el fiado es de este
                if (!aUnFiado) {
                    Text(
                        "Cambiar", color = FigmaColors.Amarillo, fontSize = 12.sp,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onCambiar).testTag("pago_cambiar_cliente"),
                    )
                }
            }
            if (fiado != null) {
                Spacer(Modifier.height(12.dp))
                // A qué fiado va el abono ("solo quiero saldar este")
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FigmaColors.Hundido)
                        .border(1.dp, FigmaColors.Dorado, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("pago_fiado_elegido"),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text("Abono solo a este fiado", color = FigmaColors.Dorado, fontSize = 12.sp)
                    Text(Fiados.titulo(fiado), color = FigmaColors.Crema, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${Fiados.cuando(fiado.createdAt)} · faltan ${Money.cop(porPagarFiado)}", color = FigmaColors.Cuerpo, fontSize = 12.sp)
                    Text(
                        "Abonar a toda la deuda", color = FigmaColors.Amarillo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onTodaLaDeuda).padding(top = 4.dp).testTag("pago_toda_la_deuda"),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            EtiquetaFiado("¿Cuánto va a abonar?")
            Spacer(Modifier.height(8.dp))
            CampoFiado(
                montoTexto, onMonto, "$ 0", Modifier.testTag("monto_pago_input"),
                teclado = KeyboardType.Number, accion = ImeAction.Done,
                transformacion = if (montoTexto.isEmpty()) VisualTransformation.None else MoneyVisualTransformation(),
                error = !problema.isNullOrEmpty(), montoGrande = true,
            )
            if (!problema.isNullOrEmpty()) {
                Text(problema, color = FigmaColors.Naranja, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp).testTag("pago_error"))
            }
            Spacer(Modifier.height(16.dp))
            EtiquetaFiado("¿Cómo pagó?")
            Spacer(Modifier.height(8.dp))
            OpcionPago("Efectivo", metodo == PayMethod.EFECTIVO, "pago_efectivo") { onMetodo(PayMethod.EFECTIVO) }
            Spacer(Modifier.height(8.dp))
            OpcionPago("Transferencia", metodo == PayMethod.TRANSFERENCIA, "pago_transferencia") { onMetodo(PayMethod.TRANSFERENCIA) }
            Spacer(Modifier.height(16.dp))
            // Saldo después del pago (verde)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FigmaColors.Hundido)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("saldo_despues_card"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(if (aUnFiado) "De este fiado quedarán" else "Saldo después del pago", color = FigmaColors.Dorado, fontSize = 12.sp)
                Text(Money.cop(((if (aUnFiado) porPagarFiado else saldo) - monto).coerceAtLeast(0)), color = FigmaColors.Verde, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                if (aUnFiado) Text("Deuda total después: ${Money.cop((saldo - monto).coerceAtLeast(0))}", color = FigmaColors.Cuerpo, fontSize = 12.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
        BotonFigma(
            "Registrar pago",
            Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp).testTag("registrar_pago_button"),
            habilitado = problema == null && !guardando,
            tamanoTexto = 15,
            onClick = onGuardar,
        )
    }
}

/** "Efectivo" / "Transferencia": la elegida con borde amarillo de 2 y texto en negrita. */
@Composable
private fun OpcionPago(texto: String, elegida: Boolean, tag: String, onClick: () -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(forma)
            .background(FigmaColors.Tarjeta)
            .border(if (elegida) 2.dp else 1.dp, if (elegida) FigmaColors.Amarillo else FigmaColors.Dorado, forma)
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, color = FigmaColors.Crema, fontSize = 13.sp, fontWeight = if (elegida) FontWeight.Bold else FontWeight.Normal)
    }
}

/** "Cambiar": lista de los clientes que deben, para registrarle el pago a otro. */
@Composable
private fun ElegirDeudor(deudores: List<com.barriogo.app.domain.FiadoPendiente>, onElegir: (Long) -> Unit) {
    if (deudores.isEmpty()) {
        Text("Nadie te debe por ahora.", color = FigmaColors.Cuerpo, fontSize = 14.sp, modifier = Modifier.padding(24.dp))
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)) {
        items(deudores, key = { it.cliente.id }) { c ->
            FilaCliente(c, ultima = c == deudores.last()) { onElegir(c.cliente.id) }
        }
    }
}

/** "Saldo actualizado": ✓ Pago registrado, saldo que queda y el historial del cliente. */
@Composable
private fun PagoRegistrado(
    nombre: String,
    monto: Long,
    antes: Long,
    historial: List<com.barriogo.app.data.MovementEntity>,
    onVolver: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp)) {
            item {
                val forma = RoundedCornerShape(20.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(forma)
                        .background(FigmaColors.Tarjeta)
                        .border(1.dp, FigmaColors.Verde.copy(alpha = 0.6f), forma)
                        .padding(horizontal = 20.dp, vertical = 26.dp)
                        .testTag("pago_registrado_card"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Filled.Check, null, tint = FigmaColors.Verde, modifier = Modifier.size(36.dp))
                    Text("Pago registrado", color = FigmaColors.Crema, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(Fiados.mensajePago(nombre, monto), color = FigmaColors.Cuerpo, fontSize = 14.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FigmaColors.Hundido)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("saldo_actualizado_card"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Saldo pendiente actualizado", color = FigmaColors.Dorado, fontSize = 12.sp)
                    Text(Money.cop((antes - monto).coerceAtLeast(0)), color = FigmaColors.Amarillo, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("${Money.cop(antes)} − ${Money.cop(monto)} abonados", color = FigmaColors.Cuerpo, fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
                SeccionFiado("Historial")
            }
            items(historial, key = { it.id }) { m -> FilaMovimientoFiado(m, ultima = m == historial.last()) }
        }
        BotonFigma(
            "Volver a Fiado",
            Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp).testTag("pago_volver_fiado"),
            tamanoTexto = 15,
            onClick = onVolver,
        )
    }
}
