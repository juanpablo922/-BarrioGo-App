package com.barriogo.app.ui.auth

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Huella con BiometricPrompt de Android. La huella nunca sale del celular: Android la compara en
 * su hardware seguro y solo le dice a la app "sí era" o "no era".
 */
object Huella {
    private const val AUTENTICADORES = BiometricManager.Authenticators.BIOMETRIC_WEAK

    /** ¿El celular tiene sensor y una huella registrada? */
    fun disponible(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTENTICADORES) == BiometricManager.BIOMETRIC_SUCCESS

    /** Mensaje para cuando no se puede usar la huella. */
    fun motivoNoDisponible(context: Context): String = when (BiometricManager.from(context).canAuthenticate(AUTENTICADORES)) {
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Primero registra una huella en los ajustes del celular."
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE, BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
            "Este celular no tiene lector de huella disponible."
        else -> "No se puede usar la huella en este celular."
    }

    /**
     * Muestra el lector de huella. [onOk] solo se llama si Android confirma la huella.
     * Cancelar o tocar "Usar mi clave" no es un error: no llama a ninguno de los dos.
     */
    fun pedir(context: Context, titulo: String, subtitulo: String, onOk: () -> Unit, onError: (String) -> Unit) {
        val actividad = context.actividad() ?: return onError("No se puede usar la huella en esta pantalla.")
        val prompt = BiometricPrompt(
            actividad,
            ContextCompat.getMainExecutor(actividad),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onOk()
                override fun onAuthenticationError(codigo: Int, mensaje: CharSequence) {
                    val cancelado = codigo == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        codigo == BiometricPrompt.ERROR_USER_CANCELED ||
                        codigo == BiometricPrompt.ERROR_CANCELED
                    if (!cancelado) onError(mensaje.toString())
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(titulo)
                .setSubtitle(subtitulo)
                .setNegativeButtonText("Usar mi clave")
                .setAllowedAuthenticators(AUTENTICADORES)
                .build(),
        )
    }

    private tailrec fun Context.actividad(): FragmentActivity? = when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.actividad()
        else -> null
    }
}
