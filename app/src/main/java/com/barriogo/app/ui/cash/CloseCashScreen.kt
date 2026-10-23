package com.barriogo.app.ui.cash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.data.CashSessionEntity
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.MAX_CLOSING_CHANGES
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.ReglasCaja
import com.barriogo.app.domain.ResultadoArqueo
import com.barriogo.app.domain.TotalesCaja
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AvisoDialogo
import com.barriogo.app.ui.components.BotonPaso
import com.barriogo.app.ui.components.CabeceraFigma
import com.barriogo.app.ui.components.FilaResumen
import com.barriogo.app.ui.components.LineaResumen
import com.barriogo.app.ui.components.LoadingOverlay
import com.barriogo.app.ui.components.PildoraPaso
import com.barriogo.app.ui.components.TarjetaResumen
import com.barriogo.app.ui.components.TextLinkButton
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.FigmaColors
import com.barriogo.app.ui.theme.Inter

/**
 * c2 · Arqueo de cierre → c3 · Resumen → c4 · Comprobante. Cualquier perfil puede cerrar.
 * Con [corregir] = true se corrige el último cierre (efectivo contado y observaciones), máximo 2 veces
 * y antes de abrir la caja siguiente.
 */
@Composable
fun CloseCashScreen(onBack: () -> Unit, onGoHome: () -> Unit, onCorregir: () -> Unit = {}, corregir: Boolean = false) {
    val vm = barrioViewModel { CashViewModel(it) }
    val s by vm.state.collectAsState()
    val ocupado by vm.ocupado.collectAsState()
    val error by vm.error.collectAsState()

    var paso by rememberSaveable { mutableIntStateOf(1) }
    var contado by rememberSaveable { mutableStateOf("") }
    var observaciones by rememberSaveable { mutableStateOf("") }
    var tocado by rememberSaveable { mutableStateOf(false) }
    var confirmarCorreccion by rememberSaveable { mutableStateOf(false) }
    var prellenado by rememberSaveable { mutableStateOf(false) }

    // Corregir: el formulario arranca con lo que se contó. Sin caja abierta y ya cerrada hoy: comprobante.
    LaunchedEffect(s.cargado) {
        if (!s.cargado || prellenado) return@LaunchedEffect
        prellenado = true
        val cerrada = s.ultimaCerrada
        if (corregir && cerrada != null) {
            contado = (cerrada.countedAmount ?: 0L).toString()
            observaciones = cerrada.closeNote
        } else if (!corregir && s.session == null && cerrada != null) {
            paso = 3
        }
    }

    BackHandler(enabled = paso == 2) { paso = 1 }

    val cerrada = s.ultimaCerrada
    val esperado = if (corregir) cerrada?.expectedAtClose ?: s.totalesCierre?.saldoEsperado ?: 0L else s.saldo
    val contadoLong = contado.toLongOrNull()
    val problema = ReglasCaja.problemaConteo(contadoLong, observaciones)
    val sinCambios = corregir && cerrada != null && contadoLong != null &&
        !ReglasCaja.hayCambiosEnCierre(cerrada.countedAmount ?: 0L, cerrada.closeNote, contadoLong, observaciones)

    Box(Modifier.fillMaxSize().background(BarrioColors.Background).statusBarsPadding().imePadding().testTag("close_cash_screen")) {
        Column(Modifier.fillMaxSize()) {
            CabeceraFigma(
                when {
                    paso == 3 -> "Comprobante de cierre"
                    corregir -> "Corregir cierre"
                    else -> "Cerrar caja"
                },
                onBack = when (paso) {
                    1 -> onBack
                    2 -> ({ paso = 1 })
                    else -> null
                },
            ) {
                when {
                    paso == 3 -> Unit
                    corregir -> PildoraPaso("Cambio ${(cerrada?.closeEditsUsed ?: 0) + 1} de $MAX_CLOSING_CHANGES")
                    else -> PildoraPaso("Paso $paso de 3")
                }
            }

            when {
                !s.cargado -> Unit
                paso == 3 && cerrada != null -> Comprobante(cerrada, s.totalesCierre, s.puedeCorregirCierre, s.cambiosCierreRestantes, onGoHome, onCorregir)
                corregir && !s.puedeCorregirCierre && cerrada != null && paso != 3 -> SinCaja(
                    if (s.session != null) "Ya se abrió otra caja: este cierre no se puede corregir."
                    else "Ya corregiste este cierre $MAX_CLOSING_CHANGES veces. No se puede cambiar más.",
                    onBack,
                )
                !corregir && s.session == null -> SinCaja("No hay una caja abierta para cerrar.", onBack)
                paso == 1 -> Conteo(
                    esperado = esperado,
                    contado = contado,
                    onContado = { contado = it.filter(Char::isDigit).take(10); tocado = true },
                    observaciones = observaciones,
                    onObservaciones = { observaciones = it.take(ReglasCaja.OBSERVACIONES_MAXIMO + 20) },
                    error = if (tocado) problema else null,
                    pie = when {
                        tocado && problema != null -> null
                        sinCambios -> "Todavía no has cambiado nada"
                        corregir -> "Solo puedes corregir el cierre $MAX_CLOSING_CHANGES veces."
                        else -> null
                    },
                    textoBoton = if (corregir) "Guardar corrección" else "Continuar",
                    habilitado = problema == null && !sinCambios,
                    onContinuar = {
                        tocado = true
                        if (problema == null && !sinCambios) {
                            if (corregir) confirmarCorreccion = true else paso = 2
                        }
                    },
                )
                paso == 2 -> s.totales?.let { t ->
                    Resumen(t, contadoLong ?: 0L) {
                        vm.cerrar(contadoLong ?: 0L, observaciones) { paso = 3 }
                    }
                }
            }
        }

        if (confirmarCorreccion) {
            val numero = (cerrada?.closeEditsUsed ?: 0) + 1
            AvisoDialogo(
                titulo = "¿Guardar la corrección $numero de $MAX_CLOSING_CHANGES?",
                mensaje = "El efectivo contado queda en ${Money.cop(contadoLong ?: 0L)}. " +
                    if (numero == MAX_CLOSING_CHANGES) "Después no se podrá corregir más." else "Te quedará 1 corrección.",
                principal = "Sí, guardar",
                onPrincipal = {
                    confirmarCorreccion = false
                    vm.corregirCierre(contadoLong ?: 0L, observaciones) { paso = 3 }
                },
                secundario = "Cancelar",
                onSecundario = { confirmarCorreccion = false },
            )
        }
        error?.let { AvisoDialogo(titulo = "No se pudo completar", mensaje = it, principal = "Entendido", onPrincipal = vm::cerrarError) }
        ocupado?.let { LoadingOverlay(it, detalle = if (it.startsWith("Cerrando")) "Estamos calculando el comprobante. No cierres la app." else "Esto toma solo unos segundos. No cierres la app.") }
    }
}

