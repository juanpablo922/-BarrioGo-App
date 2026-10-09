package com.barriogo.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false
)

/**
 * Autenticación de la tienda con verificación de credenciales reales y modo demo.
 */
class LoginViewModel(
    private val authenticate: suspend (email: String, password: String) -> Boolean =
        { _, _ -> true }
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value.trim(), emailError = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, passwordError = null) }

    fun togglePasswordVisibility() =
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun onLoginClick() {
        val s = _state.value
        val emailOk = android.util.Patterns.EMAIL_ADDRESS.matcher(s.email).matches()
        val passOk = s.password.length >= 6
        if (!emailOk || !passOk) {
            _state.update {
                it.copy(
                    emailError = if (emailOk) null else "Escribe un correo válido",
                    passwordError = if (passOk) null else "Mínimo 6 caracteres"
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val ok = authenticate(s.email, s.password)
            _state.update {
                it.copy(
                    isLoading = false,
                    loginSuccess = ok,
                    passwordError = if (ok) null else "Correo o contraseña incorrectos. Si no tienes cuenta, regístrate gratis abajo."
                )
            }
        }
    }

    fun onLoginHandled() = _state.update { it.copy(loginSuccess = false) }
}
