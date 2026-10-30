package com.barriogo.app.ui.fiados

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.domain.Fiados
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.kind
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AvisoDialogo

/** true si el movimiento es un fiado o un abono de Firestore: el libro lo abre con [DialogoFiadoDesdeLibro]. */
fun esDeFiados(m: MovementEntity): Boolean =
    m.remoteId != null && m.clientId != null && (m.kind == MovementType.FIADO || m.kind == MovementType.ABONO)

/**
 * El mismo diálogo de la ficha del cliente ("Este fiado" / "Editar abono"), abierto desde el libro
 * de Movimientos: abonar solo a ese fiado, corregir o anular. Maneja sus confirmaciones y errores.
 */
@Composable
fun DialogoFiadoDesdeLibro(
    m: MovementEntity,
    onCerrar: () -> Unit,
    /** Abonar solo a este fiado: (clienteId, id del fiado en Firestore). */
    onAbonarFiado: (Long, String) -> Unit,
) {
    val vm = barrioViewModel { FiadosViewModel(it) }
    val todos by vm.movimientos.collectAsState()
    val error by vm.error.collectAsState()
    val avisoCupo by vm.avisoCupo.collectAsState()
    var anulando by rememberSaveable { mutableStateOf(false) }
    val clienteId = m.clientId ?: return
    val remoteId = m.remoteId ?: return
    val pago = Fiados.pagosPorFiado(clienteId, todos)[m.id]

    if (!anulando) {
        DialogoMovimientoFiado(
            m = m,
            pago = pago,
            onCancelar = onCerrar,
            onAbonar = { onCerrar(); onAbonarFiado(clienteId, remoteId) },
            onGuardarFiado = { que, monto -> vm.editarFiado(remoteId, que, monto) { onCerrar() } },
            onGuardarAbono = { monto, metodo -> vm.editarAbono(remoteId, monto, metodo) { onCerrar() } },
            onAnular = { anulando = true },
        )
    } else {
        val esFiado = m.kind == MovementType.FIADO
        AvisoDialogo(
            titulo = if (esFiado) "¿Anular este fiado?" else "¿Anular este abono?",
            mensaje = "${Fiados.titulo(m)} por ${Money.cop(m.amount)} queda anulado: se ve tachado en Movimientos " +
                "y en el historial del cliente, y ya no cuenta en lo que te debe. Úsalo solo si se registró por error.",
            principal = "Sí, anular",
            onPrincipal = { vm.anular(remoteId) { onCerrar() }; anulando = false },
            secundario = "Conservar",
            onSecundario = { anulando = false },
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
