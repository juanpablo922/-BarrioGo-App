package com.barriogo.app.data

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Hash con sal para claves (PIN de 4 dígitos y contraseña de la tienda).
 * Nota: un PIN de 4 dígitos es débil por sí solo; por eso la app bloquea 30 s tras 3 intentos fallidos.
 */
object Hasher {
    fun newSalt(): String {
        val b = ByteArray(16)
        SecureRandom().nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }

    fun hash(salt: String, value: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest((salt + ":" + value).toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun matches(salt: String, value: String, expected: String): Boolean = hash(salt, value) == expected
}
