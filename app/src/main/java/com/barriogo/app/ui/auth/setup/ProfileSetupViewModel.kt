package com.barriogo.app.ui.auth.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.AppContainer
import com.barriogo.app.data.Hasher
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.domain.PinPolicy
import com.barriogo.app.domain.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileSetupUiState(
    val pin: String = "",
    val name: String = "",
    val role: Role? = null,
    val profileId: Long? = null,
    val saving: Boolean = false,
) {
    val profileReady get() = name.isNotBlank() && role != null
}

/** Primer perfil de la tienda: R5 (clave de 4 números) → R6 (nombre y rol) → R7 (huella). */
class ProfileSetupViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ProfileSetupUiState())
    val state: StateFlow<ProfileSetupUiState> = _state.asStateFlow()

    fun pressDigit(ch: Char) = _state.update { if (it.pin.length < PinPolicy.LENGTH) it.copy(pin = it.pin + ch) else it }

    fun backspace() = _state.update { it.copy(pin = it.pin.dropLast(1)) }

    fun setName(v: String) = _state.update { it.copy(name = v) }

    fun setRole(r: Role) = _state.update { it.copy(role = r) }

    /** R6: crea el perfil con la clave de R5 y lo deja como perfil activo. */
    fun createProfile(onDone: () -> Unit) {
        val s = _state.value
        val role = s.role ?: return
        if (s.name.isBlank() || s.pin.length != PinPolicy.LENGTH || s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val salt = Hasher.newSalt()
            val id = container.repo.addProfile(
                ProfileEntity(name = s.name.trim(), role = role.name, pinHash = Hasher.hash(salt, s.pin), pinSalt = salt),
            )
            container.activeProfileId.value = id
            _state.update { it.copy(profileId = id, saving = false) }
            onDone()
        }
    }

    /** R7: marca el perfil para entrar con huella (solo se llama tras validar la huella con Android). */
    fun enableBiometric(onDone: () -> Unit) {
        val id = _state.value.profileId ?: return onDone()
        viewModelScope.launch {
            container.repo.profile(id)?.let { container.repo.updateProfile(it.copy(biometric = true)) }
            onDone()
        }
    }
}
