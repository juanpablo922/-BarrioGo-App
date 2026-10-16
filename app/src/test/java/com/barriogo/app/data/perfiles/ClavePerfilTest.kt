package com.barriogo.app.data.perfiles

import com.barriogo.app.data.ClaveSegura
import com.barriogo.app.data.Hasher
import com.barriogo.app.data.ProfileEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** L4 · Ingreso con clave de 4 números: sin comodines ni perfiles sin clave. */
class ClavePerfilTest {
    private fun perfil(pinHash: String, pinSalt: String = "") =
        ProfileEntity(id = 1, name = "Don Juan", role = "ADMIN", pinHash = pinHash, pinSalt = pinSalt)

    @Test fun perfilDeFirestoreConPbkdf2() = runBlocking {
        val p = perfil(ClaveSegura.hash("4821", iteraciones = 1000))
        assertTrue(PerfilesRepository.claveCorrecta(p, "4821"))
        assertFalse(PerfilesRepository.claveCorrecta(p, "4822"))
    }

    @Test fun perfilDemoConElHashAnterior() = runBlocking {
        val sal = Hasher.newSalt()
        val p = perfil(Hasher.hash(sal, "1111"), sal)
        assertTrue(PerfilesRepository.claveCorrecta(p, "1111"))
        assertFalse(PerfilesRepository.claveCorrecta(p, "2222"))
    }

    @Test fun ya1234NoEsComodin() = runBlocking {
        val p = perfil(ClaveSegura.hash("4821", iteraciones = 1000))
        assertFalse(PerfilesRepository.claveCorrecta(p, "1234"))
    }

    @Test fun unPerfilSinClaveNuncaEntra() = runBlocking {
        assertFalse(PerfilesRepository.claveCorrecta(perfil(""), "1234"))
        assertFalse(PerfilesRepository.claveCorrecta(perfil(""), "0000"))
        assertFalse(PerfilesRepository.claveCorrecta(perfil("basura"), "1234"))
    }
}
