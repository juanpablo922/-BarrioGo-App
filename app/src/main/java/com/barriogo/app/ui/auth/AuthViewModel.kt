package com.barriogo.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.AppContainer
import com.barriogo.app.data.Hasher
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.data.auth.DatosRegistro
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.domain.BUSINESS_CATEGORIES
import com.barriogo.app.domain.Role
import com.barriogo.app.domain.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Datos que se piden en el registro del negocio (pantallas R1-R3 del prototipo). */
data class RegisterState(
    val step: Int = 0,                       // 0 = datos del propietario, 1 = categoría, 2 = contraseña
    val ownerName: String = "",
    val storeName: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    /** "Abarrotes", ..., o el texto libre que escribe la persona en "Otros". */
    val category: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    /** Creando la cuenta y la tienda en Firebase. */
    val loading: Boolean = false,
    val error: String? = null,
) {
    /** Primer problema del paso actual, o null si se puede seguir. Mismas reglas que firestore.rules. */
    val problem: String? get() = when (step) {
        0 -> when {
            !Validaciones.textoValido(ownerName, 120) -> "Ingresa tu nombre completo"
            !Validaciones.textoValido(storeName, 120) -> "Ingresa el nombre de tu tienda"
            !Validaciones.textoValido(address, 200) -> "Ingresa la dirección del negocio"
            !Validaciones.correoValido(email) -> "Ingresa un correo electrónico válido"
            !Validaciones.celularValido(phone) -> "El celular debe tener 10 números y empezar por 3"
            else -> null
        }
        1 -> Validaciones.problemaCategoria(category)
        else -> when {
            Validaciones.problemasContrasena(password).isNotEmpty() -> "La contraseña no cumple los requisitos"
            password != confirmPassword -> "Las contraseñas no coinciden"
            else -> null
        }
    }
}

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

    private val _pin = MutableStateFlow(PinState())
    val pin: StateFlow<PinState> = _pin

    private var creatingProfile = false

    // ---- Registro del negocio ----
    fun updateRegister(f: (RegisterState) -> RegisterState) { _register.value = f(_register.value).copy(error = null) }

    /** Valida el paso actual y avanza. Devuelve true si es el último paso y está completo. */
    fun nextStep(): Boolean {
        val s = _register.value
        s.problem?.let { p ->
            _register.value = s.copy(error = p)
            return false
        }
        if (s.step < 2) {
            _register.value = s.copy(step = s.step + 1, error = null)
            return false
        }
        return true
    }

    fun prevStep() {
        val s = _register.value
        if (s.step > 0) _register.value = s.copy(step = s.step - 1, error = null)
    }

    /**
     * R3 · "Finalizar registro": crea la cuenta en Firebase Auth y la tienda con la función
     * registrarTienda. Necesita internet. [onDone] se llama solo si todo quedó guardado.
     */
    fun finishRegistration(onDone: () -> Unit) {
        if (_register.value.loading || !nextStep()) return
        val s = _register.value
        viewModelScope.launch {
            _register.update { it.copy(loading = true, error = null) }
            val datos = DatosRegistro(
                nombreCompleto = s.ownerName,
                celular = s.phone,
                correo = s.email,
                contrasena = s.password,
                nombreTienda = s.storeName,
                direccion = s.address,
                categoria = s.category,
            )
            when (val r = container.authRepo.registrar(datos)) {
                is Resultado.Ok -> {
                    // Copia local para mostrar los datos sin internet.
                    store.tiendaId = r.valor
                    store.demo = false
                    container.sincronizarPerfiles()
                    store.ownerName = s.ownerName.trim()
                    store.storeName = s.storeName.trim()
                    store.address = s.address.trim()
                    store.phone = Validaciones.normalizarCelular(s.phone)
                    store.email = Validaciones.normalizarCorreo(s.email)
                    store.category = s.category.trim()
                    _register.update { it.copy(loading = false) }
                    onDone()
                }
                is Resultado.Error -> _register.update { it.copy(loading = false, error = r.mensaje) }
            }
        }
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

    /** R5 · "Crea tu clave": se guarda en memoria hasta terminar "Crea tu perfil". */
    fun guardarClaveEnCurso(clave: String) {
        container.claveEnCurso = clave
    }

    /** ¿Ya se escribió la clave del perfil que se está creando? */
    val hayClaveEnCurso: Boolean get() = container.claveEnCurso != null

    /**
     * R6 / P1 · "Crear perfil": un solo perfil con la clave de R5. En una cuenta real lo crea el
     * servidor (crearPerfil); el primero de la tienda debe ser administrador y los siguientes los crea
     * un administrador (el perfil activo). En la cuenta demo queda solo en el celular.
     */
    fun crearPerfil(name: String, role: Role, onDone: (Long) -> Unit, onError: (String) -> Unit) {
        val clave = container.claveEnCurso
        if (clave == null || !Validaciones.claveValida(clave)) return onError("Primero crea la clave de 4 números.")
        if (creatingProfile) return
        creatingProfile = true
        viewModelScope.launch {
            val tienda = store.tiendaId
            val resultado = if (store.demo || tienda == null) {
                Resultado.Ok(container.perfiles.crearLocal(name, role, clave))
            } else {
                val actor = container.activeProfileId.value?.let { container.repo.profile(it) }
                    ?.takeIf { it.isAdmin }?.remoteId
                    ?: container.adminParaCrearPerfiles()
                val r = container.perfiles.crear(tienda, name, role, clave, actor)
                if (r is Resultado.Error && r.codigo == "permission-denied") {
                    Resultado.Error("Para crear un perfil, primero entra con la clave de un administrador.")
                } else {
                    r
                }
            }
            when (resultado) {
                is Resultado.Ok -> {
                    container.claveEnCurso = null
                    store.registered = true
                    store.signedIn = true
                    container.activeProfileId.value = resultado.valor
                    container.sincronizarPerfiles()
                    onDone(resultado.valor)
                }
                is Resultado.Error -> onError(resultado.mensaje)
            }
            creatingProfile = false
        }
    }

    /** Huella activada (o no) para el perfil activo en este celular. */
    fun guardarHuella(activa: Boolean) {
        val perfil = container.activeProfileId.value ?: return
        container.alcance.launch {
            container.perfiles.guardarHuella(perfil, activa, store.tiendaId, store.dispositivoId)
        }
    }
}
