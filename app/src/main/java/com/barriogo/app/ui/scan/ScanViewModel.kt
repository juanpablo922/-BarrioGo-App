package com.barriogo.app.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.AppContainer
import com.barriogo.app.ai.InvoiceAgent
import com.barriogo.app.ai.InvoiceDraft
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ScanPhase { CAMERA, READING, REVIEW, ERROR }

data class ScanUiState(val phase: ScanPhase = ScanPhase.CAMERA, val draft: InvoiceDraft? = null, val error: String? = null)

/** ViewModel del escáner de facturas: percibe (cámara + OCR), interpreta (InvoiceAgent) y guarda al confirmar. */
class ScanViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state

    private var profileId = 0L
    private var profileName = ""
    private var sessionId: Long? = null

    init {
        viewModelScope.launch { container.activeProfileId.collect { id -> profileId = id ?: 0; profileName = id?.let { container.repo.profile(it)?.name } ?: "" } }
        viewModelScope.launch { container.repo.activeSession.collect { sessionId = it?.id } }
    }

    fun onReading() { _state.value = _state.value.copy(phase = ScanPhase.READING, error = null) }

    fun onOcrText(text: String) {
        val draft = InvoiceAgent.parse(text)
        _state.value = _state.value.copy(phase = ScanPhase.REVIEW, draft = draft)
    }

    fun onOcrError(message: String) { _state.value = _state.value.copy(phase = ScanPhase.ERROR, error = message) }

    fun retry() { _state.value = ScanUiState() }

    fun updateDraft(draft: InvoiceDraft) { _state.value = _state.value.copy(draft = draft) }

    fun confirm(draft: InvoiceDraft? = null, onSaved: () -> Unit) {
        val d = draft ?: _state.value.draft ?: return
        viewModelScope.launch {
            container.repo.insertMovement(
                MovementEntity(
                    type = MovementType.GASTO.name,
                    amount = d.total,
                    title = d.supplier ?: "Gasto de proveedor",
                    detail = "Proveedor",
                    payMethod = PayMethod.EFECTIVO.name,
                    profileId = profileId,
                    profileName = profileName,
                    sessionId = sessionId,
                    createdAt = d.dateMillis ?: System.currentTimeMillis(),
                    extra = "factura" + (d.number?.let { " · $it" } ?: ""),
                ),
            )
            onSaved()
        }
    }
}
