package com.barriogo.app.ui.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.AppContainer
import com.barriogo.app.data.ClientEntity
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MovementsViewModel(private val container: AppContainer) : ViewModel() {
    val movements: StateFlow<List<MovementEntity>> =
        container.repo.movements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val clients: StateFlow<List<ClientEntity>> =
        container.repo.clients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var profileId: Long = 0
    private var profileName: String = ""
    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId: StateFlow<Long?> = _activeSessionId

    init {
        viewModelScope.launch {
            container.activeProfileId.collect { id ->
                profileId = id ?: 0
                profileName = id?.let { container.repo.profile(it)?.name } ?: ""
            }
        }
        viewModelScope.launch {
            container.repo.activeSession.collect { _activeSessionId.value = it?.id }
        }
    }

    /** Registro manual o confirmado desde voz. [session] puede ser null si la caja está cerrada (solo fiado lo permite). */
    fun register(
        type: MovementType,
        amount: Long,
        title: String,
        detail: String = "",
        quantity: Int = 1,
        payMethod: PayMethod = PayMethod.EFECTIVO,
        clientId: Long? = null,
        sessionId: Long? = null,
        extra: String = "",
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            // Ventas y gastos de una cuenta real también van a Firestore (fiados y abonos todavía no).
            val remotos = container.movimientosRemotos
            val nube = container.enLaNube(container.perfilActivo())
            val caja = sessionId?.let { container.db.cashDao().byId(it) }?.remoteId
            val remoteId = if (nube != null && caja != null && remotos.sincroniza(type)) remotos.nuevoId(nube.first) else null
            val m = MovementEntity(
                type = type.name, amount = amount, title = title, detail = detail, quantity = quantity,
                payMethod = payMethod.name, clientId = clientId, profileId = profileId, profileName = profileName,
                sessionId = sessionId, createdAt = System.currentTimeMillis(), extra = extra, remoteId = remoteId,
            )
            val guardado = m.copy(id = container.repo.insertMovement(m))
            if (nube != null && caja != null && remoteId != null) remotos.crear(nube.first, remoteId, guardado, caja, nube.second)
            onDone()
        }
    }

    fun addClient(name: String, phone: String = "", onDone: (Long) -> Unit) {
        viewModelScope.launch { onDone(container.repo.addClient(name, phone)) }
    }

    /** Eliminar: queda en el historial tachado y deja de contar en los totales. */
    fun voidMovement(id: Long) {
        viewModelScope.launch {
            container.repo.voidMovement(id)
            val remoteId = container.repo.movement(id)?.remoteId ?: return@launch
            container.enLaNube(container.perfilActivo())?.let { container.movimientosRemotos.anular(it.first, remoteId, it.second) }
        }
    }

    fun editMovement(id: Long, amount: Long, title: String) {
        viewModelScope.launch {
            container.repo.editMovement(id, amount, title)
            val m = container.repo.movement(id)?.takeIf { it.remoteId != null } ?: return@launch
            container.enLaNube(container.perfilActivo())?.let { container.movimientosRemotos.editar(it.first, m, it.second) }
        }
    }
}
