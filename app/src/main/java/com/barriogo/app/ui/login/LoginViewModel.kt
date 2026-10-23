package com.barriogo.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.data.auth.CREDENCIALES_INCORRECTAS
import com.barriogo.app.data.auth.SIN_CONEXION
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
    /** Firebase rechazó el correo/contraseña: resalta el campo de contraseña (Figma: Login=Credenciales). */
    val credentialsError: Boolean = false,
    /** Falló por red: aviso arriba del formulario y botón "Reintentar" (Figma: Login=Sin conexión). */
    val noConnection: Boolean = false,
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false
)

/**
 * L2 · Ingreso de la tienda con Firebase Auth.
 * [authenticate] devuelve null si entra, o el mensaje de error para mostrar
 * (ver AppContainer.authenticateStore).
 */
class LoginViewModel(
    private val authenticate: suspend (email: String, password: String) -> String?,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value.trim(), emailError = null, credentialsError = false, passwordError = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, passwordError = null, credentialsError = false) }

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
                    emailError = if (emailOk) null else "Ingresa un correo electrónico válido",
                    passwordError = if (passOk) null else "Ingresa tu contraseña",
                    credentialsError = false
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, noConnection = false, credentialsError = false) }
            val error = authenticate(s.email, s.password)
            _state.update {
                it.copy(
                    isLoading = false,
                    loginSuccess = error == null,
                    noConnection = error == SIN_CONEXION,
                    // Firebase no dice si el correo existe (protección contra la enumeración): un solo mensaje.
                    credentialsError = error == CREDENCIALES_INCORRECTAS,
                    passwordError = when (error) {
                        null, SIN_CONEXION -> null
                        CREDENCIALES_INCORRECTAS -> "Correo o contraseña incorrectos"
                        else -> error
                    }
                )
            }
        }
    }

    fun onLoginHandled() = _state.update { it.copy(loginSuccess = false) }
}
