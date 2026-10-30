package com.barriogo.app.ui.fiados

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.barriogo.app.domain.FiadoPendiente
import com.barriogo.app.domain.Fiados
import com.barriogo.app.domain.Money
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AvisoDialogo
import com.barriogo.app.ui.components.BotonFigma
import com.barriogo.app.ui.components.CabeceraFigma
import com.barriogo.app.ui.components.MoneyVisualTransformation
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.FigmaColors

/**
 * Nuevo fiado en 3 pasos (Figma "Fiado · Seleccionar cliente", "Fiado · Nuevo fiado" y
 * "Fiado · Fiado guardado"). Con [clienteInicial] (desde la ficha del cliente) empieza en el paso 2.
 * Si el cliente llegó a su cupo, o el monto lo haría pasar, la app avisa y no deja guardar.
 */
@Composable
fun NewFiadoScreen(
    onBack: () -> Unit,
    onVerCliente: (Long) -> Unit,
    onVolverAFiados: () -> Unit,
    clienteInicial: Long? = null,
) {
    val vm = barrioViewModel { FiadosViewModel(it) }
    val cuentas by vm.cuentas.collectAsState()
    val error by vm.error.collectAsState()
    val avisoCupo by vm.avisoCupo.collectAsState()

    var paso by rememberSaveable { mutableStateOf(if (clienteInicial != null) 2 else 1) }
    var clienteId by rememberSaveable { mutableStateOf(clienteInicial) }
    // Nombre y celular del cliente elegido o recién agregado, por si la lista todavía no lo trae
    var nombreElegido by rememberSaveable { mutableStateOf("") }
    var celularElegido by rememberSaveable { mutableStateOf("") }
    var montoTexto by rememberSaveable { mutableStateOf("") }
    var queSeLlevo by rememberSaveable { mutableStateOf("") }
    // Lo que se muestra en "Fiado guardado"
    var montoGuardado by rememberSaveable { mutableLongStateOf(0L) }
    var saldoAnterior by rememberSaveable { mutableLongStateOf(0L) }
    var guardando by rememberSaveable { mutableStateOf(false) }

    val cuenta = cuentas?.firstOrNull { it.cliente.id == clienteId }

    val atras: () -> Unit = {
        when {
            paso == 3 -> onVolverAFiados()
            paso == 2 && clienteInicial == null -> paso = 1
            else -> onBack()
        }
    }
    BackHandler(enabled = paso != 1 && !(paso == 2 && clienteInicial != null), onBack = atras)

    Box(Modifier.fillMaxSize().background(BarrioColors.Background).statusBarsPadding().imePadding().testTag("new_fiado_screen")) {
        Column(Modifier.fillMaxSize()) {
            CabeceraFigma(
                when (paso) {
                    1 -> "Seleccionar cliente"
                    2 -> "Nuevo fiado"
                    else -> "Fiado guardado"
                },
                onBack = atras,
            )
            PasosFiado(paso, Modifier.padding(top = 8.dp, bottom = 20.dp))
            when (paso) {
                1 -> PasoCliente(
                    cuentas = cuentas,
                    onElegir = { id ->
                        val elegida = cuentas?.firstOrNull { it.cliente.id == id }
                        if (elegida != null && Fiados.enElLimite(elegida)) {
                            // Ya no se le puede fiar: se avisa y se queda en la lista
                            vm.avisarCupo(
                                Fiados.problemaCupo(elegida.cliente.name, elegida.saldo, 1, elegida.cliente.creditLimit)
                                    ?: "${elegida.cliente.name} pasó su límite de fiado.",
                            )
                            return@PasoCliente
                        }
                        if (id != clienteId) { montoTexto = ""; queSeLlevo = "" }
                        clienteId = id
                        nombreElegido = elegida?.cliente?.name.orEmpty()
                        celularElegido = elegida?.cliente?.phone.orEmpty()
                        paso = 2
                    },
                    onAgregar = { nombre, celular, direccion, listo ->
                        vm.agregarCliente(nombre, celular, direccion) { id ->
                            listo()
                            clienteId = id
                            nombreElegido = nombre
                            celularElegido = celular
                            montoTexto = ""
                            queSeLlevo = ""
                            paso = 2
                        }
                    },
                )
                2 -> PasoMonto(
                    nombre = cuenta?.cliente?.name ?: nombreElegido,
                    celular = cuenta?.cliente?.phone ?: celularElegido,
                    saldo = cuenta?.saldo ?: 0L,
                    cupo = cuenta?.cliente?.creditLimit ?: Fiados.CUPO_POR_DEFECTO,
                    montoTexto = montoTexto,
                    onMonto = { montoTexto = it.filter(Char::isDigit).take(9).trimStart('0') },
                    queSeLlevo = queSeLlevo,
                    onQueSeLlevo = { queSeLlevo = it.take(200) },
                    guardando = guardando,
                    onCambiar = { paso = 1 },
                    onGuardar = {
                        val id = clienteId ?: return@PasoMonto
                        val monto = montoTexto.toLongOrNull() ?: 0L
                        if (monto <= 0 || queSeLlevo.isBlank() || guardando) return@PasoMonto
                        guardando = true
                        saldoAnterior = cuenta?.saldo ?: 0L
                        vm.guardarFiado(id, queSeLlevo, monto) {
                            montoGuardado = monto
                            guardando = false
                            paso = 3
                        }
                    },
                )
                else -> PasoGuardado(
                    nombre = cuenta?.cliente?.name.orEmpty(),
                    queSeLlevo = queSeLlevo,
                    monto = montoGuardado,
                    anterior = saldoAnterior,
                    onVerCliente = { clienteId?.let(onVerCliente) },
                    onVolver = onVolverAFiados,
                )
            }
        }
        error?.let {
            AvisoDialogo(titulo = "No se pudo guardar el fiado", mensaje = it, principal = "Entendido", onPrincipal = {
                guardando = false
                vm.cerrarError()
            })
        }
        // Aviso al vendedor o al administrador: el cliente llegó (o pasaría) su límite de fiado
        avisoCupo?.let {
            AvisoDialogo(titulo = "Llegó a su límite de fiado", mensaje = it, principal = "Entendido", onPrincipal = {
                guardando = false
                vm.cerrarAvisoCupo()
            })
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Paso 1 · Seleccionar cliente
// ---------------------------------------------------------------------------------------------

@Composable
private fun PasoCliente(
    cuentas: List<FiadoPendiente>?,
    onElegir: (Long) -> Unit,
    onAgregar: (nombre: String, celular: String, direccion: String, listo: () -> Unit) -> Unit,
) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var agregando by rememberSaveable { mutableStateOf(false) }
    val lista = Fiados.filtrar(Fiados.recientes(cuentas.orEmpty()), busqueda)

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            EtiquetaFiado("Buscar por nombre o celular")
            Spacer(Modifier.height(8.dp))
            CampoFiado(busqueda, { busqueda = it }, "Buscar cliente", Modifier.testTag("buscar_cliente_input"), accion = ImeAction.Search)
            Spacer(Modifier.height(16.dp))
            BotonContornoFiado("+ Agregar cliente", Modifier.testTag("agregar_cliente_button")) { agregando = true }
            Spacer(Modifier.height(20.dp))
            SeccionFiado(if (busqueda.isBlank()) "Clientes recientes" else "Resultados")
        }
        when {
            cuentas == null -> Unit
            lista.isEmpty() -> Text(
                if (busqueda.isBlank()) "Todavía no tienes clientes. Agrega el primero con \"+ Agregar cliente\"."
                else "Nadie coincide con \"${busqueda.trim()}\". Puedes agregarlo como cliente nuevo.",
                color = FigmaColors.Cuerpo, fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).testTag("clientes_vacio"),
            )
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)) {
                items(lista, key = { it.cliente.id }) { c ->
                    FilaCliente(c, ultima = c == lista.last()) { onElegir(c.cliente.id) }
                }
            }
        }
    }

    if (agregando) {
        // Lo que se escribió en el buscador sirve de punto de partida: letras → nombre, números → celular
        val soloNumeros = busqueda.isNotBlank() && busqueda.all { it.isDigit() || it == ' ' }
        DialogoCliente(
            titulo = "Agregar cliente",
            boton = "Agregar y continuar",
            nombreInicial = if (soloNumeros) "" else busqueda.trim(),
            celularInicial = if (soloNumeros) busqueda.filter(Char::isDigit) else "",
            direccionInicial = "",
            onCancelar = { agregando = false },
            onGuardar = { n, c, d -> onAgregar(n, c, d) { agregando = false } },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Paso 2 · Nuevo fiado
// ---------------------------------------------------------------------------------------------

@Composable
private fun PasoMonto(
    nombre: String,
    celular: String,
    saldo: Long,
    cupo: Long,
    montoTexto: String,
    onMonto: (String) -> Unit,
    queSeLlevo: String,
    onQueSeLlevo: (String) -> Unit,
    guardando: Boolean,
    onCambiar: () -> Unit,
    onGuardar: () -> Unit,
) {
    val monto = montoTexto.toLongOrNull() ?: 0L
    val problemaCupo = if (monto > 0) Fiados.problemaCupo(nombre, saldo, monto, cupo) else null
    val listo = monto > 0 && queSeLlevo.isNotBlank() && problemaCupo == null
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            // Cliente elegido: iniciales, nombre y celular, con "Cambiar" a la derecha
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FigmaColors.Tarjeta)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("cliente_elegido"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AvatarCliente(nombre.ifBlank { "?" })
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        nombre.ifBlank { "Cliente" }, color = FigmaColors.Crema, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("cliente_elegido_nombre"),
                    )
                    Text(celular.ifBlank { "Sin celular" }, color = FigmaColors.Dorado, fontSize = 12.sp, maxLines = 1)
                }
                Text(
                    "Cambiar", color = FigmaColors.Amarillo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onCambiar).padding(4.dp).testTag("cambiar_cliente"),
                )
            }
            Spacer(Modifier.height(16.dp))
            EtiquetaFiado("¿Cuánto va a fiar?")
            Spacer(Modifier.height(8.dp))
            CampoFiado(
                montoTexto, onMonto, "$ 0", Modifier.testTag("monto_fiado_input"),
                teclado = KeyboardType.Number,
                transformacion = if (montoTexto.isEmpty()) androidx.compose.ui.text.input.VisualTransformation.None else MoneyVisualTransformation(),
            )
            Spacer(Modifier.height(16.dp))
            EtiquetaFiado("¿Qué se llevó?")
            Spacer(Modifier.height(8.dp))
            CampoFiado(
                queSeLlevo, onQueSeLlevo, "Ej. leche, pan, huevos", Modifier.testTag("que_se_llevo_input"),
                alto = 88.dp, unaLinea = false, accion = ImeAction.Default,
            )
            Spacer(Modifier.height(12.dp))
            Text("Saldo actual ${Money.cop(saldo)}", color = FigmaColors.Cuerpo, fontSize = 13.sp, modifier = Modifier.testTag("saldo_actual"))
            Spacer(Modifier.height(4.dp))
            Text(
                problemaCupo ?: "Cupo ${Money.cop(cupo)} · le puedes fiar hasta ${Money.cop((cupo - saldo).coerceAtLeast(0))}",
                color = if (problemaCupo != null) FigmaColors.Naranja else FigmaColors.Cuerpo, fontSize = 13.sp, lineHeight = 18.sp,
                modifier = Modifier.testTag("cupo_fiado"),
            )
            Spacer(Modifier.height(24.dp))
        }
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BotonFigma("Guardar fiado", Modifier.testTag("guardar_fiado_button"), habilitado = listo && !guardando, tamanoTexto = 15, onClick = onGuardar)
            Text(
                when {
                    listo -> "Todo listo, ya puedes guardar"
                    monto <= 0 -> "Escribe el monto para continuar"
                    problemaCupo != null -> "Pasa su límite de fiado"
                    else -> "Escribe qué se llevó"
                },
                color = when {
                    listo -> FigmaColors.Verde
                    problemaCupo != null -> FigmaColors.Naranja
                    else -> FigmaColors.Crema
                },
                fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().testTag("guardar_fiado_estado"),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Paso 3 · Fiado guardado
// ---------------------------------------------------------------------------------------------

@Composable
private fun PasoGuardado(
    nombre: String,
    queSeLlevo: String,
    monto: Long,
    anterior: Long,
    onVerCliente: () -> Unit,
    onVolver: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val forma = RoundedCornerShape(20.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(forma)
                    .background(FigmaColors.Tarjeta)
                    .border(1.dp, FigmaColors.Verde.copy(alpha = 0.35f), forma)
                    .padding(horizontal = 20.dp, vertical = 28.dp)
                    .testTag("fiado_guardado_card"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(FigmaColors.Verde), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, null, tint = FigmaColors.SobreAmarillo, modifier = Modifier.size(26.dp))
                }
                Text("Fiado guardado", color = FigmaColors.Crema, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    Fiados.mensajeGuardado(nombre, queSeLlevo, monto),
                    color = FigmaColors.Cuerpo, fontSize = 14.sp, lineHeight = 19.sp, textAlign = TextAlign.Center,
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FigmaColors.Hundido)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .testTag("nuevo_saldo_card"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Nuevo saldo pendiente", color = FigmaColors.Dorado, fontSize = 12.sp)
                Text(Money.cop(anterior + monto), color = FigmaColors.Amarillo, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("${Money.cop(anterior)} anterior + ${Money.cop(monto)} nuevo", color = FigmaColors.Cuerpo, fontSize = 12.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BotonFigma("Ver cliente", Modifier.testTag("ver_cliente_button"), tamanoTexto = 15, onClick = onVerCliente)
            BotonContornoFiado("Volver a Fiado", Modifier.testTag("volver_fiado_button"), onClick = onVolver)
        }
    }
}
