package com.barriogo.app.ui.expenses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.domain.CampoGasto
import com.barriogo.app.domain.ExpenseKind
import com.barriogo.app.domain.ReglasGasto
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AvisoDialogo
import com.barriogo.app.ui.components.BotonFigma
import com.barriogo.app.ui.components.CabeceraFigma
import com.barriogo.app.ui.components.LoadingOverlay
import com.barriogo.app.ui.components.MoneyVisualTransformation
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.FigmaColors
import com.barriogo.app.ui.theme.Inter

/**
 * G2 · Registrar gasto a mano (y G2 · Editar gasto con [gastoId]).
 * Validaciones del Figma: tipo obligatorio, monto mayor a cero (hasta 1.000 millones) y en qué fue
 * el gasto (1 a 200 letras). Los avisos salen al tocar el campo o al intentar guardar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterExpenseScreen(onBack: () -> Unit, onSaved: (String) -> Unit, gastoId: Long? = null) {
    val vm = barrioViewModel { GastosViewModel(it) }
    val ocupado by vm.ocupado.collectAsState()
    val error by vm.error.collectAsState()
    val original by vm.original.collectAsState()
    val editando = gastoId != null

    var tipoNombre by rememberSaveable { mutableStateOf<String?>(null) }
    var montoTexto by rememberSaveable { mutableStateOf("") }
    var detalle by rememberSaveable { mutableStateOf("") }
    var intento by rememberSaveable { mutableStateOf(false) }
    var tocadoMonto by rememberSaveable { mutableStateOf(false) }
    var tocadoDetalle by rememberSaveable { mutableStateOf(false) }
    var salir by remember { mutableStateOf(false) }
    var cargado by rememberSaveable { mutableStateOf(!editando) }

    LaunchedEffect(gastoId) { gastoId?.let(vm::cargar) }
    LaunchedEffect(original) {
        val g = original ?: return@LaunchedEffect
        if (cargado) return@LaunchedEffect
        val e = vm.editable(g)
        tipoNombre = e.tipo.name
        montoTexto = e.monto.toString()
        detalle = e.detalle
        cargado = true
    }

    val tipo = tipoNombre?.let(ExpenseKind::valueOf)
    val monto = montoTexto.toLongOrNull() ?: 0L
    val base = original?.let(vm::editable)
    val problemas = ReglasGasto.problemas(tipo, monto, detalle)
    val puedeGuardar = cargado && ReglasGasto.puedeGuardar(tipo, monto, detalle, base)
    val hayDatos = if (editando) base != null && ReglasGasto.hayCambios(base, tipo, monto, detalle) else tipo != null || monto > 0 || detalle.isNotBlank()
    val foco = LocalFocusManager.current
    val focoDetalle = remember { FocusRequester() }

    // Salir con datos sin guardar pide confirmación.
    BackHandler(enabled = hayDatos && ocupado == null) { salir = true }
    val volver = { if (hayDatos) salir = true else onBack() }

    fun guardar() {
        intento = true
        if (!puedeGuardar) return
        foco.clearFocus()
        if (gastoId != null) vm.editar(gastoId, tipo, monto, detalle) { onSaved("Gasto actualizado. Tu caja se actualizó.") }
        else vm.registrar(tipo, monto, detalle) { onSaved("Gasto guardado. Ya resta en tu caja.") }
    }

    Box(Modifier.fillMaxSize().background(BarrioColors.Background).statusBarsPadding().imePadding().testTag("register_expense_screen")) {
        Column(Modifier.fillMaxSize()) {
            CabeceraFigma(if (editando) "Editar gasto" else "Registrar gasto", onBack = volver)
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text("¿Qué tipo de gasto fue?", color = FigmaColors.Crema, fontSize = 15.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExpenseKind.entries.forEach { k ->
                        Categoria(k.label, elegida = k == tipo, apagada = tipo != null && k != tipo) { tipoNombre = k.name }
                    }
                }
                Aviso(if (intento) problemas[CampoGasto.TIPO] else null, ReglasGasto.FALTA_TIPO)

                Column(Modifier.alpha(if (tipo == null) 0.5f else 1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Cuánto pagaste?", color = FigmaColors.Crema, fontSize = 15.sp)
                    CampoMonto(
                        valor = montoTexto,
                        onValor = { montoTexto = it.filter(Char::isDigit).trimStart('0').take(10); tocadoMonto = true },
                        error = (intento || tocadoMonto) && CampoGasto.MONTO in problemas,
                        onSiguiente = { focoDetalle.requestFocus() },
                    )
                    Aviso(if (intento || tocadoMonto) problemas[CampoGasto.MONTO] else null, ReglasGasto.FALTA_MONTO)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿En qué fue el gasto?", color = FigmaColors.Crema, fontSize = 15.sp)
                    CampoDetalle(
                        valor = detalle,
                        onValor = { detalle = it.take(ReglasGasto.DETALLE_MAXIMO + 20); tocadoDetalle = true },
                        error = (intento || tocadoDetalle) && CampoGasto.DETALLE in problemas,
                        modifier = Modifier.focusRequester(focoDetalle),
                        onListo = { guardar() },
                    )
                    Aviso(if (intento || tocadoDetalle) problemas[CampoGasto.DETALLE] else null, ReglasGasto.FALTA_DETALLE)
                }
                Spacer(Modifier.height(8.dp))
            }

            Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonFigma(if (editando) "Guardar cambios" else "Guardar gasto", habilitado = puedeGuardar, modifier = Modifier.testTag("save_expense"), onClick = ::guardar)
                Text(
                    ReglasGasto.estadoBoton(tipo, monto, detalle, base, intento),
                    color = when {
                        puedeGuardar -> FigmaColors.Verde
                        intento && problemas.isNotEmpty() -> FigmaColors.Naranja
                        else -> FigmaColors.Dorado
                    },
                    fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().testTag("expense_status"),
                )
            }
        }

        if (salir) {
            AvisoDialogo(
                titulo = "¿Salir sin guardar?",
                mensaje = if (editando) "Los cambios de este gasto se perderán." else "El gasto que estabas escribiendo no se guardará.",
                principal = "Seguir editando",
                onPrincipal = { salir = false },
                secundario = "Salir",
                onSecundario = {
                    salir = false
                    onBack()
                },
                onCerrar = { salir = false },
            )
        }
        error?.let { AvisoDialogo(titulo = "No se pudo guardar", mensaje = it, principal = "Entendido", onPrincipal = vm::cerrarError) }
        ocupado?.let { LoadingOverlay(it) }
    }
}

/** "Cat Proveedor": píldora #22593F; elegida con borde amarillo de 2 y texto amarillo, las demás al 50 %. */
@Composable
private fun Categoria(texto: String, elegida: Boolean, apagada: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(999.dp)
    Text(
        texto,
        color = if (elegida) FigmaColors.Amarillo else FigmaColors.Crema,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .alpha(if (apagada) 0.5f else 1f)
            .clip(forma)
            .background(FigmaColors.Tarjeta)
            .border(if (elegida) 2.dp else 1.dp, if (elegida) FigmaColors.AmarilloBoton else FigmaColors.Dorado, forma)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("categoria_$texto"),
    )
}

