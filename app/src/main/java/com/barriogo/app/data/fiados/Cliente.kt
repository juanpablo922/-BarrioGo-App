package com.barriogo.app.data.fiados

import com.barriogo.app.domain.Fiados

/**
 * Cliente de fiados tal como está en Firestore (tiendas/{t}/clientes/{remoteId}). Es lo que leen
 * las pantallas: ya no hay copia de los clientes en Room. Entre paréntesis, el campo de Firestore.
 */
data class Cliente(
    /** Id local estable para la navegación y para MovementEntity.clientId (ver [idLocal]). */
    val id: Long,
    /** Id del documento en Firestore. */
    val remoteId: String,
    /** Nombre (nombre). */
    val name: String,
    /** Celular, solo dígitos (celular). */
    val phone: String = "",
    /** Dirección opcional (direccion). */
    val address: String = "",
    /** Lo máximo que puede deber (cupoMaximo). Nace en $ 50.000; solo un administrador lo sube. */
    val creditLimit: Long = Fiados.CUPO_POR_DEFECTO,
    /** El servidor vio que debe más que su cupo (cupoExcedido), p. ej. dos celulares sin internet. */
    val overLimit: Boolean = false,
) {
    companion object {
        /**
         * Id local para un cliente de Firestore. Es positivo (las rutas de Fiados usan ids > 0) y
         * siempre el mismo para el mismo documento, igual que MovimientosRemotos.idLocal().
         */
        fun idLocal(remoteId: String): Long = (remoteId.hashCode().toLong() and 0x7fffffff) + 1
    }
}
