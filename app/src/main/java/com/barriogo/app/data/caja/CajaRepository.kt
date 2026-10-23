package com.barriogo.app.data.caja

import com.barriogo.app.data.AppDatabase
import com.barriogo.app.data.CashChangeEntity
import com.barriogo.app.data.CashSessionEntity
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.data.SessionStore
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.EstadoCaja
import com.barriogo.app.domain.ReglasCaja
import com.barriogo.app.domain.TotalesCaja
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.Date

/**
 * Caja diaria (K0, K1, c2–c4). Room es lo que leen las pantallas y funciona sin internet; en una
 * cuenta real cada paso se copia a Firestore con las reglas del backend:
 * - Abrir y corregir la apertura (máx. 3): escritura directa, sin internet.
 * - Cerrar y corregir el cierre (máx. 2): funciones cerrarCaja / corregirCierre, con internet.
 *
 * Cualquier perfil (administrador o vendedor) abre, corrige y cierra. Sin caja abierta hoy no se
 * registran ventas, gastos ni fiados (ReglasCaja.permiteRegistrar).
 */
class CajaRepository(
    private val room: AppDatabase,
    private val store: SessionStore,
    private val remota: CajaRemota = CajaRemota(),
    /** Se llama después de abrir o cerrar, para reprogramar los avisos de cierre. */
    private val alCambiarCaja: () -> Unit = {},
) {
    private val dao = room.cashDao()
    private val candado = Mutex()

    val abierta: Flow<CashSessionEntity?> = dao.observeActive()
    val ultimaCerrada: Flow<CashSessionEntity?> = dao.observeLastClosed()
    val todas: Flow<List<CashSessionEntity>> = dao.observeAll()

    suspend fun estadoActual(hoy: LocalDate = LocalDate.now()): EstadoCaja =
        ReglasCaja.estado(hoy, dao.active()?.let(::fechaDe), dao.lastClosed()?.let(::fechaDe))

    suspend fun totales(sesion: CashSessionEntity): TotalesCaja =
        ReglasCaja.totales(sesion.openingAmount, room.movementDao().bySession(sesion.id))

    /** K0 · Abrir la caja del día. */
    suspend fun abrir(monto: Long, perfil: ProfileEntity): Resultado<CashSessionEntity> = candado.withLock {
        ReglasCaja.problemaApertura(monto)?.let { return Resultado.Error(it, "invalid-argument") }
        val hoy = LocalDate.now()
        when (estadoActual(hoy)) {
            EstadoCaja.ABIERTA -> return Resultado.Error("La caja de hoy ya está abierta.", "already-exists")
            EstadoCaja.ABIERTA_DIA_ANTERIOR -> return Resultado.Error(
                "La caja de un día anterior sigue abierta. Ciérrala antes de abrir la de hoy.", "failed-precondition",
            )
            EstadoCaja.CERRADA_HOY -> return Resultado.Error(
                "La caja de hoy ya se cerró. Mañana podrás abrir una nueva.", "failed-precondition",
            )
            EstadoCaja.SIN_ABRIR -> Unit
        }
        val ahora = System.currentTimeMillis()
        val nube = enLaNube(perfil)
        val sesion = CashSessionEntity(
            openedAt = ahora,
            openingAmount = monto,
            openedBy = perfil.name,
            operationDate = hoy.toString(),
            remoteId = if (nube != null) ReglasCaja.idCaja(hoy) else null,
        )
        val guardada = sesion.copy(id = dao.insert(sesion))
        if (nube != null) {
            remota.abrir(nube.first, guardada.remoteId!!, hoy.toString(), monto, nube.second, perfil.name, Date(ahora))
        }
        alCambiarCaja()
        Resultado.Ok(guardada)
    }

    /** K1 · Corregir la apertura: máximo 3 veces, cada cambio queda registrado. */
    suspend fun corregirApertura(sesionId: Long, nuevo: Long, perfil: ProfileEntity): Resultado<CashSessionEntity> = candado.withLock {
        val s = dao.byId(sesionId)?.takeIf { it.isOpen } ?: return Resultado.Error("Esta caja ya no está abierta.", "failed-precondition")
        if (ReglasCaja.cambiosAperturaRestantes(s.editsUsed) == 0) {
            return Resultado.Error("Ya usaste los 3 cambios de hoy. La apertura no se puede cambiar más.", "resource-exhausted")
        }
        ReglasCaja.problemaApertura(nuevo, actual = s.openingAmount)?.let { return Resultado.Error(it, "invalid-argument") }
        val numero = s.editsUsed + 1
        val nueva = s.copy(openingAmount = nuevo, editsUsed = numero)
        dao.update(nueva)
        dao.insertChange(
            CashChangeEntity(
                sessionId = s.id, kind = APERTURA, number = numero, before = s.openingAmount, after = nuevo,
                by = perfil.name, at = System.currentTimeMillis(),
            ),
        )
        val nube = enLaNube(perfil)
        if (nube != null && s.remoteId != null) remota.corregirApertura(nube.first, s.remoteId, numero, s.openingAmount, nuevo, nube.second)
        Resultado.Ok(nueva)
    }

    /**
     * c2–c4 · Cerrar la caja con el efectivo contado. En una cuenta real lo confirma primero el servidor:
     * sin internet no se cierra (el comprobante sale de todos los movimientos sincronizados).
     */
    suspend fun cerrar(sesionId: Long, contado: Long, observaciones: String, perfil: ProfileEntity): Resultado<CashSessionEntity> =
        candado.withLock {
            val s = dao.byId(sesionId)?.takeIf { it.isOpen } ?: return Resultado.Error("Esta caja ya está cerrada.", "failed-precondition")
            ReglasCaja.problemaConteo(contado, observaciones)?.let { return Resultado.Error(it, "invalid-argument") }
            val nube = enLaNube(perfil)
            if (nube != null && s.remoteId != null) {
                val r = remota.cerrar(nube.first, s.remoteId, nube.second, contado, observaciones)
                // "Ya está cerrada": la cerró otro celular; aquí también se cierra.
                if (r is Resultado.Error && r.codigo != "failed-precondition") return r
            }
            val esperado = totales(s).saldoEsperado
            val cerrada = s.copy(
                closedAt = System.currentTimeMillis(), countedAmount = contado, expectedAtClose = esperado,
                closedBy = perfil.name, closeNote = observaciones.trim(),
            )
            dao.update(cerrada)
            alCambiarCaja()
            Resultado.Ok(cerrada)
        }

    /** c4 · Corregir el cierre: máximo 2 veces y antes de abrir la caja siguiente. */
    suspend fun corregirCierre(sesionId: Long, contado: Long, observaciones: String, perfil: ProfileEntity): Resultado<CashSessionEntity> =
        candado.withLock {
            val s = dao.byId(sesionId) ?: return Resultado.Error("No encontramos esa caja.", "not-found")
            when {
                s.isOpen -> return Resultado.Error("Esta caja todavía no se ha cerrado.", "failed-precondition")
                dao.active() != null -> return Resultado.Error("Ya se abrió otra caja: este cierre no se puede corregir.", "failed-precondition")
                ReglasCaja.cambiosCierreRestantes(s.closeEditsUsed) == 0 -> return Resultado.Error(
                    "Ya corregiste este cierre 2 veces. No se puede cambiar más.", "resource-exhausted",
                )
            }
            ReglasCaja.problemaConteo(contado, observaciones)?.let { return Resultado.Error(it, "invalid-argument") }
            val contadoAntes = s.countedAmount ?: 0L
            if (!ReglasCaja.hayCambiosEnCierre(contadoAntes, s.closeNote, contado, observaciones)) {
                return Resultado.Error("Todavía no has cambiado nada", "invalid-argument")
            }
            val nube = enLaNube(perfil)
            if (nube != null && s.remoteId != null) {
                val r = remota.corregirCierre(nube.first, s.remoteId, nube.second, contado, observaciones)
                if (r is Resultado.Error) return r
            }
            val numero = s.closeEditsUsed + 1
            val corregida = s.copy(countedAmount = contado, closeNote = observaciones.trim(), closeEditsUsed = numero)
            dao.update(corregida)
            dao.insertChange(
                CashChangeEntity(
                    sessionId = s.id, kind = CIERRE, number = numero, before = contadoAntes, after = contado,
                    note = observaciones.trim(), by = perfil.name, at = System.currentTimeMillis(),
                ),
            )
            Resultado.Ok(corregida)
        }

    /** Hora de cierre de la tienda. Solo la cambia un administrador. */
    fun guardarHoraCierre(hora: String, perfil: ProfileEntity): Resultado<Unit> {
        if (!perfil.isAdmin) return Resultado.Error("Solo un administrador puede cambiar la hora de cierre.", "permission-denied")
        if (!ReglasCaja.horaValida(hora)) return Resultado.Error("Elige una hora válida.", "invalid-argument")
        store.setHoraCierre(hora)
        val tienda = store.tiendaId
        if (!store.demo && tienda != null) remota.guardarHoraCierre(tienda, hora)
        alCambiarCaja()
        return Resultado.Ok(Unit)
    }

    /** Trae la hora de cierre que tenga la tienda en Firestore (la pudo cambiar otro celular). */
    fun sincronizar() {
        val tienda = store.tiendaId ?: return
        if (store.demo) return
        remota.escucharHoraCierre(tienda) { hora ->
            if (hora != store.horaCierre.value) {
                store.setHoraCierre(hora)
                alCambiarCaja()
            }
        }
    }

    fun detener() = remota.detener()

    /** (tiendaId, perfilId en Firestore) si esta acción se copia a Firestore; null en la demo. */
    private fun enLaNube(perfil: ProfileEntity): Pair<String, String>? {
        val tienda = store.tiendaId ?: return null
        val remoto = perfil.remoteId ?: return null
        return if (store.demo) null else tienda to remoto
    }

    companion object {
        const val APERTURA = "APERTURA"
        const val CIERRE = "CIERRE"

        fun fechaDe(s: CashSessionEntity): LocalDate =
            s.operationDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse) ?: Dates.localDate(s.openedAt)
    }
}
