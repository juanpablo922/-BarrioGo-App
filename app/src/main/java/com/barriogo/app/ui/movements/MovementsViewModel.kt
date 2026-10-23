package com.barriogo.app.ui.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.AppContainer
import com.barriogo.app.data.ClientEntity
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import com.barriogo.app.domain.ReglasGasto
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** M1 · Registrar y M2 · Libro: ventas, fiados y abonos (Room) junto con los gastos (Firestore). */
class MovementsViewModel(private val container: AppContainer) : ViewModel() {
    val movements: StateFlow<List<MovementEntity>> =
        container.repo.movements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val clients: StateFlow<List<ClientEntity>> =
        container.repo.clients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Registro manual: queda en la caja abierta de Firestore (los gastos, en Firestore). */
    fun register(
        type: MovementType,
        amount: Long,
        title: String,
        detail: String = "",
        quantity: Int = 1,
        payMethod: PayMethod = PayMethod.EFECTIVO,
        clientId: Long? = null,
        extra: String = "",
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            val r = container.registrarMovimiento(
                MovementEntity(
                    type = type.name, amount = amount, title = title, detail = detail, quantity = quantity,
                    payMethod = payMethod.name, clientId = clientId, profileId = 0, profileName = "",
                    createdAt = System.currentTimeMillis(), extra = extra,
                ),
            )
            if (r is Resultado.Ok) onDone()
        }
    }

    fun addClient(name: String, phone: String = "", onDone: (Long) -> Unit) {
        viewModelScope.launch { onDone(container.repo.addClient(name, phone)) }
    }

    /** Eliminar: queda en el historial tachado y deja de contar en los totales. */
    fun voidMovement(id: Long) {
        viewModelScope.launch {
            val perfil = container.perfilActivo() ?: return@launch
            // Los gastos viven en Firestore (id local negativo).
            gastoDe(id)?.let { g -> g.remoteId?.let { container.gastos.anular(it, perfil) }; return@launch }
            container.repo.voidMovement(id)
            val remoteId = container.repo.movement(id)?.remoteId ?: return@launch
            container.enLaNube(perfil)?.let { container.movimientosRemotos.anular(it.first, remoteId, it.second) }
        }
    }

    fun editMovement(id: Long, amount: Long, title: String) {
        viewModelScope.launch {
            val perfil = container.perfilActivo() ?: return@launch
            gastoDe(id)?.let { g ->
                g.remoteId?.let { container.gastos.editar(it, ReglasGasto.tipoDe(g.category, g.detail), amount, title, perfil) }
                return@launch
            }
            container.repo.editMovement(id, amount, title)
            val m = container.repo.movement(id)?.takeIf { it.remoteId != null } ?: return@launch
            container.enLaNube(perfil)?.let { container.movimientosRemotos.editar(it.first, m, it.second) }
        }
    }

    private fun gastoDe(id: Long): MovementEntity? =
        if (id < 0) movements.value.firstOrNull { it.id == id && it.type == MovementType.GASTO.name } else null
}
