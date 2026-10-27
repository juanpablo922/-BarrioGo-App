package com.barriogo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.domain.ExpenseKind
import com.barriogo.app.domain.Fiados
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.ReglasGasto
import com.barriogo.app.domain.kind
import com.barriogo.app.domain.label
import com.barriogo.app.ui.theme.FigmaColors
import kotlin.math.abs

/** Ícono, color y signo de un movimiento según el Figma "Movimientos". */
private data class EstiloMovimiento(val icono: ImageVector, val color: Color, val signo: String)

@Composable
private fun estilo(m: MovementEntity): EstiloMovimiento = when (m.kind) {
    // El fiado no mueve la caja: va en amarillo y sin signo
    MovementType.FIADO -> EstiloMovimiento(Icons.Filled.Person, FigmaColors.Amarillo, "")
    MovementType.GASTO -> EstiloMovimiento(Icons.AutoMirrored.Filled.TrendingDown, FigmaColors.Naranja, "−")
    MovementType.AJUSTE ->
        if (m.amount < 0) EstiloMovimiento(Icons.AutoMirrored.Filled.TrendingDown, FigmaColors.Naranja, "−")
        else EstiloMovimiento(Icons.AutoMirrored.Filled.TrendingUp, FigmaColors.Verde, "+")
    MovementType.VENTA, MovementType.ABONO -> EstiloMovimiento(Icons.AutoMirrored.Filled.TrendingUp, FigmaColors.Verde, "+")
}

/** "Venta", "Gasto proveedor", "Gasto", "Fiado", "Abono", "Caja": lo que va después de la hora. */
fun categoriaMovimiento(m: MovementEntity): String = when (m.kind) {
    MovementType.VENTA -> "Venta"
    MovementType.GASTO -> ReglasGasto.tipoDe(m.category, m.detail).let { if (it == ExpenseKind.OTROS) "Gasto" else it.detail }
    MovementType.FIADO -> "Fiado"
    MovementType.ABONO -> "Abono"
    MovementType.AJUSTE -> "Caja"
}

/**
 * Fila del Figma "Movimientos" (también en Inicio): círculo #22593F con ↗ verde (venta, abono),
 * ↘ naranja (gasto) o la persona amarilla (fiado); título crema, "8:15 · Venta" dorado y el monto
 * del mismo color del ícono. Un movimiento eliminado se ve tachado y al 50 %.
 */
@Composable
fun FilaMovimientoFigma(
    m: MovementEntity,
    divisor: Boolean,
    modifier: Modifier = Modifier,
    ocultarMonto: Boolean = false,
    /** "· Eliminado 10:40", "· Editado (antes $ 5.000)". */
    nota: String = "",
    onClick: (() -> Unit)? = null,
    /** Algo al final de la fila, después del monto (el lápiz de Movimientos). */
    accion: (@Composable () -> Unit)? = null,
) {
    val e = estilo(m)
    val tachado = if (m.voided) TextDecoration.LineThrough else TextDecoration.None
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(70.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .alpha(if (m.voided) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(FigmaColors.Tarjeta), contentAlignment = Alignment.Center) {
                Icon(e.icono, null, tint = e.color, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    m.label(), color = FigmaColors.Crema, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textDecoration = tachado,
                )
                Text(
                    "${Fiados.hora(m.createdAt)} · ${categoriaMovimiento(m)}$nota",
                    color = if (m.voided) FigmaColors.Alerta else FigmaColors.Dorado,
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                if (ocultarMonto) Money.HIDDEN else e.signo + Money.cop(abs(m.amount)),
                color = e.color, fontSize = 15.sp, fontWeight = FontWeight.Bold, textDecoration = tachado,
            )
            accion?.invoke()
        }
        if (divisor) Box(Modifier.fillMaxWidth().height(1.dp).background(FigmaColors.Tarjeta))
    }
}
