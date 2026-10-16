package com.barriogo.app.domain

import com.barriogo.app.domain.CuposPerfiles.Cambio
import com.barriogo.app.domain.CuposPerfiles.PerfilConRol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Mismos casos que "cupos de perfiles" en functions/test/dominio.test.ts del backend. */
class CuposPerfilesTest {
    private fun admin(id: Long) = PerfilConRol(id, Role.ADMIN)
    private fun vendedor(id: Long) = PerfilConRol(id, Role.SELLER)

    @Test fun primerPerfilEsAdministrador() {
        assertEquals(CuposPerfiles.MENSAJE_PRIMER_PERFIL, CuposPerfiles.problema(emptyList(), Cambio.Crear(Role.SELLER)))
        assertNull(CuposPerfiles.problema(emptyList(), Cambio.Crear(Role.ADMIN)))
    }

    @Test fun maximoDosAdministradores() {
        assertNull(CuposPerfiles.problema(listOf(admin(1)), Cambio.Crear(Role.ADMIN)))
        assertEquals(
            CuposPerfiles.MENSAJE_MAXIMO_ADMINISTRADORES,
            CuposPerfiles.problema(listOf(admin(1), admin(2)), Cambio.Crear(Role.ADMIN)),
        )
        assertEquals(
            CuposPerfiles.MENSAJE_MAXIMO_ADMINISTRADORES,
            CuposPerfiles.problema(listOf(admin(1), admin(2), vendedor(3)), Cambio.CambiarRol(3, Role.ADMIN)),
        )
        assertNull(CuposPerfiles.problema(listOf(admin(1), admin(2)), Cambio.CambiarRol(1, Role.ADMIN)))
    }

    @Test fun maximoCincoPerfiles() {
        val cuatro = listOf(admin(1), vendedor(2), vendedor(3), vendedor(4))
        assertNull(CuposPerfiles.problema(cuatro, Cambio.Crear(Role.SELLER)))
        assertEquals(
            CuposPerfiles.MENSAJE_MAXIMO_PERFILES,
            CuposPerfiles.problema(cuatro + vendedor(5), Cambio.Crear(Role.SELLER)),
        )
    }

    @Test fun siempreQuedaUnAdministrador() {
        assertEquals(CuposPerfiles.MENSAJE_UNICO_ADMIN, CuposPerfiles.problema(listOf(admin(1), vendedor(2)), Cambio.Eliminar(1)))
        assertEquals(
            CuposPerfiles.MENSAJE_UNICO_ADMIN,
            CuposPerfiles.problema(listOf(admin(1), vendedor(2)), Cambio.CambiarRol(1, Role.SELLER)),
        )
        assertNull(CuposPerfiles.problema(listOf(admin(1), admin(2)), Cambio.Eliminar(1)))
        assertNull(CuposPerfiles.problema(listOf(admin(1), vendedor(2)), Cambio.Eliminar(2)))
    }

    @Test fun unaTiendaQueYaPasabaUnCupoPuedeBajar() {
        val tres = listOf(admin(1), admin(2), admin(3))
        assertNull(CuposPerfiles.problema(tres, Cambio.Eliminar(3)))
        assertNull(CuposPerfiles.problema(tres, Cambio.CambiarRol(3, Role.SELLER)))
    }
}
