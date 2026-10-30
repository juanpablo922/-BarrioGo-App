package com.barriogo.app.ui.fiados

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.domain.FiadoPendiente
import com.barriogo.app.domain.Fiados
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.kind
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AvisoDialogo
import com.barriogo.app.ui.components.BotonFigma
import com.barriogo.app.ui.components.CabeceraFigma
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.FigmaColors
import kotlinx.coroutines.delay

/**
 * Ficha del cliente (Figma "Fiado · Doña Marta"): iniciales, celular, dirección, cuánto debe y su
 * cupo, "Registrar nuevo fiado", "Registrar pago" y sus últimos fiados y abonos.
 * - "Editar datos": nombre, celular y dirección. "Cambiar cupo": solo un administrador.
 * - Tocar un fiado o abono: corregir el valor (y lo que se llevó o cómo pagó) o anularlo.
 */
@Composable
fun ClienteFiadoScreen(
    clienteId: Long,
    onBack: () -> Unit,
    onNuevoFiado: (Long) -> Unit,
    onRegistrarPago: (Long) -> Unit,
    /** Abonar a un solo fiado: (clienteId, id del fiado en Firestore). */
    onAbonarFiado: (Long, String) -> Unit = { _, _ -> },
) {
    val vm = barrioViewModel { FiadosViewModel(it) }
    val cuentas by vm.cuentas.collectAsState()
    val todos by vm.movimientos.collectAsState()
    val esAdmin by vm.esAdmin.collectAsState()
    val error by vm.error.collectAsState()
    val avisoCupo by vm.avisoCupo.collectAsState()
    val cuenta = cuentas?.firstOrNull { it.cliente.id == clienteId }
    val movimientos = Fiados.movimientosDe(clienteId, todos, conAnulados = true)
    val pagos = Fiados.pagosPorFiado(clienteId, todos)

    var editandoDatos by rememberSaveable { mutableStateOf(false) }
    var editandoCupo by rememberSaveable { mutableStateOf(false) }
    var editando by rememberSaveable { mutableStateOf<Long?>(null) }   // id del movimiento abierto
    var anulando by rememberSaveable { mutableStateOf<Long?>(null) }
    var aviso by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(aviso) {
        if (aviso != null) { delay(3500); aviso = null }
    }

    Box(Modifier.fillMaxSize().background(BarrioColors.Background).statusBarsPadding().testTag("cliente_fiado_screen")) {
        Column(Modifier.fillMaxSize()) {
            CabeceraFigma(cuenta?.cliente?.name.orEmpty(), onBack = onBack)
            if (cuenta == null) return@Column
            val c = cuenta.cliente
            // El menú flotante no tapa la lista (Scaffold le reserva su alto); el borde se desvanece en NavGraph
            LazyColumn(
                Modifier.fillMaxSize().testTag("cliente_fiado_lista"),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 40.dp),
            ) {
                item {
                    AvatarCliente(c.name, grande = true)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(c.name, color = FigmaColors.Crema, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(
                            "Editar datos", color = FigmaColors.Amarillo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { editandoDatos = true }.padding(4.dp).testTag("editar_cliente"),
                        )
                    }
                    Text(c.phone.ifBlank { "Sin celular" }, color = FigmaColors.Tenue, fontSize = 12.sp)
                    if (c.address.isNotBlank()) Text(c.address, color = FigmaColors.Tenue, fontSize = 12.sp, modifier = Modifier.testTag("cliente_direccion_texto"))
                    Spacer(Modifier.height(12.dp))
                    TarjetaDeuda(cuenta, esAdmin = esAdmin, onCambiarCupo = { editandoCupo = true })
                    Spacer(Modifier.height(12.dp))
                    BotonFigma("Registrar nuevo fiado", Modifier.testTag("cliente_nuevo_fiado"), tamanoTexto = 15) {
                        // Avisa antes de entrar: ya no se le puede fiar más
                        if (Fiados.enElLimite(cuenta)) vm.avisarCupo(Fiados.problemaCupo(c.name, cuenta.saldo, 1, c.creditLimit) ?: "${c.name} pasó su límite de fiado.")
                        else onNuevoFiado(clienteId)
                    }
                    Spacer(Modifier.height(12.dp))
                    BotonContornoFiado("Registrar pago", Modifier.testTag("cliente_registrar_pago")) { onRegistrarPago(clienteId) }
                    Spacer(Modifier.height(20.dp))
                    SeccionFiado("Últimos movimientos")
                    if (movimientos.isEmpty()) {
                        Text(
                            "Todavía no tiene fiados ni pagos.",
                            color = FigmaColors.Cuerpo, fontSize = 14.sp,
                            modifier = Modifier.padding(top = 16.dp).testTag("cliente_sin_movimientos"),
                        )
                    } else {
                        Text("Toca un fiado o un abono para corregirlo o anularlo.", color = FigmaColors.Cuerpo, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                items(movimientos, key = { it.id }) { m ->
                    FilaMovimientoFiado(m, ultima = m == movimientos.last(), pago = pagos[m.id], onClick = { editando = m.id })
                }
            }
        }

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
                    .testTag("cliente_aviso"),
            )
        }
    }

    val c = cuenta?.cliente
    if (c != null && editandoDatos) {
        DialogoCliente(
            titulo = "Editar datos", boton = "Guardar cambios",
            nombreInicial = c.name, celularInicial = c.phone, direccionInicial = c.address,
            onCancelar = { editandoDatos = false },
            onGuardar = { n, cel, dir ->
                vm.editarCliente(c.id, n, cel, dir) { aviso = "Datos de $n actualizados." }
                editandoDatos = false
            },
        )
    }
    if (c != null && editandoCupo && esAdmin) {
        DialogoCupo(
            nombre = c.name, cupoActual = c.creditLimit, saldo = cuenta.saldo,
            onCancelar = { editandoCupo = false },
            onGuardar = { cupo ->
                vm.cambiarCupo(c.id, cupo) { aviso = "Ahora a ${c.name} le puedes fiar hasta ${Money.cop(cupo)}." }
                editandoCupo = false
            },
        )
    }
    val abierto = editando?.let { id -> movimientos.firstOrNull { it.id == id } }
    if (abierto != null && anulando == null) {
        val remoteId = abierto.remoteId.orEmpty()
        DialogoMovimientoFiado(
            m = abierto,
            pago = pagos[abierto.id],
            onCancelar = { editando = null },
            onAbonar = { editando = null; onAbonarFiado(clienteId, remoteId) },
            onGuardarFiado = { que, monto -> vm.editarFiado(remoteId, que, monto) { aviso = "Fiado corregido."; editando = null } },
            onGuardarAbono = { monto, metodo -> vm.editarAbono(remoteId, monto, metodo) { aviso = "Abono corregido."; editando = null } },
            onAnular = { anulando = abierto.id },
        )
    }
    val porAnular = anulando?.let { id -> movimientos.firstOrNull { it.id == id } }
    if (porAnular != null) {
        val esFiado = porAnular.kind == MovementType.FIADO
        AvisoDialogo(
            titulo = if (esFiado) "¿Anular este fiado?" else "¿Anular este abono?",
            mensaje = "${Fiados.titulo(porAnular)} por ${Money.cop(porAnular.amount)} queda anulado: se ve tachado en Movimientos " +
                "y en el historial, y ya no cuenta en lo que te debe. Úsalo solo si se registró por error.",
            principal = "Sí, anular",
            onPrincipal = {
                vm.anular(porAnular.remoteId.orEmpty()) { aviso = if (esFiado) "Se anuló el fiado." else "Se anuló el abono." }
                anulando = null
                editando = null
            },
            secundario = "Conservar",
            onSecundario = { anulando = null },
            principalPeligro = true,
        )
    }
    avisoCupo?.let {
        AvisoDialogo(titulo = "Llegó a su límite de fiado", mensaje = it, principal = "Entendido", onPrincipal = vm::cerrarAvisoCupo)
    }
    error?.let {
        AvisoDialogo(titulo = "No se pudo guardar", mensaje = it, principal = "Entendido", onPrincipal = vm::cerrarError)
    }
}

