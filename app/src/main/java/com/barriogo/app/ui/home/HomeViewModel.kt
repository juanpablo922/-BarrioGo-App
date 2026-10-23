package com.barriogo.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.AppContainer
import com.barriogo.app.data.caja.Caja
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.cashDelta
import com.barriogo.app.domain.kind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val session: Caja? = null,
    val todayMovements: List<MovementEntity> = emptyList(),
    val fiadoTotal: Long = 0,
    val fiadoClients: Int = 0,
    val hideAmounts: Boolean = false,
) {
    val saldoHoy: Long get() = (session?.openingAmount ?: 0L) + todayMovements.sumOf { it.cashDelta() }
    val entradas: Long get() = todayMovements.filter { it.cashDelta() > 0 }.sumOf { it.cashDelta() }
    val salidas: Long get() = -todayMovements.filter { it.cashDelta() < 0 }.sumOf { it.cashDelta() }
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _profile = MutableStateFlow<ProfileEntity?>(null)
    val profile: StateFlow<ProfileEntity?> = _profile

    val state: StateFlow<HomeUiState> = combine(
        container.caja.abierta,
        container.repo.movements,
        container.store.hideAmounts,
    ) { session, movements, hide ->
        val today = movements.filter {
            !it.voided && Dates.localDate(it.createdAt) == Dates.localDate(System.currentTimeMillis())
        }
        val balances = HashMap<Long, Long>()
        for (m in movements) {
            if (m.voided || m.clientId == null) continue
            val delta = when (m.kind) {
                MovementType.FIADO -> m.amount
                MovementType.ABONO -> -m.amount
                else -> 0L
            }
            balances[m.clientId] = (balances[m.clientId] ?: 0L) + delta
        }
        val withDebt = balances.filterValues { it > 0 }
        HomeUiState(session = session, todayMovements = today, fiadoTotal = withDebt.values.sum(), fiadoClients = withDebt.size, hideAmounts = hide)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch {
            container.activeProfileId.collect { id -> _profile.value = id?.let { container.repo.profile(it) } }
        }
    }

    fun toggleHide() { container.store.setHide(!container.store.hideAmounts.value) }

    fun signOutOfStore() = container.signOutOfStore()

    fun switchProfile() { container.activeProfileId.value = null }
}
