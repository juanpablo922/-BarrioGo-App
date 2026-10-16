package com.barriogo.app.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Clave de 4 números de los perfiles guardados en Firestore.
 * Formato pbkdf2_sha256$<iteraciones>$<sal base64>$<hash base64>: es el que exige firestore.rules
 * y el mismo de functions/src/dominio/seguridad.ts del backend.
 * Usa java.util.Base64 (API 26+) para poder probarlo con JUnit.
 */
object ClaveSegura {
    const val ITERACIONES = 120_000

    /** Tarda cerca de medio segundo en gama baja: llámenlo desde Dispatchers.Default. */
    fun hash(clave: String, iteraciones: Int = ITERACIONES): String {
        val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val b64 = Base64.getEncoder()
        return "pbkdf2_sha256\$$iteraciones\$${b64.encodeToString(sal)}\$${b64.encodeToString(pbkdf2(clave, sal, iteraciones, 32))}"
    }

    fun verificar(clave: String, guardado: String): Boolean {
        val p = guardado.split("$")
        if (p.size != 4 || p[0] != "pbkdf2_sha256") return false
        return try {
            val sal = Base64.getDecoder().decode(p[2])
            val esperado = Base64.getDecoder().decode(p[3])
            MessageDigest.isEqual(pbkdf2(clave, sal, p[1].toInt(), esperado.size), esperado)
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun pbkdf2(clave: String, sal: ByteArray, iteraciones: Int, bytes: Int): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(clave.toCharArray(), sal, iteraciones, bytes * 8)).encoded
}
