package com.barriogo.app.data.auth

import com.barriogo.app.domain.Validaciones
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.functions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

sealed interface Resultado<out T> {
    data class Ok<T>(val valor: T) : Resultado<T>

    /**
     * [mensaje] viene listo para mostrar. [codigo] es el de Cloud Functions ("not-found",
     * "invalid-argument", ...) cuando el error viene del servidor, y [detalles] trae extras
     * como `intentosRestantes`.
     */
    data class Error(
        val mensaje: String,
        val codigo: String? = null,
        val detalles: Map<*, *>? = null,
    ) : Resultado<Nothing>
}

data class DatosRegistro(
    val nombreCompleto: String,
    val celular: String,
    val correo: String,
    val contrasena: String,
    val nombreTienda: String,
    val direccion: String,
    /** Como la guarda el registro: "Abarrotes", ..., o el texto libre de "Otros" ("Papelería"). */
    val categoria: String,
)

/** A dónde ir al abrir la app. */
enum class Destino { INGRESAR, COMPLETAR_REGISTRO, CREAR_CLAVE, ELEGIR_PERFIL }

data class Inicio(val destino: Destino, val tiendaId: String? = null)

/**
 * Cuenta del dueño (Firebase Auth) y tienda en el backend con Cloud Functions (plan Blaze).
 * Lo administrativo pasa por callables: registro, perfiles y recuperación de la contraseña.
 * Las funciones devuelven sus errores con mensajes en español, listos para mostrar.
 */
class AuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore,
    private val funciones: FirebaseFunctions = Firebase.functions(REGION),
) {
    val correoActual: String? get() = auth.currentUser?.email

    /** L2 · Necesita internet la primera vez en cada celular; después la sesión queda guardada. */
    suspend fun ingresar(correo: String, contrasena: String): Resultado<Unit> = intentar {
        auth.signInWithEmailAndPassword(Validaciones.normalizarCorreo(correo), contrasena).await()
        Unit
    }

    /**
     * R3 · Crea la cuenta en Firebase Auth y la tienda con la función `registrarTienda`.
     * Si la cuenta ya se creó en un intento anterior (se cayó la red), no la vuelve a crear; y si la
     * tienda alcanzó a guardarse, la reutiliza. Devuelve el id de la tienda.
     */
    suspend fun registrar(d: DatosRegistro): Resultado<String> = intentar {
        val correo = Validaciones.normalizarCorreo(d.correo)
        if (auth.currentUser?.email != correo) {
            auth.createUserWithEmailAndPassword(correo, d.contrasena).await()
        }
        val (categoriaId, categoriaOtra) = Validaciones.categoriaFirestore(d.categoria)
        try {
            val r = llamar(
                "registrarTienda",
                mapOf(
                    "propietario" to mapOf(
                        "nombreCompleto" to d.nombreCompleto.trim(),
                        "celular" to Validaciones.normalizarCelular(d.celular),
                    ),
                    "tienda" to mapOf(
                        "nombre" to d.nombreTienda.trim(),
                        "direccion" to d.direccion.trim(),
                        "categoriaNegocioId" to categoriaId,
                        "categoriaOtra" to categoriaOtra,
                    ),
                ),
            )
            r["tiendaId"] as String
        } catch (e: FirebaseFunctionsException) {
            // La tienda se guardó pero la respuesta no llegó: se usa la que ya existe.
            if (e.code == FirebaseFunctionsException.Code.ALREADY_EXISTS) tiendaDelUsuario() ?: throw e else throw e
        }
    }

    /** R5–R6 · Primer perfil (administrador). El servidor guarda la clave con hash PBKDF2. */
    suspend fun crearPrimerPerfil(tiendaId: String, nombre: String, clave: String): Resultado<String> = intentar {
        val r = llamar(
            "crearPerfil",
            mapOf("tiendaId" to tiendaId, "nombre" to nombre.trim(), "rol" to "administrador", "clave" to clave),
        )
        r["perfilId"] as String
    }

    /** Lee la tienda de la cuenta. Sin internet usa la caché de Firestore si ya se leyó antes. */
    suspend fun tiendaDelUsuario(): String? {
        val uid = auth.currentUser?.uid ?: return null
        val propietario = db.document("propietarios/$uid").get().await()
        return (propietario.get("tiendaIds") as? List<*>)?.firstOrNull() as? String
    }

    /** Splash: decide la primera pantalla. `tiendaGuardada` es la de SessionStore, para abrir sin internet. */
    suspend fun destinoInicial(tiendaGuardada: String?): Inicio {
        val usuario = auth.currentUser ?: return Inicio(Destino.INGRESAR)
        // Una sesión que el servidor ya no reconoce (cuenta de otro proyecto, borrada o con la
        // contraseña cambiada) se cierra. Sin internet no se puede revisar: se conserva.
        val revision = runCatching { withTimeout(8_000) { usuario.getIdToken(true).await() } }
        if (revision.exceptionOrNull() is FirebaseAuthException) {
            auth.signOut()
            return Inicio(Destino.INGRESAR)
        }
        val leida = runCatching { withTimeout(8_000) { tiendaDelUsuario() } }
        if (leida.isFailure) {
            // Sin red y sin caché: si este celular ya conocía la tienda, se sigue trabajando sin internet.
            return if (tiendaGuardada != null) Inicio(Destino.ELEGIR_PERFIL, tiendaGuardada) else Inicio(Destino.INGRESAR)
        }
        val tiendaId = leida.getOrNull() ?: return Inicio(Destino.COMPLETAR_REGISTRO)
        val tienePerfiles = runCatching {
            withTimeout(8_000) {
                db.collection("tiendas/$tiendaId/perfiles").whereEqualTo("eliminadoEn", null).limit(1).get().await().isEmpty.not()
            }
        }.getOrDefault(true)
        return Inicio(if (tienePerfiles) Destino.ELEGIR_PERFIL else Destino.CREAR_CLAVE, tiendaId)
    }

    // ── Olvidé mi contraseña (RP1–RP3) ─────────────────────────────────────────

    /** RP1 · Envía un código de 6 números al correo. Responde igual aunque el correo no tenga cuenta. */
    suspend fun solicitarCodigo(correo: String): Resultado<Int> = intentar {
        val r = llamar("solicitarCodigo", mapOf("correo" to Validaciones.normalizarCorreo(correo), "proposito" to PROPOSITO))
        (r["expiraEnSegundos"] as? Number)?.toInt() ?: 600
    }

    /** RP2 · Verifica el código. Devuelve un token de un solo uso para el paso 3. */
    suspend fun verificarCodigo(correo: String, codigo: String): Resultado<String> = intentar {
        val r = llamar(
            "verificarCodigo",
            mapOf("correo" to Validaciones.normalizarCorreo(correo), "proposito" to PROPOSITO, "codigo" to codigo),
        )
        r["token"] as String
    }

    /** RP3 · Cambia la contraseña con el token de RP2. */
    suspend fun restablecerContrasena(correo: String, token: String, contrasenaNueva: String): Resultado<Unit> = intentar {
        llamar(
            "restablecerContrasena",
            mapOf("correo" to Validaciones.normalizarCorreo(correo), "token" to token, "contrasenaNueva" to contrasenaNueva),
        )
        Unit
    }

    /** "Salir de la tienda": para volver hay que ingresar con correo y contraseña. */
    fun salir() = auth.signOut()

    private suspend fun llamar(nombre: String, datos: Map<String, Any?>): Map<*, *> =
        funciones.getHttpsCallable(nombre).call(datos).await().getData() as? Map<*, *> ?: emptyMap<Any, Any>()

    private suspend fun <T> intentar(bloque: suspend () -> T): Resultado<T> = try {
        Resultado.Ok(withTimeout(30_000) { bloque() })
    } catch (e: TimeoutCancellationException) {
        Resultado.Error(SIN_CONEXION)
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseFunctionsException) {
        Resultado.Error(mensajeDeError(e), e.code.name.lowercase().replace('_', '-'), e.details as? Map<*, *>)
    } catch (e: Exception) {
        Resultado.Error(mensajeDeError(e))
    }

    companion object {
        /** Misma región de functions/src/config.ts del backend. */
        const val REGION = "us-east1"
        private const val PROPOSITO = "recuperar_contrasena"
    }
}

const val SIN_CONEXION = "Sin conexión. Revisa tu internet e intenta de nuevo."
const val CREDENCIALES_INCORRECTAS = "Correo o contraseña incorrectos."

/** Errores de Firebase → mensajes en el tono de la app. El orden importa: hay subclases. */
fun mensajeDeError(e: Exception): String = when (e) {
    is FirebaseAuthUserCollisionException -> "Ya hay una cuenta con este correo. Ingresa o recupera tu contraseña."
    is FirebaseAuthWeakPasswordException -> "La contraseña no cumple los requisitos."
    is FirebaseAuthInvalidUserException,
    is FirebaseAuthInvalidCredentialsException -> CREDENCIALES_INCORRECTAS
    is FirebaseTooManyRequestsException -> "Demasiados intentos. Espera unos minutos e intenta de nuevo."
    is FirebaseNetworkException -> SIN_CONEXION
    is FirebaseFunctionsException -> when (e.code) {
        FirebaseFunctionsException.Code.UNAVAILABLE,
        FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> SIN_CONEXION
        // Las funciones de BarrioGo ya responden en español y en el tono de la app.
        else -> e.message?.takeIf { it.isNotBlank() } ?: "No pudimos completar la operación. Intenta de nuevo."
    }
    else -> "Algo salió mal. Intenta de nuevo."
}