/** c2 · "Antes de cerrar, confirma el efectivo en caja." */
@Composable
private fun Conteo(
    esperado: Long,
    contado: String,
    onContado: (String) -> Unit,
    observaciones: String,
    onObservaciones: (String) -> Unit,
    error: String?,
    pie: String?,
    textoBoton: String,
    habilitado: Boolean,
    onContinuar: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Antes de cerrar, confirma el efectivo en caja.", color = FigmaColors.Cuerpo, fontSize = 14.sp)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(FigmaColors.Tarjeta).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Saldo esperado por el sistema", color = FigmaColors.Dorado, fontSize = 13.sp)
            Text(Money.cop(esperado), color = FigmaColors.Crema, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("expected_balance"))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Etiqueta("Efectivo contado")
            CampoTexto(contado, onContado, placeholder = "0", numerico = true, alto = 52, error = error != null, tag = "counted_input")
            if (error != null) Text(error, color = FigmaColors.Naranja, fontSize = 14.sp, modifier = Modifier.testTag("count_error"))
        }
        contado.toLongOrNull()?.let { TarjetaDiferencia(ReglasCaja.arqueo(esperado, it).diferencia) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Etiqueta("Observaciones")
            CampoTexto(observaciones, onObservaciones, placeholder = "Ej. Billete de $ 5.000 no registrado", numerico = false, alto = 52, error = false, tag = "notes_input")
        }
        BotonPaso(textoBoton, habilitado = habilitado, modifier = Modifier.testTag("continue_close"), onClick = onContinuar)
        if (pie != null) Text(pie, color = FigmaColors.Dorado, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/** c3 · Resumen del día y arqueo final, con la advertencia antes de confirmar. */
@Composable
private fun Resumen(t: TotalesCaja, contado: Long, onConfirmar: () -> Unit) {
    val arqueo = ReglasCaja.arqueo(t.saldoEsperado, contado)
    AlFondo(espacio = 14) {
        TarjetaResumen("Resumen del día") { FilasTotales(t) }
        TarjetaResumen("Arqueo final") {
            FilaResumen("Efectivo contado", Money.cop(contado))
            FilaResumen("Diferencia", textoDiferencia(arqueo.diferencia, arqueo.resultado), colorValor = colorDiferencia(arqueo.resultado), negrita = true)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF28C44).copy(alpha = 0.13f))
                .border(1.dp, FigmaColors.Alerta, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.WarningAmber, null, tint = FigmaColors.Alerta, modifier = Modifier.size(18.dp))
            Text(
                "Al cerrar, no podrás registrar nuevos movimientos hasta abrir una nueva caja.",
                color = FigmaColors.Crema, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.weight(1f))
        BotonPaso("Confirmar cierre", modifier = Modifier.padding(top = 24.dp).testTag("confirm_close"), onClick = onConfirmar)
    }
}