/**
 * Lo que debe y su cupo en una sola tarjeta: saldo, fiados pendientes, barra de cuánto del cupo
 * ya usó, el cupo con "Cambiar cupo" (solo administrador) y el aviso si llegó o pasó el límite.
 */
@Composable
private fun TarjetaDeuda(cuenta: FiadoPendiente, esAdmin: Boolean, onCambiarCupo: () -> Unit) {
    val cupo = cuenta.cliente.creditLimit
    val paso = Fiados.pasoElLimite(cuenta)
    val enElLimite = Fiados.enElLimite(cuenta)
    val acento = when {
        enElLimite -> FigmaColors.Naranja
        cuenta.saldo * 10 >= cupo * 8 -> FigmaColors.Amarillo   // usó el 80 % o más
        else -> FigmaColors.Verde
    }
    val forma = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(FigmaColors.Tarjeta)
            .border(1.dp, if (enElLimite) FigmaColors.Naranja else FigmaColors.Tarjeta, forma)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("cliente_saldo_card"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Debe actualmente", color = FigmaColors.Dorado, fontSize = 12.sp, modifier = Modifier.weight(1f))
            if (enElLimite) {
                Text(
                    if (paso) "Superó el límite" else "En el límite",
                    color = FigmaColors.SobreAmarillo, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(FigmaColors.Naranja)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                        .testTag("cliente_limite_chip"),
                )
            }
        }
        Text(Money.cop(cuenta.saldo), color = FigmaColors.Amarillo, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(Fiados.fiadosPendientes(cuenta.fiados), color = FigmaColors.Cuerpo, fontSize = 12.sp)

        Spacer(Modifier.height(10.dp))
        // Cuánto del cupo ya usó
        val usado = if (cupo <= 0) 1f else (cuenta.saldo.toFloat() / cupo).coerceIn(0f, 1f)
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(FigmaColors.Hundido)
                .testTag("cliente_cupo_barra"),
        ) {
            Box(Modifier.fillMaxWidth(usado).height(8.dp).clip(RoundedCornerShape(4.dp)).background(acento))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Cupo ${Money.cop(cupo)}", color = FigmaColors.Crema, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).testTag("cliente_cupo"),
            )
            if (esAdmin) {
                Text(
                    "Cambiar cupo", color = FigmaColors.Amarillo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onCambiarCupo).padding(4.dp).testTag("cambiar_cupo"),
                )
            }
        }
        Text(
            when {
                paso && cuenta.saldo > cupo -> "Superó su límite por ${Money.cop(cuenta.saldo - cupo)}: no se le puede fiar más"
                enElLimite -> "Llegó a su límite: no se le puede fiar más"
                else -> "Le puedes fiar hasta ${Money.cop(Fiados.disponible(cuenta))} más"
            },
            color = if (enElLimite) FigmaColors.Naranja else FigmaColors.Cuerpo, fontSize = 12.sp,
            modifier = Modifier.testTag("cliente_cupo_estado"),
        )
        if (enElLimite && esAdmin) {
            Text("Para fiarle más, que abone o súbele el cupo.", color = FigmaColors.Cuerpo, fontSize = 12.sp)
        }
    }
}
