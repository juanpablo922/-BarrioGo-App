package com.barriogo.app.data

import java.security.SecureRandom

/**
 * Códigos de 6 dígitos para recuperar la contraseña de la tienda o la clave de un perfil (RP1-RP3,
 * CP1-CP3). Viven solo en memoria y vencen a los 10 minutos.
 */
class RecoveryCodeManager(private val clock: () -> Long = System::currentTimeMillis) {
    private var code: String? = null
    private var issuedAt = 0L
    private val random = SecureRandom()

    fun issue(): String {
        val value = (0 until CODE_LENGTH).joinToString("") { random.nextInt(10).toString() }
        code = value
        issuedAt = clock()
        return value
    }

    fun verify(input: String): Boolean {
        val current = code ?: return false
        if (clock() - issuedAt > TTL_MS) {
            clear()
            return false
        }
        return input == current
    }

    fun clear() { code = null }

    companion object {
        const val CODE_LENGTH = 6
        private const val TTL_MS = 10 * 60 * 1000L
    }
}

/**
 * Envío del código al correo de la tienda. La app no tiene servidor todavía, así que la
 * implementación por defecto ([DemoCodeSender]) no envía nada: devuelve el código para que la
 * pantalla lo muestre en modo demostración. Al tener backend basta con otra implementación.
 */
fun interface RecoveryCodeSender {
    /** Devuelve el código si debe mostrarse en pantalla (modo demo) o null si se envió de verdad. */
    suspend fun send(email: String, code: String): String?
}

object DemoCodeSender : RecoveryCodeSender {
    override suspend fun send(email: String, code: String): String = code
}
