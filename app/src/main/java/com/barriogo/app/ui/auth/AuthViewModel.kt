package com.barriogo.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.AppContainer
import com.barriogo.app.data.Hasher
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.domain.BUSINESS_CATEGORIES
import com.barriogo.app.domain.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Datos que se piden en el registro del negocio (pantallas R1-R3 del prototipo). */
data class RegisterState(
    val step: Int = 0,                       // 0 = datos del propietario, 1 = categoría, 2 = contraseña
    val ownerName: String = "",
    val storeName: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val category: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val error: String? = null,
) {
    val step0Valid get() = ownerName.isNotBlank() && storeName.isNotBlank() && email.contains("@")
    val step1Valid get() = category.isNotBlank()
    val step2Valid get() = password.length >= 4 && password == confirmPassword
}

data class LoginState(val email: String = "", val password: String = "", val error: String? = null, val loading: Boolean = false)

enum class PinStage { ENTER, CONFIRM }

data class PinState(
    val stage: PinStage = PinStage.ENTER,
    val first: String = "",
    val current: String = "",
    val error: String? = null,
)

class AuthViewModel(private val container: AppContainer) : ViewModel() {
    val categories = BUSINESS_CATEGORIES
    val store = container.store
    val profiles = container.repo.profiles

    private val _register = MutableStateFlow(RegisterState())
    val register: StateFlow<RegisterState> = _register

    private val _login = MutableStateFlow(LoginState())
    val login: StateFlow<LoginState> = _login

    private val _pin = MutableStateFlow(PinState())
    val pin: StateFlow<PinState> = _pin

    // ---- Registro del negocio ----
    fun updateRegister(f: (RegisterState) -> RegisterState) { _register.value = f(_register.value).copy(error = null) }

    fun nextStep(): Boolean {
        val s = _register.value
        val ok = when (s.step) {
            0 -> s.step0Valid; 1 -> s.step1Valid; else -> s.step2Valid
        }
        if (!ok) {
            _register.value = s.copy(error = "Completa los datos para continuar")
            return false
        }
        if (s.step < 2) _register.value = s.copy(step = s.step + 1, error = null)
        return s.step >= 2
    }

    fun prevStep() {
        val s = _register.value
        if (s.step > 0) _register.value = s.copy(step = s.step - 1, error = null)
    }

    /** Guarda los datos del negocio (todavía no crea el perfil administrador: eso pasa al confirmar el PIN). */
    fun saveBusinessData() {
        val s = _register.value
        store.ownerName = s.ownerName.trim()
        store.storeName = s.storeName.trim()
        store.address = s.address.trim()
        store.phone = s.phone.trim()
        store.email = s.email.trim().lowercase()
        store.category = s.category
        store.setPassword(s.password)
    }

    // ---- Ingreso con correo y contraseña ----
    fun updateLogin(f: (LoginState) -> LoginState) { _login.value = f(_login.value).copy(error = null) }

    /** true si las credenciales son correctas. */
    fun submitLogin(): Boolean {
        val s = _login.value
        val ok = s.email.trim().equals(store.email, ignoreCase = true) && store.checkPassword(s.password)
        if (!ok) _login.value = s.copy(error = "Correo o contraseña incorrectos")
        else store.signedIn = true
        return ok
    }

    // ---- Creación / verificación de PIN ----
    fun resetPin() { _pin.value = PinState() }

    fun pressDigit(ch: Char) {
        val s = _pin.value
        if (s.current.length >= 4) return
        _pin.value = s.copy(current = s.current + ch, error = null)
    }

    fun backspace() { _pin.value = _pin.value.copy(current = _pin.value.current.dropLast(1), error = null) }

    /** Para crear un PIN nuevo: primero se escribe, luego se confirma. Devuelve el PIN final o null si aún falta. */
    fun advanceCreation(): String? {
        val s = _pin.value
        if (s.current.length < 4) return null
        return if (s.stage == PinStage.ENTER) {
            _pin.value = PinState(stage = PinStage.CONFIRM, first = s.current)
            null
        } else if (s.current == s.first) {
            s.current
        } else {
            _pin.value = PinState(stage = PinStage.ENTER, error = "Los PIN no coinciden, intenta de nuevo")
            null
        }
    }

    /** Crea el primer perfil (Administrador) al terminar el registro. */
    fun createAdminProfile(pinValue: String, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val salt = Hasher.newSalt()
            val id = container.repo.addProfile(
                ProfileEntity(
                    name = store.ownerName.ifBlank { "Administrador" },
                    role = Role.ADMIN.name,
                    pinHash = Hasher.hash(salt, pinValue),
                    pinSalt = salt,
                ),
            )
            store.registered = true
            store.signedIn = true
            container.activeProfileId.value = id
            onDone(id)
        }
    }

    fun createProfile(name: String, role: Role, pinValue: String, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val salt = Hasher.newSalt()
            val id = container.repo.addProfile(
                ProfileEntity(name = name, role = role.name, pinHash = Hasher.hash(salt, pinValue), pinSalt = salt),
            )
            onDone(id)
        }
    }
}
