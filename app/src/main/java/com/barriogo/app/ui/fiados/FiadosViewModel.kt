package com.barriogo.app.ui.fiados

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.AppContainer
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.data.fiados.FiadosRepository
import com.barriogo.app.domain.FiadoPendiente
import com.barriogo.app.domain.Fiados
import com.barriogo.app.domain.PayMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Fiados (F1, ficha del cliente, Nuevo fiado, Registrar pago). Todo se lee y se escribe en
 * Firestore por medio de [FiadosRepository]: nada vive en Room.
 * - [error]: lo que no se pudo guardar. [avisoCupo]: el cliente pasaría su límite de fiado.
 */
class FiadosViewModel(private val container: AppContainer) : ViewModel() {
    private val fiados = container.fiados

    /** Estado de cuenta de todos los clientes (también los que no deben). null mientras carga. */
    val cuentas: StateFlow<List<FiadoPendiente>?> = combine(fiados.clientes, fiados.movimientos) { clientes, movimientos ->
        Fiados.cuentas(clientes, movimientos)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** F1 · Fiados: clientes que deben, con la fecha de la deuda y cuántos fiados lleva. null mientras carga. */
    val pendientes: StateFlow<List<FiadoPendiente>?> = combine(fiados.clientes, fiados.movimientos) { clientes, movimientos ->
        Fiados.pendientes(clientes, movimientos)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Fiados y abonos de la tienda (también los anulados), para la ficha del cliente. */
    val movimientos: StateFlow<List<MovementEntity>> =
        fiados.movimientos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** El ojo de Inicio también oculta los montos aquí. */
    val ocultarMontos: StateFlow<Boolean> = container.store.hideAmounts

    /** Solo un administrador cambia el cupo de un cliente. */
    val esAdmin: StateFlow<Boolean> = container.activeProfileId
        .map { id -> id?.let { container.repo.profile(it) }?.isAdmin == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    fun cerrarError() { _error.value = null }

    private val _avisoCupo = MutableStateFlow<String?>(null)
    val avisoCupo: StateFlow<String?> = _avisoCupo
    fun cerrarAvisoCupo() { _avisoCupo.value = null }
    fun avisarCupo(mensaje: String) { _avisoCupo.value = mensaje }

    // ── Clientes ───────────────────────────────────────────────────────────────────────────────

    /** "+ Agregar cliente": [onListo] recibe el id del cliente (el nuevo o el que ya tenía ese celular). */
    fun agregarCliente(nombre: String, celular: String, direccion: String, onListo: (Long) -> Unit) {
        viewModelScope.launch { manejar(fiados.crearCliente(nombre, celular, direccion)) { onListo(it.id) } }
    }

    fun editarCliente(id: Long, nombre: String, celular: String, direccion: String, onListo: () -> Unit) {
        viewModelScope.launch { manejar(fiados.editarCliente(id, nombre, celular, direccion)) { onListo() } }
    }

    fun cambiarCupo(id: Long, cupo: Long, onListo: () -> Unit) {
        viewModelScope.launch {
            val perfil = perfil() ?: return@launch
            manejar(fiados.cambiarCupo(id, cupo, perfil)) { onListo() }
        }
    }

    // ── Fiados y abonos ────────────────────────────────────────────────────────────────────────

    /** Guarda el fiado en la caja abierta. Si pasa el cupo queda en [avisoCupo] y no se llama [onListo]. */
    fun guardarFiado(clientId: Long, queSeLlevo: String, monto: Long, onListo: () -> Unit) {
        viewModelScope.launch {
            val perfil = perfil() ?: return@launch
            manejar(fiados.registrarFiado(clientId, queSeLlevo, monto, perfil)) { onListo() }
        }
    }

    /** "Sí, me pagó": abono en efectivo por todo lo que debe; [onListo] recibe cuánto entró a la caja. */
    fun marcarPagado(clientId: Long, onListo: (Long) -> Unit) {
        viewModelScope.launch {
            val saldo = fiados.cuenta(clientId)?.saldo ?: 0L
            if (saldo <= 0) return@launch
            val perfil = perfil() ?: return@launch
            manejar(fiados.registrarAbono(clientId, saldo, PayMethod.EFECTIVO, perfil)) { onListo(saldo) }
        }
    }

    /**
     * Registrar pago: el abono en efectivo entra a la caja; por transferencia solo baja la deuda.
     * Con [fiadoId] (id en Firestore) el abono salda solo ese fiado.
     */
    fun guardarPago(clientId: Long, monto: Long, metodo: PayMethod, fiadoId: String? = null, onListo: () -> Unit) {
        viewModelScope.launch {
            val perfil = perfil() ?: return@launch
            manejar(fiados.registrarAbono(clientId, monto, metodo, perfil, fiadoId)) { onListo() }
        }
    }

    /** Editar fiado: valor y "¿Qué se llevó?". */
    fun editarFiado(remoteId: String, queSeLlevo: String, monto: Long, onListo: () -> Unit) {
        viewModelScope.launch {
            val perfil = perfil() ?: return@launch
            manejar(fiados.editarFiado(remoteId, queSeLlevo, monto, perfil)) { onListo() }
        }
    }

    /** Editar abono: valor y método de pago. */
    fun editarAbono(remoteId: String, monto: Long, metodo: PayMethod, onListo: () -> Unit) {
        viewModelScope.launch {
            val perfil = perfil() ?: return@launch
            manejar(fiados.editarAbono(remoteId, monto, metodo, perfil)) { onListo() }
        }
    }

    /** Anular un fiado o abono registrado por error: queda tachado, como en Movimientos. */
    fun anular(remoteId: String, onListo: () -> Unit) {
        viewModelScope.launch {
            val perfil = perfil() ?: return@launch
            manejar(fiados.anular(remoteId, perfil)) { onListo() }
        }
    }

    private suspend fun perfil(): ProfileEntity? =
        container.perfilActivo() ?: run { _error.value = "Vuelve a entrar con tu clave para continuar."; null }

    private inline fun <T> manejar(r: Resultado<T>, onOk: (T) -> Unit) {
        when (r) {
            is Resultado.Ok -> onOk(r.valor)
            is Resultado.Error -> if (r.codigo == FiadosRepository.CUPO) _avisoCupo.value = r.mensaje else _error.value = r.mensaje
        }
    }
}
