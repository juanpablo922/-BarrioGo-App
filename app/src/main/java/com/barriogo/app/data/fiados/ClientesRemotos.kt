package com.barriogo.app.data.fiados

import android.util.Log
import com.barriogo.app.domain.Fiados
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.Normalizer

/**
 * Clientes de fiados en Firestore (tiendas/{t}/clientes), con los campos que exige
 * firestore.rules → clienteValido(). Se escribe sin await: queda en la caché de Firestore y sube
 * cuando vuelve la señal. Los saldos y cupoExcedido los calcula el servidor; aquí no se escriben.
 */
class ClientesRemotos(private val db: FirebaseFirestore = Firebase.firestore) {

    /** Clientes de la tienda en tiempo real, por nombre. */
    fun clientes(tiendaId: String): Flow<List<Cliente>> = callbackFlow {
        val escucha = db.collection("tiendas/$tiendaId/clientes").addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "No se pudieron leer los clientes", error)
                return@addSnapshotListener
            }
            trySend(snap?.documents.orEmpty().mapNotNull(::aCliente).sortedBy { busqueda(it.name) })
        }
        awaitClose { escucha.remove() }
    }

    /** Id nuevo para crear el cliente sin esperar a Firestore. */
    fun nuevoId(tiendaId: String): String = db.collection("tiendas/$tiendaId/clientes").document().id

    /** "+ Agregar cliente": nace con el cupo por defecto (firestore.rules → cupoFiado()). */
    fun crear(tiendaId: String, c: Cliente) {
        db.document("tiendas/$tiendaId/clientes/${c.remoteId}").set(
            mapOf(
                "nombre" to c.name,
                "nombreBusqueda" to busqueda(c.name),
                "celular" to c.phone,
                "direccion" to c.address.ifBlank { null },
                "cupoMaximo" to Fiados.CUPO_POR_DEFECTO,
                "creadoEn" to Timestamp.now(),
            ),
        ).addOnFailureListener { Log.w(TAG, "Firestore rechazó el cliente ${c.remoteId}", it) }
    }

    /** Editar datos del cliente: nombre, celular y dirección. */
    fun editar(tiendaId: String, c: Cliente) {
        db.document("tiendas/$tiendaId/clientes/${c.remoteId}").update(
            mapOf(
                "nombre" to c.name,
                "nombreBusqueda" to busqueda(c.name),
                "celular" to c.phone,
                "direccion" to c.address.ifBlank { null },
                "actualizadoEn" to Timestamp.now(),
            ),
        ).addOnFailureListener { Log.w(TAG, "Firestore rechazó la edición del cliente ${c.remoteId}", it) }
    }

    /** Subir el cupo (solo administrador; el rol se revisa en FiadosRepository). */
    fun cambiarCupo(tiendaId: String, remoteId: String, cupo: Long, perfilId: String) {
        db.document("tiendas/$tiendaId/clientes/$remoteId").update(
            mapOf(
                "cupoMaximo" to cupo,
                "cupoActualizadoPor" to perfilId,
                "cupoActualizadoEn" to Timestamp.now(),
            ),
        ).addOnFailureListener { Log.w(TAG, "Firestore rechazó el cupo de $remoteId", it) }
    }

    companion object {
        private const val TAG = "ClientesRemotos"

        /** "marta ramirez": sin tildes ni mayúsculas, para buscar. */
        fun busqueda(nombre: String): String =
            Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()
                .replace(Regex("\\s+"), " ").take(120)

        /** Documento de Firestore → Cliente. null si está incompleto. */
        fun aCliente(d: DocumentSnapshot): Cliente? {
            val nombre = d.getString("nombre") ?: return null
            return Cliente(
                id = Cliente.idLocal(d.id),
                remoteId = d.id,
                name = nombre,
                phone = d.getString("celular").orEmpty(),
                address = d.getString("direccion").orEmpty(),
                creditLimit = d.getLong("cupoMaximo") ?: Fiados.CUPO_POR_DEFECTO,
                overLimit = d.getBoolean("cupoExcedido") == true,
            )
        }
    }
}
