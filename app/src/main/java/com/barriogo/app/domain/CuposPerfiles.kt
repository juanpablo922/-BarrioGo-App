package com.barriogo.app.domain

/**
 * Cupos de perfiles por tienda (P1, P2). Misma regla que functions/src/dominio/perfiles.ts del
 * backend: siempre queda al menos 1 administrador; como máximo 2 administradores y 5 perfiles.
 * La app la revisa antes de llamar al servidor (y en la cuenta demo, que no tiene servidor).
 */
object CuposPerfiles {
    const val MAXIMO_PERFILES = 5
    const val MAXIMO_ADMINISTRADORES = 2

    const val MENSAJE_PRIMER_PERFIL = "El primer perfil de la tienda debe ser administrador."
    const val MENSAJE_UNICO_ADMIN =
        "Es el único administrador. Para quitarlo, primero nombra a otra persona administradora."
    const val MENSAJE_MAXIMO_PERFILES =
        "La tienda ya tiene $MAXIMO_PERFILES perfiles, el máximo. Elimina uno para agregar otro."
    const val MENSAJE_MAXIMO_ADMINISTRADORES =
        "Ya hay $MAXIMO_ADMINISTRADORES administradores, el máximo. Elige el rol vendedor."

    data class PerfilConRol(val id: Long, val rol: Role)

    sealed interface Cambio {
        data class Crear(val rol: Role) : Cambio
        data class CambiarRol(val perfilId: Long, val rol: Role) : Cambio
        data class Eliminar(val perfilId: Long) : Cambio
    }

    /**
     * Problema que deja el cambio sobre los perfiles activos, o null si cabe.
     * Solo se rechaza lo que empeora un cupo: una tienda que ya lo pasaba todavía puede bajar.
     */
    fun problema(activos: List<PerfilConRol>, cambio: Cambio): String? {
        val despues = when (cambio) {
            is Cambio.Crear -> activos + PerfilConRol(0, cambio.rol)
            is Cambio.CambiarRol -> activos.map { if (it.id == cambio.perfilId) it.copy(rol = cambio.rol) else it }
            is Cambio.Eliminar -> activos.filter { it.id != cambio.perfilId }
        }
        val adminsAntes = activos.count { it.rol == Role.ADMIN }
        val adminsDespues = despues.count { it.rol == Role.ADMIN }
        return when {
            adminsDespues == 0 -> if (activos.isEmpty()) MENSAJE_PRIMER_PERFIL else MENSAJE_UNICO_ADMIN
            despues.size > MAXIMO_PERFILES && despues.size > activos.size -> MENSAJE_MAXIMO_PERFILES
            adminsDespues > MAXIMO_ADMINISTRADORES && adminsDespues > adminsAntes -> MENSAJE_MAXIMO_ADMINISTRADORES
            else -> null
        }
    }
}
