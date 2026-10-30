package com.barriogo.app.ui.voice

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.AppContainer
import com.barriogo.app.ai.AgentResult
import com.barriogo.app.ai.ClientRef
import com.barriogo.app.ai.MovementAgent
import com.barriogo.app.ai.MovementDraft
import com.barriogo.app.ai.VoiceRecognizer
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class VoicePhase { IDLE, LISTENING, PROCESSING, PROPOSAL, NOT_UNDERSTOOD, SAVED }

data class VoiceUiState(
    val phase: VoicePhase = VoicePhase.IDLE,
    val mode: MovementType = MovementType.VENTA,
    val transcript: String = "",
    val draft: MovementDraft? = null,
    val reason: String? = null,
    val error: String? = null,
)

/**
 * ViewModel de la captura por voz. Es el puente entre el sensor (VoiceRecognizer) y el agente
 * de reglas (MovementAgent): perciBe -> interpreta -> valida -> (el usuario) decide y confirma -> actúa.
 */
class VoiceViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state

    private var recognizer: VoiceRecognizer? = null
    private var profileId: Long = 0
    private var profileName: String = ""

    init {
        viewModelScope.launch { container.activeProfileId.collect { id -> profileId = id ?: 0; profileName = id?.let { container.repo.profile(it)?.name } ?: "" } }
    }

    fun setMode(mode: MovementType) { _state.value = _state.value.copy(mode = mode) }

    fun startListening(context: Context) {
        val r = recognizer ?: VoiceRecognizer(context).also { recognizer = it }
        _state.value = _state.value.copy(phase = VoicePhase.LISTENING, error = null)
        r.start(
            onListening = { _state.value = _state.value.copy(phase = VoicePhase.LISTENING) },
            onResult = { text -> onTranscript(text) },
            onError = { msg -> _state.value = _state.value.copy(phase = VoicePhase.IDLE, error = msg) },
        )
    }

    fun stopListening() { recognizer?.stop() }

    fun onTranscript(text: String) {
        _state.value = _state.value.copy(phase = VoicePhase.PROCESSING, transcript = text)
        viewModelScope.launch {
            // Una sola lectura del estado actual de clientes; no hace falta observarlos en vivo aquí.
            val clients = container.fiados.clientes.first().map { c -> ClientRef(c.id, c.name) }
            when (val r = MovementAgent.parse(text, _state.value.mode, clients)) {
                is AgentResult.Ok -> _state.value = _state.value.copy(phase = VoicePhase.PROPOSAL, draft = r.draft, error = null)
                is AgentResult.NotUnderstood -> _state.value = _state.value.copy(phase = VoicePhase.NOT_UNDERSTOOD, reason = r.reason)
            }
        }
    }

    /** V4 · Editar por voz: cambia solo lo que se dijo en la corrección, sin perder el resto. */
    fun editWithVoice(text: String) {
        val current = _state.value.draft ?: return
        val updated = MovementAgent.applyEdit(current, text)
        _state.value = _state.value.copy(draft = updated, transcript = text)
    }

    fun retry() { _state.value = _state.value.copy(phase = VoicePhase.IDLE, draft = null, reason = null, transcript = "") }

    fun confirm(onSaved: () -> Unit) {
        val d = _state.value.draft ?: return
        viewModelScope.launch {
            val clientId = d.clientId
            // El cliente nuevo necesita su celular: por voz no se puede crear, se agrega en Fiados.
            if (clientId == null && d.type == MovementType.FIADO && !d.heardClient.isNullOrBlank()) {
                _state.value = _state.value.copy(
                    phase = VoicePhase.PROPOSAL,
                    error = "${d.heardClient} no está en tus clientes. Agrégalo en Fiados con su celular y vuelve a intentarlo.",
                )
                return@launch
            }
            // Gastos, fiados y abonos van a Firestore; ventas a Room, en la caja abierta.
            val r = container.registrarMovimiento(
                MovementEntity(
                    type = d.type.name,
                    amount = d.amount,
                    title = when (d.type) {
                        MovementType.VENTA -> if (d.items.isEmpty()) "Venta" else d.itemText
                        MovementType.GASTO -> if (d.items.isEmpty()) "Gasto" else d.itemText
                        else -> "Fiado"
                    },
                    detail = when (d.type) {
                        MovementType.VENTA -> "Por voz"
                        MovementType.GASTO -> "Gasto por voz"
                        else -> d.itemText
                    },
                    quantity = d.quantity,
                    payMethod = d.payMethod.name,
                    clientId = clientId,
                    profileId = profileId,
                    profileName = profileName,
                    createdAt = System.currentTimeMillis(),
                    extra = "voz",
                ),
            )
            if (r is Resultado.Error) {
                _state.value = _state.value.copy(phase = VoicePhase.PROPOSAL, error = r.mensaje)
                return@launch
            }
            _state.value = _state.value.copy(phase = VoicePhase.SAVED)
            onSaved()
        }
    }

    override fun onCleared() { recognizer?.release(); super.onCleared() }
}
