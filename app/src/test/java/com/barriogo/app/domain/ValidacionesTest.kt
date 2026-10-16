package com.barriogo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mismos casos que functions/test/dominio.test.ts del backend (seguridad) y que firestore.rules. */
class ValidacionesTest {
    @Test fun correo() {
        assertTrue(Validaciones.correoValido("  Juan.Perez@Gmail.com "))
        assertEquals("juan.perez@gmail.com", Validaciones.normalizarCorreo("  Juan.Perez@Gmail.com "))
        assertFalse(Validaciones.correoValido("juan@"))
        assertFalse(Validaciones.correoValido("juan perez@gmail.com"))
        assertFalse(Validaciones.correoValido(""))
    }

    @Test fun contrasenaComoR3() {
        assertEquals(
            listOf("Al menos 8 caracteres", "Una letra mayúscula", "Un número o carácter especial"),
            Validaciones.problemasContrasena("corta"),
        )
        assertTrue(Validaciones.problemasContrasena("Tiendita2026").isEmpty())
        assertTrue(Validaciones.problemasContrasena("Tiendita!").isEmpty())
        assertTrue(Validaciones.problemasContrasena("Ñandúes2026").isEmpty())
        assertEquals(listOf("Una letra mayúscula"), Validaciones.problemasContrasena("tiendita2026"))
        assertEquals(listOf("Un número o carácter especial"), Validaciones.problemasContrasena("Tienditas"))
        assertEquals(listOf("Máximo 100 caracteres"), Validaciones.problemasContrasena("A1" + "x".repeat(99)))
        assertTrue(Validaciones.problemasContrasena("A1" + "x".repeat(98)).isEmpty())
    }

    @Test fun celular() {
        assertTrue(Validaciones.celularValido("300 123 4567"))
        assertEquals("3001234567", Validaciones.normalizarCelular("300 123 4567"))
        assertFalse(Validaciones.celularValido("6012345678"))   // fijo de Bogotá
        assertFalse(Validaciones.celularValido("300123456"))    // 9 números
    }

    @Test fun textos() {
        assertTrue(Validaciones.textoValido(" Calle 10 # 5-20 ", 200))
        assertFalse(Validaciones.textoValido("   ", 200))
        assertFalse(Validaciones.textoValido("x".repeat(61), 60))
    }

    @Test fun codigosYClaves() {
        assertTrue(Validaciones.codigoValido("048213"))
        assertFalse(Validaciones.codigoValido("48213"))
        assertFalse(Validaciones.codigoValido("04821a"))
        assertTrue(Validaciones.claveValida("4821"))
        assertFalse(Validaciones.claveValida("482"))
    }

    @Test fun categoriaParaFirestore() {
        assertEquals("abarrotes" to null, Validaciones.categoriaFirestore("Abarrotes"))
        assertEquals("panaderia" to null, Validaciones.categoriaFirestore("Panadería"))
        assertEquals("otros" to "Papelería", Validaciones.categoriaFirestore(" Papelería "))
        assertEquals(null, Validaciones.problemaCategoria("Abarrotes"))
        assertEquals(null, Validaciones.problemaCategoria("Papelería"))
        assertEquals("Selecciona una categoría para continuar", Validaciones.problemaCategoria(""))
        assertEquals("Escribe de qué es tu negocio", Validaciones.problemaCategoria("Otros"))
        assertEquals("Escribe de qué es tu negocio en máximo 60 letras", Validaciones.problemaCategoria("x".repeat(61)))
    }

    @Test fun todasLasCategoriasTienenId() {
        BUSINESS_CATEGORIES.forEach { assertTrue("Falta el id de $it", Validaciones.CATEGORIA_ID.containsKey(it)) }
        assertEquals("otros", Validaciones.CATEGORIA_ID[Validaciones.CATEGORIA_OTROS])
    }
}
