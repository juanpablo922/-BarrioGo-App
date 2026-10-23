package com.barriogo.app.ui.cash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.domain.AvisoCierre
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.EstadoCaja
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.ReglasCaja
import com.barriogo.app.domain.ResultadoArqueo
import com.barriogo.app.domain.cashDelta
import com.barriogo.app.domain.label
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AvisoDialogo
import com.barriogo.app.ui.components.AvisoFlotante
import com.barriogo.app.ui.components.BotonFigma
import com.barriogo.app.ui.components.BotonPaso
import com.barriogo.app.ui.components.CabeceraFigma
import com.barriogo.app.ui.components.LoadingOverlay
import com.barriogo.app.ui.components.MensajeFlotante
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.FigmaColors
import com.barriogo.app.ui.theme.Inter
import java.time.LocalTime

/**
 * C1 · Caja: dinero en caja, ventas y gastos de la caja abierta, fiados, cerrar caja, arqueo (a1)
 * y la hora de cierre (la configura un administrador; desde ahí salen los avisos).
 * Sin caja abierta hoy muestra cómo abrirla; con la de hoy cerrada, el comprobante y "Corregir cierre".
 */
@Composable
fun CashScreen(
    onOpenCash: () -> Unit,
    onSeeAllMovements: () -> Unit,
    onEditOpening: () -> Unit = onOpenCash,
    onCloseCash: () -> Unit = {},
    onSeeClosing: () -> Unit = {},
    onCorrectClosing: () -> Unit = {},
    onGoExpenses: () -> Unit = {},
) {
    val vm = barrioViewModel { CashViewModel(it) }
    val s by vm.state.collectAsState()
    val ocupado by vm.ocupado.collectAsState()
    val error by vm.error.collectAsState()

    var showArqueo by rememberSaveable { mutableStateOf(false) }
    var elegirHora by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<MensajeFlotante?>(null) }

    if (showArqueo && s.session != null) {
        ArqueoView(
            expectedAmount = s.saldo,
            perfil = s.perfil?.name ?: "",
            onBack = { showArqueo = false },
            onSave = { counted, note ->
                vm.saveArqueo(counted, note) {
                    showArqueo = false
                    mensaje = MensajeFlotante("Arqueo guardado. La caja sigue abierta.")
                }
            },
        )
        ocupado?.let { LoadingOverlay(it) }
        return
    }

    val abierta = s.session
    Box(Modifier.fillMaxSize().background(BarrioColors.Background).testTag("cash_screen")) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                // Header-TopAppBar del Figma: "Caja" amarillo Bold 22 en una franja de 56.
                Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Caja", color = FigmaColors.Amarillo, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("cash_title"))
                }
            }

            s.textoAviso?.let { texto -> item { FranjaAviso(texto, vencido = s.aviso is AvisoCierre.Vencido, esAdmin = s.esAdmin, onCerrar = onCloseCash) } }
            if (s.estado == EstadoCaja.ABIERTA_DIA_ANTERIOR && abierta != null) {
                item {
                    FranjaAviso(
                        "La caja del ${Dates.day(abierta.openedAt)} sigue abierta. Ciérrala para poder abrir la de hoy.",
                        vencido = true, esAdmin = s.esAdmin, onCerrar = onCloseCash,
                    )
                }
            }

            when {
                !s.cargado -> Unit
                abierta == null && s.estado == EstadoCaja.CERRADA_HOY -> item {
                    TarjetaEstado(
                        titulo = "La caja de hoy ya está cerrada",
                        texto = "Mañana podrás abrir una nueva caja.",
                        boton = "Ver comprobante",
                        onBoton = onSeeClosing,
                        enlace = if (s.puedeCorregirCierre) "Corregir cierre (te ${if (s.cambiosCierreRestantes == 1) "queda 1" else "quedan ${s.cambiosCierreRestantes}"})" else null,
                        onEnlace = onCorrectClosing,
                    )
                }
                abierta == null -> item {
                    TarjetaEstado(
                        titulo = "Abre la caja del día",
                        texto = "Sin la apertura no se pueden registrar ventas, gastos ni fiados.",
                        boton = "Abrir caja",
                        onBoton = onOpenCash,
                    )
                }
                else -> {
                    item { TarjetaDinero(s.saldo, abierta.openingAmount, s.editsLeft, onEditOpening) }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TarjetaCifra("Ventas", Money.cop(s.entradas), Icons.AutoMirrored.Filled.TrendingUp, FigmaColors.Verde, Modifier.weight(1f))
                            TarjetaCifra(
                                "Gastos", if (s.salidas > 0) "-" + Money.cop(s.salidas) else "$ 0", Icons.AutoMirrored.Filled.TrendingDown, FigmaColors.Naranja,
                                Modifier.weight(1f).testTag("expenses_card"), onClick = onGoExpenses,
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(FigmaColors.Tarjeta).padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Total fiados pendientes", color = FigmaColors.Dorado, fontSize = 13.sp)
                        Text(Money.cop(s.fiadoTotal), color = FigmaColors.Crema, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (s.fiadoClientes == 1) "1 cliente" else "${s.fiadoClientes} clientes",
                        color = FigmaColors.Dorado, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(BarrioColors.Background).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            if (abierta != null) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BotonCaja("Cerrar caja", principal = true, modifier = Modifier.weight(1f).testTag("close_cash_button"), onClick = onCloseCash)
                        BotonCaja("Arqueo de caja", principal = false, modifier = Modifier.weight(1f).testTag("arqueo_button")) { showArqueo = true }
                    }
                }
            }

            item { FilaHoraCierre(s.horaCierre, s.esAdmin) { elegirHora = true } }

            if (abierta != null) {
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Últimos movimientos de caja", color = FigmaColors.Dorado, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Ver todos", color = BarrioColors.Yellow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onSeeAllMovements))
                    }
                }
                if (s.movimientosCaja.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(FigmaColors.Tarjeta).padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Sin movimientos registrados en caja hoy", color = BarrioColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("Los ingresos y egresos aparecerán aquí", color = BarrioColors.TextHint, fontSize = 13.sp)
                        }
                    }
                } else {
                    val ultimos = s.movimientosCaja.take(5)
                    items(ultimos, key = { it.id }) { m ->
                        val entra = m.cashDelta() >= 0
                        FilaMovimiento(
                            titulo = m.label(),
                            subtitulo = "${Dates.time(m.createdAt)} · ${m.type.lowercase().replaceFirstChar { it.uppercase() }}",
                            monto = (if (entra) "+" else "-") + Money.cop(m.amount),
                            entra = entra,
                            divisor = m != ultimos.last(),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }

        AvisoFlotante(mensaje, onTerminar = { mensaje = null })

        if (elegirHora) {
            SelectorHora(
                inicial = ReglasCaja.hora(s.horaCierre),
                onCancelar = { elegirHora = false },
                onElegir = { hora ->
                    elegirHora = false
                    vm.guardarHoraCierre(ReglasCaja.horaTexto(hora)) {
                        mensaje = MensajeFlotante("Hora de cierre: ${ReglasCaja.horaLegible(hora)}. Te avisaremos 30 minutos antes.")
                    }
                },
            )
        }
        error?.let { AvisoDialogo(titulo = "No se pudo guardar", mensaje = it, principal = "Entendido", onPrincipal = vm::cerrarError) }
        ocupado?.let { LoadingOverlay(it) }
    }
}

