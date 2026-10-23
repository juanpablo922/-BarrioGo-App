package com.barriogo.app.data.movimientos

import com.barriogo.app.data.MovementEntity
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.data.SessionStore
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.data.caja.CajaRepository
import com.barriogo.app.domain.ExpenseKind
import com.barriogo.app.domain.GastoEditable
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import com.barriogo.app.domain.ReglasGasto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import java.time.LocalDate

/**
 * Gastos (G1, G2) en Firestore (tiendas/{t}/movimientos con tipo "gasto"): se leen y se escriben
 * solo allí, en tiempo real y con la caché de Firestore cuando no hay internet.
 * - Solo con la caja del día abierta, y solo se editan o anulan los gastos de esa caja
 *   (firestore.rules no deja cambiar movimientos de una caja cerrada).
 * - Anular no borra: el gasto queda tachado en el historial y deja de restar en la caja.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GastosRepository(
    private val store: SessionStore,
    private val cajas: CajaRepository,
    alcance: CoroutineScope,
    private val remotos: MovimientosRemotos = MovimientosRemotos(),
) {
    /** Gastos de la tienda activa, del más nuevo al más viejo. */
    val gastos: Flow<List<MovementEntity>> = store.cuenta
        .flatMapLatest { tienda -> if (tienda.isBlank()) flowOf(emptyList()) else remotos.gastos(tienda) }
        .shareIn(alcance, SharingStarted.WhileSubscribed(5_000), replay = 1)

    suspend fun gasto(remoteId: String): MovementEntity? = gastos.first().firstOrNull { it.remoteId == remoteId }

    /** G2 · Registrar a mano (también lo usan la voz y el escáner). */
    suspend fun registrar(tipo: ExpenseKind?, monto: Long, detalle: String, perfil: ProfileEntity): Resultado<MovementEntity> {
        primerProblema(tipo, monto, detalle)?.let { return Resultado.Error(it, "invalid-argument") }
        val caja = cajas.cajaAbiertaUnaVez() ?: return SIN_CAJA
        if (CajaRepository.fechaDe(caja) != LocalDate.now()) return CAJA_VIEJA
        val nube = enLaNube(perfil) ?: return SIN_NUBE
        val remoteId = remotos.nuevoId(nube.first)
        val nuevo = MovementEntity(
            id = MovimientosRemotos.idLocal(remoteId),
            type = MovementType.GASTO.name,
            amount = monto,
            title = detalle.trim(),
            detail = tipo!!.detail,
            payMethod = PayMethod.EFECTIVO.name,
            profileId = perfil.id,
            profileName = perfil.name,
            createdAt = System.currentTimeMillis(),
            remoteId = remoteId,
            category = tipo.name,
            cuenta = nube.first,
            cajaId = caja.id,
        )
        remotos.crear(nube.first, remoteId, nuevo, caja.id, nube.second)
        return Resultado.Ok(nuevo)
    }

    suspend fun editar(remoteId: String, tipo: ExpenseKind?, monto: Long, detalle: String, perfil: ProfileEntity): Resultado<MovementEntity> {
        val g = gastoCambiable(remoteId).let { if (it is Resultado.Error) return it else (it as Resultado.Ok).valor }
        primerProblema(tipo, monto, detalle)?.let { return Resultado.Error(it, "invalid-argument") }
        val original = GastoEditable(ReglasGasto.tipoDe(g.category, g.detail), g.amount, g.title)
        if (!ReglasGasto.hayCambios(original, tipo, monto, detalle)) return Resultado.Error("Todavía no has cambiado nada", "invalid-argument")
        val nube = enLaNube(perfil) ?: return SIN_NUBE
        val editado = g.copy(amount = monto, title = detalle.trim(), detail = tipo!!.detail, category = tipo.name)
        remotos.editar(nube.first, editado, nube.second)
        return Resultado.Ok(editado)
    }

    suspend fun anular(remoteId: String, perfil: ProfileEntity): Resultado<Unit> {
        gastoCambiable(remoteId).let { if (it is Resultado.Error) return it }
        val nube = enLaNube(perfil) ?: return SIN_NUBE
        remotos.anular(nube.first, remoteId, nube.second)
        return Resultado.Ok(Unit)
    }

    /** El gasto existe, no está anulado y es de la caja abierta. */
    private suspend fun gastoCambiable(remoteId: String): Resultado<MovementEntity> {
        val g = gasto(remoteId) ?: return Resultado.Error("No encontramos ese gasto.", "not-found")
        if (g.voided) return Resultado.Error("Este gasto ya está anulado.", "failed-precondition")
        val caja = cajas.cajaAbiertaUnaVez()
        if (caja == null || g.cajaId != caja.id) return Resultado.Error(CAJA_CERRADA, "failed-precondition")
        return Resultado.Ok(g)
    }

    private fun primerProblema(tipo: ExpenseKind?, monto: Long, detalle: String): String? =
        ReglasGasto.problemas(tipo, monto, detalle).values.firstOrNull()

    private fun enLaNube(perfil: ProfileEntity): Pair<String, String>? {
        val tienda = store.tiendaId?.takeIf { it.isNotBlank() } ?: return null
        val remoto = perfil.remoteId ?: return null
        return tienda to remoto
    }

    companion object {
        const val CAJA_CERRADA = "Este gasto es de una caja cerrada: ya no se puede cambiar."
        private val SIN_CAJA = Resultado.Error("Primero abre la caja del día para registrar gastos.", "failed-precondition")
        private val CAJA_VIEJA = Resultado.Error("La caja de un día anterior sigue abierta. Ciérrala y abre la de hoy.", "failed-precondition")
        private val SIN_NUBE = Resultado.Error(
            "Tu perfil todavía no está listo en la nube. Revisa tu internet y vuelve a entrar con tu clave.", "failed-precondition",
        )
    }
}
