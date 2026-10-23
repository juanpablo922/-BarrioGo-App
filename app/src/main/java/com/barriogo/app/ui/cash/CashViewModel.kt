package com.barriogo.app.ui.cash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.AppContainer
import com.barriogo.app.data.ArqueoEntity
import com.barriogo.app.data.CashSessionEntity
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.data.caja.CajaRepository
import com.barriogo.app.domain.AvisoCierre
import com.barriogo.app.domain.EstadoCaja
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.ReglasCaja
import com.barriogo.app.domain.TotalesCaja
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

data class CashUiState(
    val cargado: Boolean = false,
    val hoy: LocalDate = LocalDate.now(),
    val estado: EstadoCaja = EstadoCaja.SIN_ABRIR,
    /** Caja abierta (de hoy o de un día anterior que no se cerró). */
    val session: CashSessionEntity? = null,
    val ultimaCerrada: CashSessionEntity? = null,
    /** Movimientos de la caja abierta, del más nuevo al más viejo (sin los anulados). */
    val movimientosCaja: List<MovementEntity> = emptyList(),
    val totales: TotalesCaja? = null,
    /** Totales de la última caja cerrada, para el comprobante (c4). */
    val totalesCierre: TotalesCaja? = null,
    val fiadoTotal: Long = 0,
    /** Clientes con saldo pendiente (píldora "4 clientes"). */
    val fiadoClientes: Int = 0,
    val perfil: ProfileEntity? = null,
    val horaCierre: String = ReglasCaja.HORA_CIERRE_POR_DEFECTO,
    val aviso: AvisoCierre = AvisoCierre.Ninguno,
) {
    val saldo: Long get() = totales?.saldoEsperado ?: 0L
    val entradas: Long get() = totales?.entradas ?: 0L
    val salidas: Long get() = totales?.salidas ?: 0L
    val editsLeft: Int get() = ReglasCaja.cambiosAperturaRestantes(session?.editsUsed ?: 0)
    val cambiosCierreRestantes: Int get() = ReglasCaja.cambiosCierreRestantes(ultimaCerrada?.closeEditsUsed ?: 0)
    /** El cierre se corrige solo antes de abrir la caja siguiente. */
    val puedeCorregirCierre: Boolean get() = session == null && ultimaCerrada != null && cambiosCierreRestantes > 0
    val esAdmin: Boolean get() = perfil?.isAdmin == true
    val textoAviso: String? get() = ReglasCaja.textoAviso(aviso, ReglasCaja.hora(horaCierre))
}

/**
 * Caja diaria: C1 (Caja), K0/K1 (apertura), c2–c4 (cierre y su corrección), a1 (arqueo) y la hora de cierre.
 * [ocupado] es el texto del "Cargando…" mientras se guarda; [error] el aviso para mostrar.
 */
class CashViewModel(private val container: AppContainer) : ViewModel() {
    private val caja = container.caja