/** Franja de aviso de cierre: amarilla antes de la hora, naranja cuando ya pasó. */
@Composable
internal fun FranjaAviso(texto: String, vencido: Boolean, esAdmin: Boolean, onCerrar: () -> Unit) {
    val color = if (vencido) FigmaColors.Alerta else FigmaColors.Amarillo
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color, RoundedCornerShape(12.dp))
            .clickable(onClick = onCerrar)
            .padding(12.dp)
            .testTag("aviso_cierre"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.NotificationsActive, null, tint = color, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            if (esAdmin) Text("Administrador", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(texto, color = FigmaColors.Crema, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Text("Cerrar", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** "Tarjeta Saldo" del Figma (C1 y H1): #22593F, esquinas 16, etiqueta dorada 13 y monto amarillo Bold 34. */
@Composable
internal fun TarjetaSaldo(
    etiqueta: String,
    saldo: String,
    apertura: String,
    cambiosRestantes: Int?,
    onEditar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FigmaColors.Tarjeta).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(etiqueta, color = FigmaColors.Dorado, fontSize = 13.sp)
            // K1 · Corregir la apertura (máx. 3). Sin cambios: candado al 55 % y "Sin más cambios hoy".
            if (cambiosRestantes != null) {
                val bloqueada = cambiosRestantes == 0
                Icon(
                    if (bloqueada) Icons.Filled.Lock else Icons.Outlined.Edit,
                    contentDescription = if (bloqueada) "Sin más cambios hoy" else "Corregir apertura",
                    tint = if (bloqueada) FigmaColors.Crema else FigmaColors.Amarillo,
                    modifier = Modifier.size(20.dp).alpha(if (bloqueada) 0.55f else 1f)
                        .clip(CircleShape).clickable(enabled = !bloqueada, onClick = onEditar).testTag("edit_cash_button"),
                )
            }
        }
        Text(saldo, color = FigmaColors.Amarillo, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text(apertura, color = FigmaColors.Cuerpo, fontSize = 13.sp)
        if (cambiosRestantes == 0) ContadorCambios(0, compacto = true)
    }
}

@Composable
private fun TarjetaDinero(saldo: Long, apertura: Long, cambiosRestantes: Int, onEditar: () -> Unit) =
    TarjetaSaldo("Dinero en caja", Money.cop(saldo), "Apertura del día: ${Money.cop(apertura)}", cambiosRestantes, onEditar)

/** "Tarjeta Entró / Salió" del Figma: ícono de 20 arriba, etiqueta dorada 13 y monto Bold 20 de color. */
@Composable
internal fun TarjetaCifra(
    titulo: String,
    valor: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(FigmaColors.Tarjeta)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icono, null, tint = color, modifier = Modifier.size(20.dp))
        Text(titulo, color = FigmaColors.Dorado, fontSize = 13.sp)
        Text(valor, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** C1 · "Cerrar caja" relleno amarillo (acción principal) y "Arqueo de caja" con borde dorado. */
@Composable
private fun BotonCaja(texto: String, principal: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    Box(
        modifier
            .clip(forma)
            .then(if (principal) Modifier.background(FigmaColors.AmarilloBoton) else Modifier.border(1.dp, FigmaColors.Dorado, forma))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, color = if (principal) FigmaColors.SobreAmarillo else FigmaColors.Amarillo, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TarjetaEstado(titulo: String, texto: String, boton: String, onBoton: () -> Unit, enlace: String? = null, onEnlace: () -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FigmaColors.Tarjeta).padding(horizontal = 20.dp, vertical = 18.dp)
            .testTag("cash_state_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(titulo, color = BarrioColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(texto, color = BarrioColors.TextHint, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        BotonFigma(boton, modifier = Modifier.testTag("cash_state_action"), onClick = onBoton)
        if (enlace != null) {
            Text(
                enlace, color = BarrioColors.Gold, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onEnlace).padding(vertical = 12.dp).testTag("correct_close_link"),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/** Hora de cierre: el administrador la cambia; los demás la ven. */
@Composable
private fun FilaHoraCierre(hora: String, esAdmin: Boolean, onCambiar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(FigmaColors.Tarjeta)
            .then(if (esAdmin) Modifier.clickable(onClick = onCambiar) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp).testTag("closing_time_row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.AccessTime, null, tint = BarrioColors.Yellow, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Hora de cierre", color = BarrioColors.TextHint, fontSize = 13.sp)
            Text(ReglasCaja.horaLegible(ReglasCaja.hora(hora)), color = BarrioColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                if (esAdmin) "Te avisamos 30 minutos antes y después, mientras siga abierta."
                else "La cambia un administrador.",
                color = BarrioColors.TextHint, fontSize = 12.sp,
            )
        }
        if (esAdmin) Text("Cambiar", color = BarrioColors.Yellow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorHora(inicial: LocalTime, onCancelar: () -> Unit, onElegir: (LocalTime) -> Unit) {
    val estado = rememberTimePickerState(initialHour = inicial.hour, initialMinute = inicial.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = FigmaColors.Tarjeta,
        title = { Text("¿A qué hora cierras la caja?", color = FigmaColors.Crema, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            TimePicker(
                state = estado,
                colors = TimePickerDefaults.colors(
                    clockDialColor = FigmaColors.Hundido,
                    selectorColor = FigmaColors.AmarilloBoton,
                    timeSelectorSelectedContainerColor = FigmaColors.AmarilloBoton,
                    timeSelectorSelectedContentColor = FigmaColors.SobreAmarillo,
                    timeSelectorUnselectedContainerColor = FigmaColors.Hundido,
                    timeSelectorUnselectedContentColor = FigmaColors.Crema,
                    periodSelectorSelectedContainerColor = FigmaColors.AmarilloBoton,
                    periodSelectorSelectedContentColor = FigmaColors.SobreAmarillo,
                    periodSelectorUnselectedContentColor = FigmaColors.Crema,
                    clockDialUnselectedContentColor = FigmaColors.Crema,
                ),
                modifier = Modifier.testTag("closing_time_picker"),
            )
        },
        confirmButton = {
            TextButton(onClick = { onElegir(LocalTime.of(estado.hour, estado.minute)) }, modifier = Modifier.testTag("save_closing_time")) {
                Text("Guardar", color = FigmaColors.Amarillo, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar", color = FigmaColors.Dorado) } },
    )
}

/** "row-…" del Figma: fila plana con divisor #22593F, círculo de 40 y monto verde (entra) o naranja (sale). */
@Composable
private fun FilaMovimiento(titulo: String, subtitulo: String, monto: String, entra: Boolean, divisor: Boolean) {
    val color = if (entra) FigmaColors.Verde else FigmaColors.Naranja
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(FigmaColors.Tarjeta), contentAlignment = Alignment.Center) {
                Icon(if (entra) Icons.Filled.SouthWest else Icons.Filled.NorthEast, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, color = FigmaColors.Crema, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(subtitulo, color = FigmaColors.Dorado, fontSize = 13.sp)
            }
            Text(monto, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        if (divisor) Box(Modifier.fillMaxWidth().height(1.dp).background(FigmaColors.Tarjeta))
    }
}

/** a1 · Arqueo de caja: conteo parcial sin cerrar. La diferencia se calcula mientras se escribe. */
@Composable
private fun ArqueoView(expectedAmount: Long, perfil: String, onBack: () -> Unit, onSave: (Long, String) -> Unit) {
    var contado by rememberSaveable { mutableStateOf("") }
    var nota by rememberSaveable { mutableStateOf("") }
    var tocado by rememberSaveable { mutableStateOf(false) }
    val valor = contado.toLongOrNull()
    val problema = ReglasCaja.problemaConteo(valor, nota)
    Column(
        Modifier.fillMaxSize().background(BarrioColors.Background).systemBarsPadding().imePadding().testTag("arqueo_screen"),
    ) {
        CabeceraFigma("Arqueo de caja", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FigmaColors.Tarjeta).padding(18.dp)) {
                Text("Saldo esperado por el sistema", color = FigmaColors.Dorado, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(Money.cop(expectedAmount), color = FigmaColors.Amarillo, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Text("Conteo de efectivo real", color = FigmaColors.Dorado, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            CampoArqueo(contado, { contado = it.filter(Char::isDigit).take(10); tocado = true }, "0", numerico = true, error = tocado && problema != null, tag = "real_cash_count_input")
            if (tocado && problema != null) Text(problema, color = FigmaColors.Naranja, fontSize = 14.sp)
            valor?.let {
                val a = ReglasCaja.arqueo(expectedAmount, it)
                val color = when (a.resultado) {
                    ResultadoArqueo.CUADRADO -> FigmaColors.Verde
                    ResultadoArqueo.FALTANTE -> FigmaColors.Alerta
                    ResultadoArqueo.SOBRANTE -> FigmaColors.Naranja
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)).padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(a.resultado.etiqueta, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(Money.cop(a.diferencia), color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("Observaciones", color = FigmaColors.Dorado, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            CampoArqueo(nota, { nota = it.take(ReglasCaja.OBSERVACIONES_MAXIMO + 20) }, "Ej. Billete de $ 5.000 no registrado", numerico = false, error = false, tag = "observation_input")
            Text("Realizado por: $perfil · ${Dates.full()}", color = FigmaColors.Tenue, fontSize = 12.sp)
        }
        BotonPaso(
            "Guardar arqueo",
            habilitado = problema == null,
            modifier = Modifier.padding(24.dp).testTag("save_arqueo_button"),
        ) {
            tocado = true
            if (problema == null) onSave(valor ?: 0L, nota)
        }
    }
}

@Composable
private fun CampoArqueo(valor: String, onValor: (String) -> Unit, placeholder: String, numerico: Boolean, error: Boolean, tag: String) {
    val forma = RoundedCornerShape(12.dp)
    BasicTextField(
        value = valor,
        onValueChange = onValor,
        singleLine = numerico,
        textStyle = TextStyle(fontFamily = Inter, color = FigmaColors.Crema, fontSize = if (numerico) 18.sp else 15.sp, fontWeight = if (numerico) FontWeight.Bold else FontWeight.Normal),
        cursorBrush = SolidColor(FigmaColors.AmarilloBoton),
        keyboardOptions = KeyboardOptions(keyboardType = if (numerico) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth().testTag(tag),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(forma).background(FigmaColors.Campo)
                    .border(if (error) 2.dp else 1.dp, if (error) FigmaColors.Naranja else FigmaColors.BordeCampo, forma).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (numerico) Text("$ ", color = FigmaColors.Amarillo, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.weight(1f)) {
                    if (valor.isEmpty()) Text(placeholder, color = FigmaColors.Tenue, fontSize = 15.sp)
                    inner()
                }
            }
        },
    )
}
