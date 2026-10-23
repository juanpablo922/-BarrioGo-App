package com.barriogo.app.data.movimientos

import android.util.Log
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.domain.ExpenseKind
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import com.barriogo.app.domain.ReglasGasto
import com.barriogo.app.domain.kind
import com.barriogo.app.domain.pay
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.util.Date

/**
 * Libro de movimientos en Firestore (tiendas/{t}/movimientos), con los campos que exige
 * firestore.rules → movimientoBienFormado(). Se escribe sin await: queda en la caché de Firestore
 * y sube cuando vuelve la señal.
 *
 * Por ahora se sincronizan ventas y gastos. Fiados y abonos necesitan los clientes en Firestore
 * (módulo de fiados, pendiente) y siguen solo en el celular.
 */
class MovimientosRemotos(private val db: FirebaseFirestore = Firebase.firestore) {

    /** Id nuevo para guardarlo en Room antes de escribir. */
    fun nuevoId(tiendaId: String): String = db.collection("tiendas/$tiendaId/movimientos").document().id

    fun sincroniza(tipo: MovementType) = tipo == MovementType.VENTA || tipo == MovementType.GASTO

    fun crear(tiendaId: String, id: String, m: MovementEntity, cajaId: String, perfilId: String) {
        val datos = buildMap<String, Any?> {
            put("cajaId", cajaId)
            put("perfilId", perfilId)
            put("perfilNombre", m.profileName.take(60))
            put("tipo", tipo(m))
            put("monto", m.amount)
            put("efectoCaja", efectoCaja(m).toLong())
            put("descripcion", m.title.trim().take(200))
            put("origen", "manual")
            put("estado", "activo")
            put("registradoEn", Timestamp(Date(m.createdAt)))
            if (m.kind == MovementType.VENTA) put("metodoPago", if (m.pay == PayMethod.TRANSFERENCIA) "transferencia" else "efectivo")
            if (m.kind == MovementType.GASTO) put("categoriaGastoId", categoria(m))
        }
        db.document("tiendas/$tiendaId/movimientos/$id").set(datos)
            .addOnFailureListener { Log.w(TAG, "Firestore rechazó el movimiento $id", it) }
    }

    /** M1 · Editar / G2 · Editar gasto: siempre con actualizadoPor y actualizadoEn (el historial lo arma el servidor). */
    fun editar(tiendaId: String, m: MovementEntity, perfilId: String) {
        val id = m.remoteId ?: return
        val cambios = buildMap<String, Any?> {
            put("monto", m.amount)
            put("descripcion", m.title.trim().take(200))
            put("actualizadoPor", perfilId)
            put("actualizadoEn", Timestamp.now())
            if (m.kind == MovementType.GASTO) put("categoriaGastoId", categoria(m))
        }
        db.document("tiendas/$tiendaId/movimientos/$id").update(cambios)
            .addOnFailureListener { Log.w(TAG, "Firestore rechazó la edición de $id", it) }
    }

    /** M2 / G1 · Anular: no se borra, queda tachado. */
    fun anular(tiendaId: String, remoteId: String, perfilId: String) {
        db.document("tiendas/$tiendaId/movimientos/$remoteId").update(
            mapOf(
                "estado" to "anulado",
                "anuladoPor" to perfilId,
                "anuladoEn" to Timestamp.now(),
                "actualizadoPor" to perfilId,
                "actualizadoEn" to Timestamp.now(),
            ),
        ).addOnFailureListener { Log.w(TAG, "Firestore rechazó la anulación de $remoteId", it) }
    }

    private fun tipo(m: MovementEntity) = when (m.kind) {
        MovementType.VENTA -> "venta"
        MovementType.GASTO -> "gasto"
        MovementType.FIADO -> "fiado"
        MovementType.ABONO -> "abono"
        MovementType.AJUSTE -> "ajuste_caja"
    }

    /** Misma regla que efectoCajaPara() del backend y efectoEsperado() de firestore.rules. */
    private fun efectoCaja(m: MovementEntity): Int = when (m.kind) {
        MovementType.VENTA, MovementType.ABONO -> if (m.pay == PayMethod.TRANSFERENCIA) 0 else 1
        MovementType.GASTO -> -1
        MovementType.FIADO -> 0
        MovementType.AJUSTE -> if (m.amount < 0) -1 else 1
    }

    private fun categoria(m: MovementEntity) =
        ReglasGasto.categoriaId(m.category?.let { runCatching { ExpenseKind.valueOf(it) }.getOrNull() } ?: ReglasGasto.tipoDe(null, m.detail))

    companion object {
        private const val TAG = "MovimientosRemotos"
    }
}
