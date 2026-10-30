package com.barriogo.app.ui.fiados

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.domain.EstadoPagoFiado
import com.barriogo.app.domain.Fiados
import com.barriogo.app.domain.PagoFiado
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import com.barriogo.app.domain.kind
import com.barriogo.app.domain.pay
import com.barriogo.app.ui.components.BotonFigma
import com.barriogo.app.ui.components.MoneyVisualTransformation
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.FigmaColors

/** Marco común de los diálogos de Fiados: fondo de la app, borde dorado, título amarillo y "Cancelar". */
@Composable
private fun MarcoDialogo(titulo: String, tag: String, onCancelar: () -> Unit, contenido: @Composable ColumnScope.() -> Unit) {
    val forma = RoundedCornerShape(20.dp)
    Dialog(onDismissRequest = onCancelar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 364.dp)
                .fillMaxWidth()
                .shadow(24.dp, forma, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.6f))
                .clip(forma)
                .background(BarrioColors.Background)
                .border(1.dp, FigmaColors.Dorado, forma)
                .verticalScroll(rememberScrollState())
                .padding(22.dp)
                .testTag(tag),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(titulo, color = FigmaColors.Amarillo, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            contenido()
            Text(
                "Cancelar",
                color = FigmaColors.Dorado, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onCancelar).padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun ErrorCampo(texto: String?) {
    if (texto != null) Text(texto, color = FigmaColors.Naranja, fontSize = 13.sp)
}

/**
 * "+ Agregar cliente" y "Editar datos": nombre y celular obligatorios (7 a 10 dígitos) y dirección
 * opcional. Mismas reglas que Fiados.problemasCliente() y firestore.rules → clienteValido().
 */
@Composable
internal fun DialogoCliente(
    titulo: String,
    boton: String,
    nombreInicial: String,
    celularInicial: String,
    direccionInicial: String,
    onCancelar: () -> Unit,
    onGuardar: (nombre: String, celular: String, direccion: String) -> Unit,
) {
    var nombre by rememberSaveable { mutableStateOf(nombreInicial) }
    var celular by rememberSaveable { mutableStateOf(celularInicial) }
    var direccion by rememberSaveable { mutableStateOf(direccionInicial) }
    var intento by rememberSaveable { mutableStateOf(false) }
    var enviando by rememberSaveable { mutableStateOf(false) }
    val problemas = Fiados.problemasCliente(nombre, celular, direccion)

    MarcoDialogo(titulo, "dialogo_cliente", onCancelar) {
        EtiquetaFiado("Nombre")
        CampoFiado(nombre, { nombre = it.take(60) }, "Ej. Marta Ramírez", Modifier.testTag("cliente_nombre"), error = intento && "nombre" in problemas)
        if (intento) ErrorCampo(problemas["nombre"])
        Spacer(Modifier.height(4.dp))
        EtiquetaFiado("Celular")
        CampoFiado(
            celular, { celular = it.filter(Char::isDigit).take(10) }, "Ej. 300 456 7890",
            Modifier.testTag("cliente_celular"), teclado = KeyboardType.Phone, error = intento && "celular" in problemas,
        )
        if (intento) ErrorCampo(problemas["celular"])
        Spacer(Modifier.height(4.dp))
        EtiquetaFiado("Dirección (opcional)")
        CampoFiado(
            direccion, { direccion = it.take(200) }, "Ej. Calle 8 # 4-10", Modifier.testTag("cliente_direccion"),
            accion = ImeAction.Done, error = intento && "direccion" in problemas,
        )
        if (intento) ErrorCampo(problemas["direccion"])
        Spacer(Modifier.height(8.dp))
        BotonFigma(boton, Modifier.testTag("guardar_cliente"), tamanoTexto = 15) {
            intento = true
            if (problemas.isEmpty() && !enviando) {
                enviando = true
                onGuardar(nombre.trim(), celular, direccion.trim())
            }
        }
    }
}

/** "Cambiar cupo" (solo administrador): nunca por debajo de $ 50.000. */
@Composable
internal fun DialogoCupo(nombre: String, cupoActual: Long, saldo: Long, onCancelar: () -> Unit, onGuardar: (Long) -> Unit) {
    var texto by rememberSaveable { mutableStateOf(cupoActual.toString()) }
    var intento by rememberSaveable { mutableStateOf(false) }
    val cupo = texto.toLongOrNull() ?: 0L
    val problema = Fiados.problemaNuevoCupo(cupoActual, cupo)

    MarcoDialogo("Cupo de fiado", "dialogo_cupo", onCancelar) {
        Text(
            "Lo máximo que $nombre puede deber. Hoy debe ${Money.cop(saldo)}.",
            color = FigmaColors.Cuerpo, fontSize = 14.sp, lineHeight = 19.sp,
        )
        Spacer(Modifier.height(4.dp))
        EtiquetaFiado("Nuevo cupo")
        CampoFiado(
            texto, { texto = it.filter(Char::isDigit).take(10).trimStart('0') }, "$ 50.000", Modifier.testTag("cupo_input"),
            teclado = KeyboardType.Number, accion = ImeAction.Done,
            transformacion = if (texto.isEmpty()) VisualTransformation.None else MoneyVisualTransformation(),
            error = intento && !problema.isNullOrEmpty(), montoGrande = true,
        )
        if (intento && !problema.isNullOrEmpty()) ErrorCampo(problema)
        Spacer(Modifier.height(8.dp))
        BotonFigma("Guardar cupo", Modifier.testTag("guardar_cupo"), habilitado = problema != "", tamanoTexto = 15) {
            intento = true
            if (problema == null) onGuardar(cupo)
        }
    }
}

/**
 * Tocar un fiado o abono en la ficha: abonar solo a ese fiado, corregir el valor y lo que se llevó
 * (fiado) o el método de pago (abono), o anularlo si se registró por error. Lo anulado queda
 * tachado, no se borra.
 */
@Composable
internal fun DialogoMovimientoFiado(
    m: MovementEntity,
    onCancelar: () -> Unit,
    onGuardarFiado: (queSeLlevo: String, monto: Long) -> Unit,
    onGuardarAbono: (monto: Long, metodo: PayMethod) -> Unit,
    onAnular: () -> Unit,
    /** Estado de pago del fiado, para saber si todavía se le puede abonar. */
    pago: PagoFiado? = null,
    onAbonar: (() -> Unit)? = null,
) {
    val esFiado = m.kind == MovementType.FIADO
    var montoTexto by rememberSaveable { mutableStateOf(m.amount.toString()) }
    var queSeLlevo by rememberSaveable { mutableStateOf(m.detail) }
    var metodo by rememberSaveable { mutableStateOf(m.pay) }
    val monto = montoTexto.toLongOrNull() ?: 0L
    val cambio = monto != m.amount || (if (esFiado) queSeLlevo.trim() != m.detail else metodo != m.pay)
    val listo = monto > 0 && (!esFiado || queSeLlevo.isNotBlank()) && cambio

    MarcoDialogo(if (esFiado) "Este fiado" else "Editar abono", "dialogo_movimiento_fiado", onCancelar) {
        Text(Fiados.cuando(m.createdAt), color = FigmaColors.Dorado, fontSize = 12.sp)
        if (esFiado && pago != null) {
            Text(
                when (pago.estado) {
                    EstadoPagoFiado.PAGADO -> "Pagado por completo"
                    EstadoPagoFiado.ABONADO -> "Abonado · faltan ${Money.cop(pago.porPagar)}"
                    EstadoPagoFiado.PENDIENTE -> "Pendiente · faltan ${Money.cop(pago.porPagar)}"
                },
                color = if (pago.estado == EstadoPagoFiado.PAGADO) FigmaColors.Verde else FigmaColors.Crema, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )
            if (onAbonar != null && pago.estado != EstadoPagoFiado.PAGADO) {
                Spacer(Modifier.height(4.dp))
                BotonFigma("Abonar solo a este fiado", Modifier.testTag("abonar_a_fiado"), tamanoTexto = 15, onClick = onAbonar)
                Spacer(Modifier.height(8.dp))
                SeccionFiado("Corregir")
            }
        }
        EtiquetaFiado(if (esFiado) "¿Cuánto fió?" else "¿Cuánto abonó?")
        CampoFiado(
            montoTexto, { montoTexto = it.filter(Char::isDigit).take(9).trimStart('0') }, "$ 0", Modifier.testTag("editar_monto_fiado"),
            teclado = KeyboardType.Number,
            transformacion = if (montoTexto.isEmpty()) VisualTransformation.None else MoneyVisualTransformation(),
        )
        if (esFiado) {
            Spacer(Modifier.height(4.dp))
            EtiquetaFiado("¿Qué se llevó?")
            CampoFiado(
                queSeLlevo, { queSeLlevo = it.take(200) }, "Ej. leche, pan, huevos", Modifier.testTag("editar_que_se_llevo"),
                alto = 88.dp, unaLinea = false, accion = ImeAction.Default, error = queSeLlevo.isBlank(),
            )
        } else {
            Spacer(Modifier.height(4.dp))
            EtiquetaFiado("¿Cómo pagó?")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OpcionMetodo("Efectivo", metodo == PayMethod.EFECTIVO, Modifier.weight(1f)) { metodo = PayMethod.EFECTIVO }
                OpcionMetodo("Transferencia", metodo == PayMethod.TRANSFERENCIA, Modifier.weight(1f)) { metodo = PayMethod.TRANSFERENCIA }
            }
        }
        Spacer(Modifier.height(8.dp))
        BotonFigma("Guardar cambios", Modifier.testTag("guardar_movimiento_fiado"), habilitado = listo, tamanoTexto = 15) {
            if (!listo) return@BotonFigma
            if (esFiado) onGuardarFiado(queSeLlevo.trim(), monto) else onGuardarAbono(monto, metodo)
        }
        BotonContornoFiado(if (esFiado) "Anular este fiado" else "Anular este abono", Modifier.testTag("anular_movimiento_fiado"), onClick = onAnular)
    }
}

@Composable
private fun OpcionMetodo(texto: String, elegida: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(48.dp)
            .clip(forma)
            .background(if (elegida) FigmaColors.AmarilloBoton else FigmaColors.Tarjeta)
            .border(1.dp, FigmaColors.Dorado, forma)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, color = if (elegida) FigmaColors.SobreAmarillo else FigmaColors.Crema, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
