package com.barriogo.app.ui.auth.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.AppContainer
import com.barriogo.app.data.Hasher
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.domain.PinPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PinLoginUiState(
    val profile: ProfileEntity? = null,
    val pin: String = "",
    val error: String? = null,
)

/**
 * L4 · Clave de 4 números de un perfil. Tras [PinPolicy.MAX_ATTEMPTS] fallos bloquea
 * [PinPolicy.LOCK_MS] (L6). Los intentos y el bloqueo se guardan, así que cerrar la app no los reinicia.
 */
class PinLoginViewModel(private val container: AppContainer, private val profileId: Long) : ViewModel() {
    private val store = container.store
    private val _state = MutableStateFlow(PinLoginUiState())
    val state: StateFlow<PinLoginUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.update { it.copy(profile = container.repo.profile(profileId)) } }
    }

    fun isLocked(): Boolean = store.lockUntil(profileId) > System.currentTimeMillis()

    fun backspace() = _state.update { it.copy(pin = it.pin.dropLast(1), error = null) }

    /** Agrega un número; al completar 4 los verifica. */
    fun pressDigit(ch: Char, onSuccess: () -> Unit, onLocked: () -> Unit) {
        val s = _state.value
        if (s.pin.length >= PinPolicy.LENGTH) return
        val pin = s.pin + ch
        _state.update { it.copy(pin = pin, error = null) }
        if (pin.length == PinPolicy.LENGTH) verify(pin, onSuccess, onLocked)
    }

    private fun verify(pin: String, onSuccess: () -> Unit, onLocked: () -> Unit) {
        val p = _state.value.profile ?: return
        if (Hasher.matches(p.pinSalt, pin, p.pinHash)) {
            signIn()
            onSuccess()
            return
        }
        val attempts = store.failedAttempts(profileId) + 1
        if (attempts >= PinPolicy.MAX_ATTEMPTS) {
            store.setFailedAttempts(profileId, 0)
            store.setLockUntil(profileId, System.currentTimeMillis() + PinPolicy.LOCK_MS)
            _state.update { it.copy(pin = "", error = null) }
            onLocked()
        } else {
            store.setFailedAttempts(profileId, attempts)
            val left = PinPolicy.MAX_ATTEMPTS - attempts
            _state.update { it.copy(pin = "", error = "Clave incorrecta. Te ${if (left == 1) "queda 1 intento" else "quedan $left intentos"}") }
        }
    }

    /** Deja el perfil como activo (también lo usa el ingreso con huella L5). */
    fun signIn() {
        store.setFailedAttempts(profileId, 0)
        container.activeProfileId.value = profileId
    }
}
