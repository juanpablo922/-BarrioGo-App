package com.barriogo.app.ui.cash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.AppContainer
import com.barriogo.app.data.ArqueoEntity
import com.barriogo.app.data.CashSessionEntity
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.MAX_OPENING_CHANGES
import com.barriogo.app.domain.cashDelta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CashUiState(
    val session: CashSessionEntity? = null,
    val todayMovements: List<MovementEntity> = emptyList(),
    val fiadoTotal: Long = 0,
    val profileName: String = "",
) {
    val saldo: Long get() = (session?.openingAmount ?: 0L) + todayMovements.sumOf { it.cashDelta() }
    val entradas: Long get() = todayMovements.filter { it.cashDelta() > 0 }.sumOf { it.cashDelta() }
    val salidas: Long get() = -todayMovements.filter { it.cashDelta() < 0 }.sumOf { it.cashDelta() }
    val editsLeft: Int get() = MAX_OPENING_CHANGES - (session?.editsUsed ?: 0)
}

class CashViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<CashUiState> = combine(
        container.repo.activeSession, container.repo.movements, container.repo.clients,
    ) { session, movements, _ ->
        val today = movements.filter { !it.voided && Dates.localDate(it.createdAt) == Dates.localDate(System.currentTimeMillis()) }
        val fiado = movements.filter { !it.voided && it.clientId != null }
            .groupBy { it.clientId }
            .mapValues { (_, ms) -> ms.sumOf { m -> if (m.type == "FIADO") m.amount else if (m.type == "ABONO") -m.amount else 0L } }
            .values.filter { it > 0 }.sum()
        CashUiState(session = session, todayMovements = today, fiadoTotal = fiado)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CashUiState())

    private var profileName: String = ""
    init {
        viewModelScope.launch {
            container.activeProfileId.collect { id -> profileName = id?.let { container.repo.profile(it)?.name } ?: "" }
        }
    }

    fun openCash(amount: Long, onDone: () -> Unit) {
        viewModelScope.launch { container.repo.openCash(amount, profileName); onDone() }
    }

    /** Devuelve false si ya se usaron los 3 cambios permitidos. */
    fun editOpening(newAmount: Long, onResult: (Boolean) -> Unit) {
        val s = state.value.session ?: return onResult(false)
        viewModelScope.launch { onResult(container.repo.changeOpening(s, newAmount) != null) }
    }

    fun saveArqueo(counted: Long, note: String, onDone: () -> Unit) {
        val s = state.value.session ?: return
        viewModelScope.launch {
            container.repo.saveArqueo(
                ArqueoEntity(sessionId = s.id, at = System.currentTimeMillis(), expected = state.value.saldo, counted = counted, note = note, by = profileName),
            )
            onDone()
        }
    }

    fun closeCash(counted: Long, note: String, onDone: () -> Unit) {
        val s = state.value.session ?: return
        viewModelScope.launch {
            container.repo.closeCash(s, counted = counted, expected = state.value.saldo, by = profileName, note = note)
            onDone()
        }
    }
}