/** c4 · Comprobante de cierre. */
@Composable
private fun Comprobante(
    caja: CashSessionEntity,
    totales: TotalesCaja?,
    puedeCorregir: Boolean,
    restantes: Int,
    onGoHome: () -> Unit,
    onCorregir: () -> Unit,
) {
    val contado = caja.countedAmount ?: 0L
    val esperado = caja.expectedAtClose ?: totales?.saldoEsperado ?: 0L
    val arqueo = ReglasCaja.arqueo(esperado, contado)
    AlFondo(espacio = 16, centrado = true) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(FigmaColors.Tarjeta).border(2.dp, FigmaColors.Verde, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Check, null, tint = FigmaColors.Verde, modifier = Modifier.size(28.dp)) }
        Text(
            if (caja.closeEditsUsed > 0) "Cierre corregido" else "Caja cerrada exitosamente",
            color = FigmaColors.Crema, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-6).dp).testTag("close_success"),
        )
        TarjetaResumen("Detalle del comprobante") {
            FilaResumen("Fecha y hora", Dates.full(caja.closedAt ?: System.currentTimeMillis()))
            FilaResumen("Responsable", caja.closedBy ?: "—")
            LineaResumen()
            totales?.let { FilasTotales(it, conSaldo = false) }
            LineaResumen()
            FilaResumen("Saldo esperado", Money.cop(esperado), negrita = true)
            FilaResumen("Efectivo contado", Money.cop(contado), negrita = true)
            FilaResumen("Diferencia", textoDiferencia(arqueo.diferencia, arqueo.resultado), colorValor = colorDiferencia(arqueo.resultado), destacada = true)
            if (caja.closeNote.isNotBlank()) FilaResumen("Observaciones", caja.closeNote)
            if (caja.closeEditsUsed > 0) FilaResumen("Correcciones", "${caja.closeEditsUsed} de $MAX_CLOSING_CHANGES")
        }
        Spacer(Modifier.weight(1f))
        BotonPaso("Ir al inicio", modifier = Modifier.padding(top = 24.dp).testTag("go_home"), onClick = onGoHome)
        if (puedeCorregir) {
            TextLinkButton(
                "Corregir cierre (te ${if (restantes == 1) "queda 1" else "quedan $restantes"})",
                color = FigmaColors.Dorado,
                modifier = Modifier.testTag("correct_close"),
                onClick = onCorregir,
            )
        }
    }
}

/** Columna con scroll que ocupa al menos toda la pantalla: un Spacer(weight) deja el botón al fondo, como en el Figma. */
@Composable
private fun AlFondo(espacio: Int, centrado: Boolean = false, contenido: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val alto = maxHeight
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = alto).padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(espacio.dp),
                horizontalAlignment = if (centrado) Alignment.CenterHorizontally else Alignment.Start,
                content = contenido,
            )
        }
    }
}

