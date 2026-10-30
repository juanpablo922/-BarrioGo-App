package com.barriogo.app.ui.fiados

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.domain.EstadoPagoFiado
import com.barriogo.app.domain.FiadoPendiente
import com.barriogo.app.domain.Fiados
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PagoFiado
import com.barriogo.app.domain.kind
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.FigmaColors

/**
 * Barra "1 — 2 — 3" de Nuevo fiado: los pasos hechos van en verde con ✓, el actual en amarillo
 * con su número y los que faltan con borde dorado. La línea queda verde hasta el paso actual.
 */
@Composable
internal fun PasosFiado(actual: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        for (paso in 1..3) {
            PuntoPaso(paso, actual)
            if (paso < 3) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(if (paso < actual) FigmaColors.Verde else FigmaColors.Dorado.copy(alpha = 0.6f))
                )
            }
        }
    }
}

@Composable
private fun PuntoPaso(paso: Int, actual: Int) {
    val base = Modifier.size(24.dp).clip(CircleShape)
    when {
        paso < actual -> Box(base.background(FigmaColors.Verde), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, null, tint = FigmaColors.SobreAmarillo, modifier = Modifier.size(14.dp))
        }
        paso == actual -> Box(base.background(FigmaColors.AmarilloBoton), contentAlignment = Alignment.Center) {
            Text("$paso", color = FigmaColors.SobreAmarillo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        else -> Box(base.border(1.5.dp, FigmaColors.Dorado, CircleShape), contentAlignment = Alignment.Center) {
            Text("$paso", color = FigmaColors.Dorado, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Etiqueta de campo: "¿Cuánto va a fiar?", "Buscar por nombre o celular". */
@Composable
internal fun EtiquetaFiado(texto: String, modifier: Modifier = Modifier) {
    Text(texto, color = FigmaColors.Crema, fontSize = 15.sp, modifier = modifier)
}

/** Título dorado de sección: "Clientes recientes", "Últimos movimientos". */
@Composable
internal fun SeccionFiado(texto: String, modifier: Modifier = Modifier) {
    Text(texto, color = FigmaColors.Dorado, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

/**
 * Campo del Figma: fondo #22593F, borde dorado de 1 (amarillo de 2 si hay error), esquinas 12,
 * 56 de alto (o [alto] para "¿Qué se llevó?") y placeholder tenue de 16.
 */
@Composable
internal fun CampoFiado(
    valor: String,
    onCambio: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    alto: Dp = 56.dp,
    unaLinea: Boolean = true,
    teclado: KeyboardType = KeyboardType.Text,
    accion: ImeAction = ImeAction.Next,
    transformacion: VisualTransformation = VisualTransformation.None,
    error: Boolean = false,
    /** Monto escrito en grande ("$ 20.000" Bold 20 de Registrar pago). */
    montoGrande: Boolean = false,
) {
    val forma = RoundedCornerShape(12.dp)
    BasicTextField(
        value = valor,
        onValueChange = onCambio,
        singleLine = unaLinea,
        maxLines = if (unaLinea) 1 else 3,
        textStyle = if (montoGrande) TextStyle(color = FigmaColors.Crema, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        else TextStyle(color = FigmaColors.Crema, fontSize = 16.sp),
        cursorBrush = SolidColor(FigmaColors.Amarillo),
        visualTransformation = transformacion,
        keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = accion),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { campo ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(alto)
                    .clip(forma)
                    .background(FigmaColors.Tarjeta)
                    .border(if (error) 2.dp else 1.dp, if (error) FigmaColors.Naranja else FigmaColors.Dorado, forma)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (valor.isEmpty()) {
                    Text(placeholder, color = if (BarrioColors.isDark) Color(0x99E6E2DC) else FigmaColors.Tenue, fontSize = 16.sp)
                }
                campo()
            }
        },
    )
}

/** "+ Agregar cliente", "Registrar pago", "Volver a Fiado": fondo #22593F, borde dorado y texto amarillo Bold 15. */
@Composable
internal fun BotonContornoFiado(texto: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(forma)
            .background(FigmaColors.Tarjeta)
            .border(1.dp, FigmaColors.Dorado, forma)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(texto, color = FigmaColors.Amarillo, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
}

/** Círculo con las iniciales del cliente: 40 verde con letras amarillas en las listas, 56 amarillo en la ficha. */
@Composable
internal fun AvatarCliente(nombre: String, grande: Boolean = false) {
    Box(
        Modifier
            .size(if (grande) 56.dp else 40.dp)
            .clip(CircleShape)
            .background(if (grande) FigmaColors.AmarilloBoton else FigmaColors.Tarjeta),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            Fiados.iniciales(nombre),
            color = if (grande) FigmaColors.SobreAmarillo else FigmaColors.Amarillo,
            fontSize = if (grande) 17.sp else 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Fila de "Clientes recientes": iniciales, nombre, celular (o "Sin saldo pendiente") y lo que debe. */
@Composable
internal fun FilaCliente(c: FiadoPendiente, ultima: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(88.dp)
            .lineaAbajo(!ultima, FigmaColors.Tarjeta)
            .clickable(onClick = onClick)
            .testTag("cliente_${c.cliente.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AvatarCliente(c.cliente.name)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(c.cliente.name, color = FigmaColors.Crema, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when {
                    Fiados.enElLimite(c) -> "Llegó a su límite de fiado"
                    c.saldo <= 0 -> "Sin saldo pendiente"
                    c.cliente.phone.isNotBlank() -> c.cliente.phone
                    else -> "Sin celular"
                },
                color = if (Fiados.enElLimite(c)) FigmaColors.Naranja else FigmaColors.Dorado, fontSize = 12.sp, maxLines = 1,
            )
        }
        Text(Money.cop(c.saldo), color = FigmaColors.Crema, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** Fila de lista con la línea #22593F abajo (sin línea en la última). */
internal fun Modifier.lineaAbajo(mostrar: Boolean, color: Color): Modifier =
    if (!mostrar) this else drawBehind {
        drawLine(color, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), strokeWidth = 1.dp.toPx())
    }

/**
 * Movimiento de la ficha: ↑ amarillo "+$ 15.000" para fiados, ↓ verde "−$ 10.000" para abonos.
 * El fiado dice si está pendiente, abonado o pagado ([pago]). Con [onClick] se edita o anula.
 */
@Composable
internal fun FilaMovimientoFiado(m: MovementEntity, ultima: Boolean, pago: PagoFiado? = null, onClick: (() -> Unit)? = null) {
    val esFiado = m.kind == MovementType.FIADO
    val color = if (esFiado) FigmaColors.Amarillo else FigmaColors.Verde
    // Anulado: tachado y al 50 %, como lo eliminado en Movimientos
    val tachado = if (m.voided) TextDecoration.LineThrough else TextDecoration.None
    val estado = when {
        m.voided -> " · Anulado" + (m.voidedAt?.let { " " + Fiados.hora(it) } ?: "")
        pago == null -> ""
        pago.estado == EstadoPagoFiado.ABONADO -> " · Abonado, faltan ${Money.cop(pago.porPagar)}"
        else -> " · " + pago.estado.texto
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(76.dp)
            .lineaAbajo(!ultima, FigmaColors.Tarjeta)
            .then(if (onClick != null && !m.voided) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag("movimiento_fiado_${m.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.weight(1f).alpha(if (m.voided) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(if (esFiado) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    Fiados.titulo(m), color = FigmaColors.Crema, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textDecoration = tachado,
                )
                Text(
                    Fiados.cuando(m.createdAt) + estado,
                    color = when {
                        m.voided -> FigmaColors.Alerta
                        pago?.estado == EstadoPagoFiado.PAGADO -> FigmaColors.Verde
                        else -> FigmaColors.Dorado
                    },
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                (if (esFiado) "+$ " else "−$ ") + Money.group(m.amount),
                color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold, textDecoration = tachado,
            )
        }
    }
}
