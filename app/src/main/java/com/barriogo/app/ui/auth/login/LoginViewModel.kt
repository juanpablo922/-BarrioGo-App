package com.barriogo.app.ui.auth.login

import androidx.lifecycle.ViewModel
import com.barriogo.app.AppContainer
import com.barriogo.app.domain.FormRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
)

/** L2 · Ingreso con el correo y la contraseña de la tienda (guardados con sal + hash en el celular). */
class LoginViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value.trim(), emailError = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null) }

    /** Valida el formulario y las credenciales; llama a [onSuccess] solo si todo es correcto. */
    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        val emailError = if (FormRules.isEmail(s.email)) null else "Ingresa un correo electrónico válido"
        val passwordError = if (s.password.isNotEmpty()) null else "Ingresa tu contraseña"
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        val store = container.store
        val ok = store.registered && s.email.equals(store.email, ignoreCase = true) && store.checkPassword(s.password)
        if (!ok) {
            _state.update { it.copy(passwordError = "Correo o contraseña incorrectos") }
            return
        }
        store.signedIn = true
        _state.update { LoginUiState() }
        onSuccess()
    }
}
