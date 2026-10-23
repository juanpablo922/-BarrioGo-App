package com.barriogo.app.data.movimientos

import com.barriogo.app.data.AppDatabase
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.data.SessionStore
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.domain.ExpenseKind
import com.barriogo.app.domain.GastoEditable
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import com.barriogo.app.domain.ReglasGasto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Gastos (G1, G2): registrar a mano, ver, editar y anular.
 * - Solo con la caja del día abierta, y solo se editan o anulan los gastos de esa caja
 *   (firestore.rules no deja cambiar movimientos de una caja cerrada).
 * - Anular no borra: el gasto queda tachado en el historial y deja de restar en la caja.
 * Room es lo que leen las pantallas; en una cuenta real cada cambio se copia a Firestore.
 */
class GastosRepository(
    private val room: AppDatabase,
    private val store: SessionStore,
    private val remotos: MovimientosRemotos = MovimientosRemotos(),
) {
    private val dao = room.movementDao()

    val gastos: Flow<List<MovementEntity>> = dao.observeAll().map { lista -> lista.filter { it.type == MovementType.GASTO.name } }

    suspend fun gasto(id: Long): MovementEntity? = dao.byId(id)?.takeIf { it.type == MovementType.GASTO.name }

    suspend fun registrar(tipo: ExpenseKind?, monto: Long, detalle: String, perfil: ProfileEntity): Resultado<MovementEntity> {
        primerProblema(tipo, monto, detalle)?.let { return Resultado.Error(it, "invalid-argument") }
        val caja = room.cashDao().active() ?: return SIN_CAJA
        if (caja.operationDate.isNotBlank() && caja.operationDate != LocalDate.now().toString()) return CAJA_VIEJA
        val nube = enLaNube(perfil)
        val remoteId = if (nube != null && caja.remoteId != null) remotos.nuevoId(nube.first) else null
        val nuevo = MovementEntity(
            type = MovementType.GASTO.name,
            amount = monto,
            title = detalle.trim(),
            detail = tipo!!.detail,
            payMethod = PayMethod.EFECTIVO.name,
            profileId = perfil.id,
            profileName = perfil.name,
            sessionId = caja.id,
            createdAt = System.currentTimeMillis(),
            remoteId = remoteId,
            category = tipo.name,
        )
        val guardado = nuevo.copy(id = dao.insert(nuevo))
        if (nube != null && remoteId != null) remotos.crear(nube.first, remoteId, guardado, caja.remoteId!!, nube.second)
        return Resultado.Ok(guardado)
    }

    suspend fun editar(id: Long, tipo: ExpenseKind?, monto: Long, detalle: String, perfil: ProfileEntity): Resultado<MovementEntity> {
        val g = gastoCambiable(id).let { if (it is Resultado.Error) return it else (it as Resultado.Ok).valor }
        primerProblema(tipo, monto, detalle)?.let { return Resultado.Error(it, "invalid-argument") }
        val original = GastoEditable(ReglasGasto.tipoDe(g.category, g.detail), g.amount, g.title)
        if (!ReglasGasto.hayCambios(original, tipo, monto, detalle)) return Resultado.Error("Todavía no has cambiado nada", "invalid-argument")
        val editado = g.copy(
            amount = monto,
            title = detalle.trim(),
            detail = tipo!!.detail,
            category = tipo.name,
            editedAt = System.currentTimeMillis(),
            originalAmount = g.originalAmount ?: g.amount.takeIf { it != monto },
        )
        dao.update(editado)
        enLaNube(perfil)?.let { remotos.editar(it.first, editado, it.second) }
        return Resultado.Ok(editado)
    }

    suspend fun anular(id: Long, perfil: ProfileEntity): Resultado<MovementEntity> {
        val g = gastoCambiable(id).let { if (it is Resultado.Error) return it else (it as Resultado.Ok).valor }
        val anulado = g.copy(voided = true, voidedAt = System.currentTimeMillis())
        dao.update(anulado)
        val nube = enLaNube(perfil)
        if (nube != null && g.remoteId != null) remotos.anular(nube.first, g.remoteId, nube.second)
        return Resultado.Ok(anulado)
    }

    /** El gasto existe, no está anulado y es de la caja abierta. */
    private suspend fun gastoCambiable(id: Long): Resultado<MovementEntity> {
        val g = gasto(id) ?: return Resultado.Error("No encontramos ese gasto.", "not-found")
        if (g.voided) return Resultado.Error("Este gasto ya está anulado.", "failed-precondition")
        val caja = room.cashDao().active()
        if (caja == null || g.sessionId != caja.id) return Resultado.Error(CAJA_CERRADA, "failed-precondition")
        return Resultado.Ok(g)
    }

    private fun primerProblema(tipo: ExpenseKind?, monto: Long, detalle: String): String? =
        ReglasGasto.problemas(tipo, monto, detalle).values.firstOrNull()

    private fun enLaNube(perfil: ProfileEntity): Pair<String, String>? {
        val tienda = store.tiendaId ?: return null
        val remoto = perfil.remoteId ?: return null
        return if (store.demo) null else tienda to remoto
    }

    companion object {
        const val CAJA_CERRADA = "Este gasto es de una caja cerrada: ya no se puede cambiar."
        private val SIN_CAJA = Resultado.Error("Primero abre la caja del día para registrar gastos.", "failed-precondition")
        private val CAJA_VIEJA = Resultado.Error("La caja de un día anterior sigue abierta. Ciérrala y abre la de hoy.", "failed-precondition")
    }
}
