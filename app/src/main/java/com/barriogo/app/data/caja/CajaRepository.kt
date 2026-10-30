package com.barriogo.app.data.caja

import com.barriogo.app.data.AppDatabase
import com.barriogo.app.data.BarrioRepository
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.data.SessionStore
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.data.movimientos.MovimientosRemotos
import com.barriogo.app.domain.EstadoCaja
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.ReglasCaja
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.Date

/**
 * Caja diaria (K0, K1, a1, c2–c4) leída de Firestore: tiendas/{t}/cajas es la única fuente.
 * Cada cuenta ve solo las cajas de su tienda ([SessionStore.cuenta]); sin internet, Firestore
 * responde con su caché y guarda las escrituras pendientes.
 * - Abrir y corregir la apertura (máx. 3): escritura directa, también sin internet.
 * - Cerrar y corregir el cierre (máx. 2): funciones cerrarCaja / corregirCierre, con internet.
 *
 * Cualquier perfil (administrador o vendedor) abre, corrige y cierra. Sin caja abierta hoy no se
 * registran ventas, gastos ni fiados (ReglasCaja.permiteRegistrar).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CajaRepository(
    private val room: AppDatabase,
    private val store: SessionStore,
    alcance: CoroutineScope,
    private val remota: CajaRemota = CajaRemota(),
    private val remotos: MovimientosRemotos = MovimientosRemotos(),
    /** Se llama después de abrir o cerrar, para reprogramar los avisos de cierre. */
    private val alCambiarCaja: () -> Unit = {},
) {
    private val candado = Mutex()

    /** Últimas cajas de la tienda activa, en tiempo real. */
    private val cajas: Flow<List<Caja>> = store.cuenta
        .flatMapLatest { tienda -> if (tienda.isBlank()) flowOf(emptyList()) else remota.cajas(tienda) }
        .shareIn(alcance, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val abierta: Flow<Caja?> = cajas.map { lista -> lista.firstOrNull { it.isOpen } }.distinctUntilChanged()
    val ultimaCerrada: Flow<Caja?> = cajas.map { lista -> lista.firstOrNull { !it.isOpen } }.distinctUntilChanged()

    /**
     * Movimientos de una caja: gastos, fiados y abonos de Firestore más las ventas de Room.
     * Las ventas también están copiadas en Firestore, pero se cuentan una sola vez, desde Room.
     */
    fun movimientosDe(cajaId: String?): Flow<List<MovementEntity>> {
        val tienda = store.cuenta.value
        if (cajaId == null || tienda.isBlank()) return flowOf(emptyList())
        return combine(remotos.deCaja(tienda, cajaId), room.movementDao().observeByCaja(tienda, cajaId)) { nube, local ->
            (nube.filter { it.type in BarrioRepository.EN_FIRESTORE } + local.filter { it.type !in BarrioRepository.EN_FIRESTORE })
                .sortedByDescending { it.createdAt }
        }
    }

    /** Lectura única: con internet, la de Firestore; sin internet, la de la caché. */
    private suspend fun cajasAhora(): List<Caja> {
        val tienda = store.cuenta.value
        return if (tienda.isBlank()) emptyList() else remota.cajasUnaVez(tienda)
    }

    suspend fun cajaAbiertaUnaVez(): Caja? = cajasAhora().firstOrNull { it.isOpen }

    suspend fun estadoActual(hoy: LocalDate = LocalDate.now()): EstadoCaja {
        val lista = cajasAhora()
        return ReglasCaja.estado(hoy, lista.firstOrNull { it.isOpen }?.let(::fechaDe), lista.firstOrNull { !it.isOpen }?.let(::fechaDe))
    }

    /** K0 · Abrir la caja del día. */
    suspend fun abrir(monto: Long, perfil: ProfileEntity): Resultado<Unit> = candado.withLock {
        ReglasCaja.problemaApertura(monto)?.let { return Resultado.Error(it, "invalid-argument") }
        val nube = enLaNube(perfil) ?: return SIN_NUBE
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
        remota.abrir(nube.first, ReglasCaja.idCaja(hoy), hoy.toString(), monto, nube.second, perfil.name, Date())
        alCambiarCaja()
        Resultado.Ok(Unit)
    }

    /** K1 · Corregir la apertura: máximo 3 veces, cada cambio queda registrado en cambiosApertura/{n}. */
    suspend fun corregirApertura(caja: Caja, nuevo: Long, perfil: ProfileEntity): Resultado<Unit> = candado.withLock {
        if (!caja.isOpen) return Resultado.Error("Esta caja ya no está abierta.", "failed-precondition")
        if (ReglasCaja.cambiosAperturaRestantes(caja.editsUsed) == 0) {
            return Resultado.Error("Ya usaste los 3 cambios de hoy. La apertura no se puede cambiar más.", "resource-exhausted")
        }
        ReglasCaja.problemaApertura(nuevo, actual = caja.openingAmount)?.let { return Resultado.Error(it, "invalid-argument") }
        val nube = enLaNube(perfil) ?: return SIN_NUBE
        remota.corregirApertura(nube.first, caja.id, caja.editsUsed + 1, caja.openingAmount, nuevo, nube.second)
        Resultado.Ok(Unit)
    }

    /** a1 · Arqueo parcial: queda en cajas/{c}/arqueos y no cierra la caja. */
    fun guardarArqueo(caja: Caja, esperado: Long, contado: Long, observaciones: String, perfil: ProfileEntity): Resultado<Unit> {
        ReglasCaja.problemaConteo(contado, observaciones)?.let { return Resultado.Error(it, "invalid-argument") }
        val nube = enLaNube(perfil) ?: return SIN_NUBE
        val arqueo = ReglasCaja.arqueo(esperado, contado)
        remota.guardarArqueo(nube.first, caja.id, esperado, contado, arqueo.resultado, observaciones, nube.second, perfil.name)
        return Resultado.Ok(Unit)
    }

    /** c2–c4 · Cerrar la caja con el efectivo contado. Lo hace el servidor: sin internet no se cierra. */
    suspend fun cerrar(caja: Caja, contado: Long, observaciones: String, perfil: ProfileEntity): Resultado<Unit> = candado.withLock {
        if (!caja.isOpen) return Resultado.Error("Esta caja ya está cerrada.", "failed-precondition")
        ReglasCaja.problemaConteo(contado, observaciones)?.let { return Resultado.Error(it, "invalid-argument") }
        val nube = enLaNube(perfil) ?: return SIN_NUBE
        val r = remota.cerrar(nube.first, caja.id, nube.second, contado, observaciones)
        alCambiarCaja()
        r
    }

    /** c4 · Corregir el cierre: máximo 2 veces y antes de abrir la caja siguiente. */
    suspend fun corregirCierre(caja: Caja, contado: Long, observaciones: String, perfil: ProfileEntity): Resultado<Unit> = candado.withLock {
        when {
            caja.isOpen -> return Resultado.Error("Esta caja todavía no se ha cerrado.", "failed-precondition")
            cajaAbiertaUnaVez() != null -> return Resultado.Error("Ya se abrió otra caja: este cierre no se puede corregir.", "failed-precondition")
            ReglasCaja.cambiosCierreRestantes(caja.closeEditsUsed) == 0 -> return Resultado.Error(
                "Ya corregiste este cierre 2 veces. No se puede cambiar más.", "resource-exhausted",
            )
        }
        ReglasCaja.problemaConteo(contado, observaciones)?.let { return Resultado.Error(it, "invalid-argument") }
        if (!ReglasCaja.hayCambiosEnCierre(caja.countedAmount ?: 0L, caja.closeNote, contado, observaciones)) {
            return Resultado.Error("Todavía no has cambiado nada", "invalid-argument")
        }
        val nube = enLaNube(perfil) ?: return SIN_NUBE
        remota.corregirCierre(nube.first, caja.id, nube.second, contado, observaciones)
    }

    /** Hora de cierre de la tienda. Solo la cambia un administrador. */
    fun guardarHoraCierre(hora: String, perfil: ProfileEntity): Resultado<Unit> {
        if (!perfil.isAdmin) return Resultado.Error("Solo un administrador puede cambiar la hora de cierre.", "permission-denied")
        if (!ReglasCaja.horaValida(hora)) return Resultado.Error("Elige una hora válida.", "invalid-argument")
        store.setHoraCierre(hora)
        store.tiendaId?.let { remota.guardarHoraCierre(it, hora) }
        alCambiarCaja()
        return Resultado.Ok(Unit)
    }

    /** Trae la hora de cierre que tenga la tienda en Firestore (la pudo cambiar otro celular). */
    fun sincronizar() {
        val tienda = store.tiendaId ?: return
        remota.escucharHoraCierre(tienda) { hora ->
            if (hora != store.horaCierre.value) {
                store.setHoraCierre(hora)
                alCambiarCaja()
            }
        }
    }

    fun detener() = remota.detener()

    /** (tiendaId, perfilId en Firestore), o null si el perfil todavía no llegó de Firestore. */
    private fun enLaNube(perfil: ProfileEntity): Pair<String, String>? {
        val tienda = store.tiendaId?.takeIf { it.isNotBlank() } ?: return null
        val remoto = perfil.remoteId ?: return null
        return tienda to remoto
    }

    companion object {
        private val SIN_NUBE = Resultado.Error(
            "Tu perfil todavía no está listo en la nube. Revisa tu internet y vuelve a entrar con tu clave.", "failed-precondition",
        )

        fun fechaDe(c: Caja): LocalDate = runCatching { LocalDate.parse(c.operationDate) }.getOrElse { LocalDate.MIN }
    }
}
