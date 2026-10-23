package com.barriogo.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.data.auth.AuthRepository
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.domain.Validaciones
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PasoRecuperar { CORREO, CODIGO, NUEVA, LISTO }

data class RecuperarState(
    val paso: PasoRecuperar = PasoRecuperar.CORREO,
    val correo: String = "",
    val codigo: String = "",
    val contrasena: String = "",
    val confirmacion: String = "",
    val token: String? = null,
    /** Segundos que faltan para poder pedir otro código. */
    val reenviarEn: Int = 0,
    val intentosRestantes: Int? = null,
    val cargando: Boolean = false,
    val error: String? = null,
) {
    val requisitosPendientes: List<String> get() = Validaciones.problemasContrasena(contrasena)
}

/**
 * RP1–RP3 · Olvidé mi contraseña con código de 6 números (funciones solicitarCodigo,
 * verificarCodigo y restablecerContrasena del backend). Si el correo no tiene cuenta, el servidor
 * lo dice y la pantalla se queda en el paso 1 con el mensaje. El código vence a los 10 minutos y
 * admite 5 intentos; el paso 3 tiene 15 minutos.
 */
class RecuperarViewModel(private val repo: AuthRepository, correoInicial: String = "") : ViewModel() {
    private val _estado = MutableStateFlow(RecuperarState(correo = correoInicial))
    val estado: StateFlow<RecuperarState> = _estado.asStateFlow()
    private var cuentaRegresiva: Job? = null

    fun onCorreo(v: String) = _estado.update { it.copy(correo = v, error = null) }
    fun onContrasena(v: String) = _estado.update { it.copy(contrasena = v, error = null) }
    fun onConfirmacion(v: String) = _estado.update { it.copy(confirmacion = v, error = null) }

    /** El campo solo acepta números; con 6 verifica solo. */
    fun onCodigo(v: String) {
        val limpio = v.filter(Char::isDigit).take(6)
        _estado.update { it.copy(codigo = limpio, error = null) }
        if (limpio.length == 6) verificar()
    }

    /** RP1 · "Enviar código", y también "Reenviar código" en RP2. */
    fun enviarCodigo() {
        val s = _estado.value
        if (s.cargando || s.reenviarEn > 0) return
        if (!Validaciones.correoValido(s.correo)) {
            _estado.update { it.copy(error = "Ingresa un correo electrónico válido") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val r = repo.solicitarCodigo(s.correo)) {
                is Resultado.Ok -> {
                    _estado.update {
                        it.copy(cargando = false, paso = PasoRecuperar.CODIGO, codigo = "", intentosRestantes = null)
                    }
                    iniciarCuenta(SEGUNDOS_PARA_REENVIAR)
                }
                is Resultado.Error -> _estado.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    /** RP2 · Verificar el código. */
    fun verificar() {
        val s = _estado.value
        if (s.cargando) return
        if (!Validaciones.codigoValido(s.codigo)) {
            _estado.update { it.copy(error = "El código tiene 6 números") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val r = repo.verificarCodigo(s.correo, s.codigo)) {
                is Resultado.Ok -> _estado.update { it.copy(cargando = false, token = r.valor, paso = PasoRecuperar.NUEVA) }
                is Resultado.Error -> _estado.update {
                    it.copy(
                        cargando = false,
                        codigo = "",
                        error = r.mensaje,
                        intentosRestantes = (r.detalles?.get("intentosRestantes") as? Number)?.toInt(),
                    )
                }
            }
        }
    }

    /** RP3 · Guardar la nueva contraseña. */
    fun guardar() {
        val s = _estado.value
        if (s.cargando) return
        if (s.requisitosPendientes.isNotEmpty()) {
            _estado.update { it.copy(error = "La contraseña no cumple los requisitos") }
            return
        }
        if (s.contrasena != s.confirmacion) {
            _estado.update { it.copy(error = "Las contraseñas no coinciden") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val r = repo.restablecerContrasena(s.correo, s.token.orEmpty(), s.contrasena)) {
                is Resultado.Ok -> _estado.update {
                    it.copy(cargando = false, paso = PasoRecuperar.LISTO, contrasena = "", confirmacion = "", token = null)
                }
                is Resultado.Error -> _estado.update {
                    // Se acabó el tiempo del paso 3: hay que pedir otro código.
                    if (r.codigo == "failed-precondition") {
                        it.copy(cargando = false, paso = PasoRecuperar.CORREO, token = null, error = r.mensaje)
                    } else {
                        it.copy(cargando = false, error = r.mensaje)
                    }
                }
            }
        }
    }

    /** Flecha de volver dentro del flujo: empieza de nuevo desde el correo. */
    fun volver() = _estado.update {
        it.copy(paso = PasoRecuperar.CORREO, codigo = "", token = null, contrasena = "", confirmacion = "", error = null)
    }

    private fun iniciarCuenta(segundos: Int) {
        cuentaRegresiva?.cancel()
        cuentaRegresiva = viewModelScope.launch {
            for (s in segundos downTo 0) {
                _estado.update { it.copy(reenviarEn = s) }
                delay(1_000)
            }
        }
    }

    private companion object {
        /** El servidor permite 5 códigos por hora; esperar un minuto entre uno y otro evita gastarlos. */
        const val SEGUNDOS_PARA_REENVIAR = 60
    }
}
