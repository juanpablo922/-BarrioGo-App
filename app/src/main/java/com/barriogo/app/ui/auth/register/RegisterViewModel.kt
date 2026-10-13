package com.barriogo.app.ui.auth.register

import androidx.lifecycle.ViewModel
import com.barriogo.app.AppContainer
import com.barriogo.app.domain.BUSINESS_CATEGORIES
import com.barriogo.app.domain.FormRules
import com.barriogo.app.domain.PasswordPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Errores de R1 por campo (null = válido). */
data class OwnerErrors(
    val ownerName: String? = null,
    val storeName: String? = null,
    val address: String? = null,
    val email: String? = null,
    val phone: String? = null,
) {
    val hasAny get() = listOf(ownerName, storeName, address, email, phone).any { it != null }
}

data class RegisterUiState(
    // R1 · Datos del propietario
    val ownerName: String = "",
    val storeName: String = "",
    val address: String = "",
    val email: String = "",
    val phone: String = "",
    val ownerErrors: OwnerErrors = OwnerErrors(),
    // R2 · Categoría
    val category: String? = null,
    // R3 · Contraseña
    val password: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val confirmError: String? = null,
) {
    val passwordChecks get() = PasswordPolicy.checks(password)
}

/** Registro del negocio R1 → R2 → R3. Lo comparten las tres pantallas del subgrafo de registro. */
class RegisterViewModel(private val container: AppContainer) : ViewModel() {
    val categories = BUSINESS_CATEGORIES

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun update(f: (RegisterUiState) -> RegisterUiState) = _state.update { s ->
        val next = f(s)
        // Al corregir un campo se borra solo su error, no los de los demás.
        next.copy(
            ownerErrors = OwnerErrors(
                ownerName = s.ownerErrors.ownerName.takeIf { next.ownerName == s.ownerName },
                storeName = s.ownerErrors.storeName.takeIf { next.storeName == s.storeName },
                address = s.ownerErrors.address.takeIf { next.address == s.address },
                email = s.ownerErrors.email.takeIf { next.email == s.email },
                phone = s.ownerErrors.phone.takeIf { next.phone == s.phone },
            ),
            passwordError = s.passwordError.takeIf { next.password == s.password },
            confirmError = s.confirmError.takeIf { next.confirmPassword == s.confirmPassword && next.password == s.password },
        )
    }

    /** R1: true si los datos del propietario son válidos. */
    fun validateOwner(): Boolean {
        val s = _state.value
        val errors = OwnerErrors(
            ownerName = if (s.ownerName.isBlank()) "Escribe tu nombre completo" else null,
            storeName = if (s.storeName.isBlank()) "Escribe el nombre de la tienda" else null,
            address = if (s.address.isBlank()) "Escribe la dirección del negocio" else null,
            email = if (FormRules.isEmail(s.email)) null else "Ingresa un correo electrónico válido",
            phone = if (FormRules.isPhone(s.phone)) null else "Ingresa un celular de 10 dígitos",
        )
        _state.update { it.copy(ownerErrors = errors) }
        return !errors.hasAny
    }

    /** R3: guarda la tienda si la contraseña cumple las reglas y coincide con la confirmación. */
    fun finish(onDone: () -> Unit) {
        val s = _state.value
        val passwordError = if (PasswordPolicy.isValid(s.password)) null else "Tu contraseña aún no cumple los requisitos"
        val confirmError = if (s.password == s.confirmPassword) null else "Las contraseñas no coinciden"
        if (passwordError != null || confirmError != null) {
            _state.update { it.copy(passwordError = passwordError, confirmError = confirmError) }
            return
        }
        container.store.apply {
            ownerName = s.ownerName.trim()
            storeName = s.storeName.trim()
            address = s.address.trim()
            email = s.email.trim().lowercase()
            phone = s.phone.filter(Char::isDigit)
            category = s.category.orEmpty()
            setPassword(s.password)
            registered = true
            signedIn = true
        }
        onDone()
    }
}