@Composable
private fun FilasTotales(t: TotalesCaja, conSaldo: Boolean = true) {
    FilaResumen("Apertura de caja", Money.cop(t.apertura))
    FilaResumen("Ventas en efectivo", Money.signed(t.ventasEfectivo), colorValor = FigmaColors.Verde)
    FilaResumen("Gastos", "-" + Money.cop(t.gastos), colorValor = FigmaColors.Naranja)
    FilaResumen("Abonos recibidos", Money.signed(t.abonosEfectivo), colorValor = FigmaColors.Verde)
    if (t.ajustes != 0L) FilaResumen("Ajustes de caja", Money.signed(t.ajustes), colorValor = if (t.ajustes > 0) FigmaColors.Verde else FigmaColors.Naranja)
    if (t.ventasTransferencia > 0) FilaResumen("Ventas por transferencia (no entran a caja)", Money.cop(t.ventasTransferencia), colorValor = FigmaColors.Tenue)
    if (conSaldo) {
        LineaResumen()
        FilaResumen("Saldo esperado", Money.cop(t.saldoEsperado), colorValor = FigmaColors.Amarillo, destacada = true)
    }
}

@Composable
private fun TarjetaDiferencia(diferencia: Long) {
    val r = ReglasCaja.arqueo(0, diferencia).resultado
    val color = colorDiferencia(r)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("difference_card"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Resultado", color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(
            if (r == ResultadoArqueo.CUADRADO) "Cuadrado" else "${r.etiqueta}: ${Money.cop(diferencia)}",
            color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun colorDiferencia(r: ResultadoArqueo) = when (r) {
    ResultadoArqueo.CUADRADO -> FigmaColors.Verde
    ResultadoArqueo.FALTANTE -> FigmaColors.Alerta
    ResultadoArqueo.SOBRANTE -> FigmaColors.Naranja
}

private fun textoDiferencia(diferencia: Long, r: ResultadoArqueo) =
    if (r == ResultadoArqueo.CUADRADO) "$ 0 (Cuadrado)" else "${Money.cop(diferencia)} (${r.etiqueta})"

@Composable
private fun Etiqueta(texto: String) {
    Text(texto, color = FigmaColors.Dorado, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 0.dp))
}

/** "InputField" de c2: #1C4E36, borde blanco al 18 %, esquinas 12; el monto lleva "$" amarillo. */
@Composable
private fun CampoTexto(valor: String, onValor: (String) -> Unit, placeholder: String, numerico: Boolean, alto: Int, error: Boolean, tag: String) {
    val forma = RoundedCornerShape(12.dp)
    BasicTextField(
        value = valor,
        onValueChange = onValor,
        singleLine = numerico,
        textStyle = TextStyle(
            fontFamily = Inter, color = FigmaColors.Crema,
            fontSize = if (numerico) 18.sp else 16.sp, fontWeight = if (numerico) FontWeight.Bold else FontWeight.Normal,
        ),
        cursorBrush = SolidColor(FigmaColors.AmarilloBoton),
        visualTransformation = if (numerico) MilesTransformation else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (numerico) KeyboardType.Number else KeyboardType.Text, imeAction = if (numerico) ImeAction.Next else ImeAction.Done),
        modifier = Modifier.fillMaxWidth().testTag(tag),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = alto.dp)
                    .clip(forma)
                    .background(FigmaColors.Campo)
                    .border(if (error) 2.dp else 1.dp, if (error) FigmaColors.Naranja else FigmaColors.BordeCampo, forma)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (numerico) Text("$ ", color = FigmaColors.Amarillo, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.weight(1f)) {
                    if (valor.isEmpty()) Text(placeholder, color = FigmaColors.Tenue, fontSize = if (numerico) 18.sp else 15.sp)
                    inner()
                }
            }
        },
    )
}

/** Separador de miles mientras se escribe ("540000" → "540.000"). */
private object MilesTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitos = text.text
        val salida = if (digitos.isEmpty()) "" else Money.group(digitos.toLong())
        val mapa = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = salida.length
            override fun transformedToOriginal(offset: Int) = digitos.length
        }
        return TransformedText(AnnotatedString(salida), mapa)
    }
}

@Composable
private fun SinCaja(texto: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(texto, color = FigmaColors.Crema, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        BotonPaso("Volver", onClick = onBack)
    }
}
