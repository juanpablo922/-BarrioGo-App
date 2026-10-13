package com.barriogo.app.ui.auth.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.AppContainer
import com.barriogo.app.data.Hasher
import com.barriogo.app.data.RecoveryCodeManager
import com.barriogo.app.domain.FormRules
import com.barriogo.app.domain.PasswordPolicy
import com.barriogo.app.domain.PinPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Qué se recupera: la contraseña de la tienda (RP1-RP3) o la clave de 4 números de un perfil (CP1-CP3). */
enum class RecoveryTarget(val title: String) {
    PASSWORD("Recuperar contraseña"),
    PIN("Recuperar clave"),
}

data class RecoveryUiState(
    val email: String = "",
    val emailError: String? = null,
    val code: String = "",
    val codeError: String? = null,
    /** Código a mostrar en pantalla mientras la app no tenga envío real de correos. */
    val demoCode: String? = null,
    val password: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val confirmError: String? = null,
    val pin: String = "",
    val busy: Boolean = false,
) {
    val maskedEmail get() = FormRules.maskEmail(email.trim().lowercase())
}

/** Flujo de recuperación compartido por las tres pantallas de cada subgrafo. */
class RecoveryViewModel(
    private val container: AppContainer,
    val target: RecoveryTarget,
    private val profileId: Long?,
) : ViewModel() {
    private val store = container.store
    private val codes = container.recoveryCodes

    private val _state = MutableStateFlow(RecoveryUiState())
    val state: StateFlow<RecoveryUiState> = _state.asStateFlow()

    fun onEmailChange(v: String) = _state.update { it.copy(email = v.trim(), emailError = null) }

    fun onCodeChange(v: String) = _state.update { it.copy(code = v, codeError = null) }

    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, passwordError = null, confirmError = null) }

    fun onConfirmChange(v: String) = _state.update { it.copy(confirmPassword = v, confirmError = null) }

    fun pressDigit(ch: Char) = _state.update { if (it.pin.length < PinPolicy.LENGTH) it.copy(pin = it.pin + ch) else it }

    fun backspace() = _state.update { it.copy(pin = it.pin.dropLast(1)) }

    /** RP1/CP1: el correo debe ser el de la tienda registrada en este celular. */
    fun sendCode(onSent: () -> Unit) {
        val email = _state.value.email
        val error = when {
            !FormRules.isEmail(email) -> "Ingresa un correo electrónico válido"
            !email.equals(store.email, ignoreCase = true) -> "Este correo no corresponde a la tienda registrada"
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(emailError = error) }
            return
        }
        deliverCode(onSent)
    }

    fun resendCode() = deliverCode {}

    private fun deliverCode(onSent: () -> Unit) {
        _state.update { it.copy(busy = true, code = "", codeError = null) }
        viewModelScope.launch {
            val demo = container.codeSender.send(store.email, codes.issue())
            _state.update { it.copy(busy = false, demoCode = demo) }
            onSent()
        }
    }

    /** RP2/CP2. */
    fun verifyCode(onValid: () -> Unit) {
        val code = _state.value.code
        if (code.length != RecoveryCodeManager.CODE_LENGTH) {
            _state.update { it.copy(codeError = "Escribe los ${RecoveryCodeManager.CODE_LENGTH} dígitos del código") }
        } else if (!codes.verify(code)) {
            _state.update { it.copy(codeError = "El código no es correcto o ya venció") }
        } else {
            onValid()
        }
    }

    /** RP3: guarda la nueva contraseña de la tienda. */
    fun savePassword(onDone: () -> Unit) {
        val s = _state.value
        val passwordError = if (PasswordPolicy.isValid(s.password)) null else "Tu contraseña aún no cumple los requisitos"
        val confirmError = if (s.password == s.confirmPassword) null else "Las contraseñas no coinciden"
        if (passwordError != null || confirmError != null) {
            _state.update { it.copy(passwordError = passwordError, confirmError = confirmError) }
            return
        }
        store.setPassword(s.password)
        codes.clear()
        onDone()
    }

    /** CP3: guarda la nueva clave del perfil y levanta el bloqueo por intentos. */
    fun savePin(onDone: () -> Unit) {
        val id = profileId ?: return
        val pin = _state.value.pin
        if (pin.length != PinPolicy.LENGTH) return
        viewModelScope.launch {
            val profile = container.repo.profile(id) ?: return@launch
            val salt = Hasher.newSalt()
            container.repo.updateProfile(profile.copy(pinSalt = salt, pinHash = Hasher.hash(salt, pin)))
            store.setFailedAttempts(id, 0)
            store.setLockUntil(id, 0)
            codes.clear()
            onDone()
        }
    }
}
