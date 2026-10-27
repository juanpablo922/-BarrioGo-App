package com.barriogo.app.ui.fiados

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.AppContainer
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.domain.FiadoPendiente
import com.barriogo.app.domain.Fiados
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer

class FiadosViewModel(private val container: AppContainer) : ViewModel() {
    /** Estado de cuenta de todos los clientes (también los que no deben). null mientras carga. */
    val cuentas: StateFlow<List<FiadoPendiente>?> = combine(container.repo.clients, container.repo.movements) { clients, movements ->
        Fiados.cuentas(clients, movements)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** F1 · Fiados: clientes que deben, con la fecha de la deuda y cuántos fiados lleva. null mientras carga. */
    val pendientes: StateFlow<List<FiadoPendiente>?> = combine(container.repo.clients, container.repo.movements) { clients, movements ->
        Fiados.pendientes(clients, movements)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Libro completo, para los "Últimos movimientos" de la ficha del cliente. */
    val movimientos: StateFlow<List<MovementEntity>> =
        container.repo.movements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** El ojo de Inicio también oculta los montos aquí. */
    val ocultarMontos: StateFlow<Boolean> = container.store.hideAmounts

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    fun cerrarError() { _error.value = null }

    private var profileId: Long = 0
    private var profileName: String = ""
    init {
        viewModelScope.launch { container.activeProfileId.collect { id -> profileId = id ?: 0; profileName = id?.let { container.repo.profile(it)?.name } ?: "" } }
    }

    /**
     * "+ Agregar cliente": si ya hay uno con el mismo nombre (sin importar tildes ni mayúsculas)
     * se usa ese en vez de duplicarlo.
     */
    fun agregarCliente(nombre: String, celular: String, onListo: (Long) -> Unit) {
        viewModelScope.launch {
            val existente = container.repo.clients.first().firstOrNull { normalizar(it.name) == normalizar(nombre) }
            onListo(existente?.id ?: container.repo.addClient(nombre, celular))
        }
    }

    /** Guarda el fiado en la caja abierta. Si algo falla queda en [error] y no se llama [onListo]. */
    fun guardarFiado(clientId: Long, queSeLlevo: String, monto: Long, onListo: () -> Unit) {
        viewModelScope.launch {
            val r = container.registrarMovimiento(
                MovementEntity(
                    type = MovementType.FIADO.name, amount = monto, title = "Fiado", detail = queSeLlevo.trim(),
                    payMethod = PayMethod.EFECTIVO.name, clientId = clientId, profileId = profileId, profileName = profileName,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            when (r) {
                is Resultado.Ok -> onListo()
                is Resultado.Error -> _error.value = r.mensaje
            }
        }
    }

    /** "Sí, me pagó": abono en efectivo por todo lo que debe; [onListo] recibe cuánto entró a la caja. */
    fun marcarPagado(clientId: Long, onListo: (Long) -> Unit) {
        viewModelScope.launch {
            val saldo = Fiados.cuentas(container.repo.clients.first(), container.repo.movements.first())
                .firstOrNull { it.cliente.id == clientId }?.saldo ?: 0L
            if (saldo <= 0) return@launch
            guardarPago(clientId, saldo, PayMethod.EFECTIVO) { onListo(saldo) }
        }
    }

    /**
     * "Anular este fiado": anula los fiados y abonos de la deuda actual, igual que eliminar en
     * Movimientos (quedan tachados, no se borran) y también en la nube.
     */
    fun anularFiado(clientId: Long, onListo: () -> Unit) {
        viewModelScope.launch {
            val deuda = Fiados.deudaActual(clientId, container.repo.movements.first())
            val nube = container.enLaNube(container.perfilActivo())
            deuda.forEach { m ->
                container.repo.voidMovement(m.id)
                val remoteId = m.remoteId
                if (nube != null && remoteId != null) container.movimientosRemotos.anular(nube.first, remoteId, nube.second)
            }
            onListo()
        }
    }

    /** Registrar pago: el abono en efectivo entra a la caja; por transferencia solo baja la deuda. */
    fun guardarPago(clientId: Long, monto: Long, metodo: PayMethod, onListo: () -> Unit) {
        viewModelScope.launch {
            val r = container.registrarMovimiento(
                MovementEntity(
                    type = MovementType.ABONO.name, amount = monto, title = "Abono", detail = "Abono a fiado",
                    payMethod = metodo.name, clientId = clientId, profileId = profileId, profileName = profileName,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            when (r) {
                is Resultado.Ok -> onListo()
                is Resultado.Error -> _error.value = r.mensaje
            }
        }
    }

    private fun normalizar(s: String) =
        Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase().replace(Regex("\\s+"), " ")
}