    /** Reloj de la pantalla: cambia el día y los avisos de cierre sin tener que salir y entrar. */
    private val reloj = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(30_000)
        }
    }

    private val perfil = MutableStateFlow<ProfileEntity?>(null)

    val state: StateFlow<CashUiState> = combine(
        combine(caja.abierta, caja.ultimaCerrada, container.repo.movements, ::Triple),
        perfil,
        container.store.horaCierre,
        reloj,
    ) { (abierta, cerrada, movimientos), perfil, hora, ahora ->
        val hoy = ahora.toLocalDate()
        val deLaCaja = abierta?.let { s -> movimientos.filter { it.sessionId == s.id } }.orEmpty()
        val deudas = movimientos.filter { !it.voided && it.clientId != null }
            .groupBy { it.clientId }
            .mapValues { (_, ms) -> ms.sumOf { m -> if (m.type == MovementType.FIADO.name) m.amount else if (m.type == MovementType.ABONO.name) -m.amount else 0L } }
            .values.filter { it > 0 }
        CashUiState(
            cargado = true,
            hoy = hoy,
            estado = ReglasCaja.estado(hoy, abierta?.let(CajaRepository::fechaDe), cerrada?.let(CajaRepository::fechaDe)),
            session = abierta,
            ultimaCerrada = cerrada,
            movimientosCaja = deLaCaja.filter { !it.voided },
            totales = abierta?.let { ReglasCaja.totales(it.openingAmount, deLaCaja) },
            totalesCierre = cerrada?.let { c -> ReglasCaja.totales(c.openingAmount, movimientos.filter { it.sessionId == c.id }) },
            fiadoTotal = deudas.sum(),
            fiadoClientes = deudas.size,
            perfil = perfil,
            horaCierre = hora,
            aviso = ReglasCaja.aviso(ahora, ReglasCaja.hora(hora), abierta?.let(CajaRepository::fechaDe)),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CashUiState())

    private val _ocupado = MutableStateFlow<String?>(null)
    val ocupado: StateFlow<String?> = _ocupado

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    fun cerrarError() { _error.value = null }

    init {
        viewModelScope.launch {
            container.activeProfileId.collect { id -> perfil.value = id?.let { container.repo.profile(it) } }
        }
    }

    fun abrir(monto: Long, onOk: (CashSessionEntity) -> Unit) = lanzar("Guardando la apertura…", onOk) { p -> caja.abrir(monto, p) }

    fun corregirApertura(monto: Long, onOk: (CashSessionEntity) -> Unit) {
        val s = state.value.session ?: return run { _error.value = "No hay una caja abierta." }
        lanzar("Guardando la apertura…", onOk) { p -> caja.corregirApertura(s.id, monto, p) }
    }

    fun cerrar(contado: Long, observaciones: String, onOk: (CashSessionEntity) -> Unit) {
        val s = state.value.session ?: return run { _error.value = "No hay una caja abierta." }
        lanzar("Cerrando la caja…", onOk) { p -> caja.cerrar(s.id, contado, observaciones, p) }
    }

    fun corregirCierre(contado: Long, observaciones: String, onOk: (CashSessionEntity) -> Unit) {
        val s = state.value.ultimaCerrada ?: return run { _error.value = "No hay un cierre para corregir." }
        lanzar("Guardando la corrección…", onOk) { p -> caja.corregirCierre(s.id, contado, observaciones, p) }
    }

    fun guardarHoraCierre(hora: String, onOk: () -> Unit) {
        val p = perfil.value ?: return run { _error.value = SIN_PERFIL }
        when (val r = caja.guardarHoraCierre(hora, p)) {
            is Resultado.Ok -> onOk()
            is Resultado.Error -> _error.value = r.mensaje
        }
    }

    /** a1 · Arqueo parcial (no cierra la caja). */
    fun saveArqueo(counted: Long, note: String, onDone: () -> Unit) {
        val s = state.value.session ?: return
        lanzar("Guardando el arqueo…", { _: Unit -> onDone() }) { p ->
            container.repo.saveArqueo(
                ArqueoEntity(sessionId = s.id, at = System.currentTimeMillis(), expected = state.value.saldo, counted = counted, note = note.trim(), by = p.name),
            )
            Resultado.Ok(Unit)
        }
    }

    private fun <T> lanzar(mensaje: String, onOk: (T) -> Unit, accion: suspend (ProfileEntity) -> Resultado<T>) {
        if (_ocupado.value != null) return
        viewModelScope.launch {
            val p = perfil.value ?: container.perfilActivo()
            if (p == null) {
                _error.value = SIN_PERFIL
                return@launch
            }
            _ocupado.value = mensaje
            val inicio = System.currentTimeMillis()
            val r = try {
                accion(p)
            } finally {
                // Se deja ver el "Cargando…" al menos medio segundo para que no parpadee.
                delay((MINIMO_CARGANDO - (System.currentTimeMillis() - inicio)).coerceAtLeast(0))
                _ocupado.value = null
            }
            when (r) {
                is Resultado.Ok -> onOk(r.valor)
                is Resultado.Error -> _error.value = r.mensaje
            }
        }
    }

    companion object {
        private const val MINIMO_CARGANDO = 500L
        private const val SIN_PERFIL = "Vuelve a entrar con tu clave para continuar."
    }
}