/** Mensaje de alerta con su espacio reservado (opacidad 0 cuando no hay error), como en el Figma. */
@Composable
private fun Aviso(error: String?, reservado: String) {
    Text(
        error ?: reservado,
        color = FigmaColors.Naranja,
        fontSize = 14.sp,
        modifier = Modifier.alpha(if (error != null) 1f else 0f).testTag("aviso_$reservado"),
    )
}

@Composable
private fun CampoMonto(valor: String, onValor: (String) -> Unit, error: Boolean, onSiguiente: () -> Unit) {
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    val activo = enfocado || valor.isNotEmpty()
    val forma = RoundedCornerShape(12.dp)
    BasicTextField(
        value = valor,
        onValueChange = onValor,
        singleLine = true,
        interactionSource = interaccion,
        textStyle = TextStyle(fontFamily = Inter, color = FigmaColors.Crema, fontSize = 26.sp, fontWeight = FontWeight.Bold),
        cursorBrush = SolidColor(FigmaColors.AmarilloBoton),
        visualTransformation = if (valor.isEmpty()) VisualTransformation.None else MoneyVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onSiguiente() }),
        modifier = Modifier.fillMaxWidth().testTag("expense_amount"),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(forma)
                    .background(FigmaColors.Tarjeta)
                    .border(
                        if (error || activo) 2.dp else 1.dp,
                        when {
                            error -> FigmaColors.Naranja
                            activo -> FigmaColors.AmarilloBoton
                            else -> FigmaColors.Dorado
                        },
                        forma,
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (valor.isEmpty()) Text("$ 0", color = FigmaColors.Tenue, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                inner()
            }
        },
    )
}

@Composable
private fun CampoDetalle(valor: String, onValor: (String) -> Unit, error: Boolean, modifier: Modifier, onListo: () -> Unit) {
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    val activo = enfocado || valor.isNotEmpty()
    val forma = RoundedCornerShape(12.dp)
    BasicTextField(
        value = valor,
        onValueChange = onValor,
        singleLine = true,
        interactionSource = interaccion,
        textStyle = TextStyle(fontFamily = Inter, color = FigmaColors.Crema, fontSize = 16.sp),
        cursorBrush = SolidColor(FigmaColors.AmarilloBoton),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onListo() }),
        modifier = modifier.fillMaxWidth().testTag("expense_detail_input"),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(forma)
                    .background(FigmaColors.Tarjeta)
                    .border(
                        if (error || activo) 2.dp else 1.dp,
                        when {
                            error -> FigmaColors.Naranja
                            activo -> FigmaColors.AmarilloBoton
                            else -> FigmaColors.Dorado
                        },
                        forma,
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (valor.isEmpty()) Text("Ej. Postobón, recibo del gas", color = FigmaColors.Tenue, fontSize = 16.sp)
                inner()
            }
        },
    )
}
