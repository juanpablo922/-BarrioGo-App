package com.barriogo.app.ui.fiados

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

data class ClientDebt(val client: ClientEntity, val balance: Long, val lastMovement: MovementEntity?)

class FiadosViewModel(private val container: AppContainer) : ViewModel() {
    val clients: StateFlow<List<ClientEntity>> =
        container.repo.clients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debts: StateFlow<List<ClientDebt>> = combine(container.repo.clients, container.repo.movements) { clients, movements ->
        clients.mapNotNull { c ->
            val ms = movements.filter { !it.voided && it.clientId == c.id }
            val balance = ms.sumOf { m -> if (m.type == MovementType.FIADO.name) m.amount else if (m.type == MovementType.ABONO.name) -m.amount else 0L }
            if (balance <= 0) null else ClientDebt(c, balance, ms.maxByOrNull { it.createdAt })
        }.sortedByDescending { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var profileId: Long = 0
    private var profileName: String = ""
    private val _sessionId = MutableStateFlow<Long?>(null)
    init {
        viewModelScope.launch { container.activeProfileId.collect { id -> profileId = id ?: 0; profileName = id?.let { container.repo.profile(it)?.name } ?: "" } }
        viewModelScope.launch { container.repo.activeSession.collect { _sessionId.value = it?.id } }
    }

    fun addClient(name: String, phone: String, onDone: (Long) -> Unit) {
        viewModelScope.launch { onDone(container.repo.addClient(name, phone)) }
    }

    fun registerFiado(clientId: Long, itemsText: String, amount: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            container.repo.insertMovement(
                MovementEntity(
                    type = MovementType.FIADO.name, amount = amount, title = "Fiado", detail = itemsText,
                    payMethod = PayMethod.EFECTIVO.name, clientId = clientId, profileId = profileId, profileName = profileName,
                    sessionId = _sessionId.value, createdAt = System.currentTimeMillis(),
                ),
            )
            onDone()
        }
    }

    fun registerPayment(clientId: Long, amount: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            container.repo.insertMovement(
                MovementEntity(
                    type = MovementType.ABONO.name, amount = amount, title = "Abono", detail = "Abono a fiado",
                    payMethod = PayMethod.EFECTIVO.name, clientId = clientId, profileId = profileId, profileName = profileName,
                    sessionId = _sessionId.value, createdAt = System.currentTimeMillis(),
                ),
            )
            onDone()
        }
    }
}
