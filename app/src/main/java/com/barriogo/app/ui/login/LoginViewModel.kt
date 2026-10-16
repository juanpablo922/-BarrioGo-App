package com.barriogo.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.domain.Validaciones
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
 * L2 · Ingreso de la tienda: cuenta real (Firebase Auth) o cuenta demo.
 * [authenticate] devuelve null si entra, o el mensaje de error para mostrar
 * (ver AppContainer.authenticateStore).
 */
class LoginViewModel(
    private val authenticate: suspend (email: String, password: String) -> String?,
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
        if (s.isLoading) return
        val emailOk = Validaciones.correoValido(s.email)
        val passOk = s.password.isNotEmpty()
        if (!emailOk || !passOk) {
            _state.update {
                it.copy(
                    emailError = if (emailOk) null else "Escribe un correo válido",
                    passwordError = if (passOk) null else "Escribe tu contraseña"
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val error = authenticate(s.email, s.password)
            _state.update {
                it.copy(
                    isLoading = false,
                    loginSuccess = error == null,
                    // Firebase no dice si el correo existe (protección contra la enumeración): un solo mensaje.
                    passwordError = when (error) {
                        null -> null
                        "Correo o contraseña incorrectos." -> "Correo o contraseña incorrectos. Si no tienes cuenta, regístrate gratis abajo."
                        else -> error
                    }
                )
            }
        }
    }

    fun onLoginHandled() = _state.update { it.copy(loginSuccess = false) }
}
