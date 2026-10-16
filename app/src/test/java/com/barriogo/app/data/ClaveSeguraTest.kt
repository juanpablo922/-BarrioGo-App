package com.barriogo.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaveSeguraTest {
    /** El formato que exige firestore.rules → perfilBienFormado(). */
    private val formatoDeLasReglas = Regex("pbkdf2_sha256[$][0-9]+[$][A-Za-z0-9+/=]+[$][A-Za-z0-9+/=]+")

    @Test fun formatoYVerificacion() {
        val h = ClaveSegura.hash("4821", iteraciones = 1000)
        assertTrue(formatoDeLasReglas.matches(h))
        assertTrue(h.startsWith("pbkdf2_sha256\$1000\$"))
        assertTrue(ClaveSegura.verificar("4821", h))
        assertFalse(ClaveSegura.verificar("4822", h))
        assertFalse(ClaveSegura.verificar("4821", "basura"))
        assertFalse(ClaveSegura.verificar("4821", "pbkdf2_sha256\$1000\$no-es-base64!\$x"))
    }

    @Test fun cadaHashLlevaSuPropiaSal() {
        assertFalse(ClaveSegura.hash("4821", iteraciones = 1000) == ClaveSegura.hash("4821", iteraciones = 1000))
    }

    /** Hash generado por functions/src/dominio/seguridad.ts → hashClave("4821", 1000): la app y el backend son compatibles. */
    @Test fun compatibleConElBackend() {
        assertTrue(ClaveSegura.verificar("4821", HASH_DEL_BACKEND))
        assertFalse(ClaveSegura.verificar("4822", HASH_DEL_BACKEND))
    }

    private companion object {
        const val HASH_DEL_BACKEND = "pbkdf2_sha256\$1000\$fGyVYzomEUVSH2jegG/V9A==\$a3dTkuSANmOv2r76G4eWPdVc+tCPnwnlWOw9L3F5N5Y="
    }
}
