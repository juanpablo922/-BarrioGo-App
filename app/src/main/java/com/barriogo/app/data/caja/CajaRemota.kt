package com.barriogo.app.data.caja

import android.util.Log
import com.barriogo.app.data.auth.AuthRepository
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.data.auth.SIN_CONEXION
import com.barriogo.app.data.auth.mensajeDeError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.functions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.Date

/**
 * Caja en Firestore (docs/INTEGRACION_ANDROID.md §6 y §10 del backend).
 * - Abrir y corregir la apertura se escriben directo y funcionan sin internet (las valida firestore.rules).
 * - Cerrar y corregir el cierre pasan por las funciones cerrarCaja y corregirCierre: necesitan internet,
 *   porque los totales del comprobante salen del servidor.
 */
class CajaRemota(
    private val db: FirebaseFirestore = Firebase.firestore,
    private val funciones: FirebaseFunctions = Firebase.functions(AuthRepository.REGION),
) {
    private var escuchaTienda: ListenerRegistration? = null

    /** K0 · batch caja + tienda.cajaAbiertaId. Sin await: queda en la caché y sube cuando haya señal. */
    fun abrir(tiendaId: String, cajaId: String, fecha: String, monto: Long, perfilId: String, perfilNombre: String, abiertaEn: Date) {
        val tienda = db.document("tiendas/$tiendaId")
        db.batch().apply {
            set(
                tienda.collection("cajas").document(cajaId),
                mapOf(
                    "fechaOperacion" to fecha,
                    "montoApertura" to monto,
                    "cambiosApertura" to 0L,
                    "estado" to "abierta",
                    "abiertaPor" to perfilId,
                    "abiertaPorNombre" to perfilNombre,
                    "abiertaEn" to Timestamp(abiertaEn),
                ),
            )
            update(tienda, "cajaAbiertaId", cajaId)
        }.commit().addOnFailureListener { Log.w(TAG, "Firestore rechazó la apertura $cajaId", it) }
    }

    /** K1 · El cambio n (1..3) va junto con su registro en cambiosApertura/{n}. */
    fun corregirApertura(tiendaId: String, cajaId: String, numero: Int, anterior: Long, nuevo: Long, perfilId: String) {
        val caja = db.document("tiendas/$tiendaId/cajas/$cajaId")
        db.batch().apply {
            update(caja, mapOf("montoApertura" to nuevo, "cambiosApertura" to numero.toLong()))
            set(
                caja.collection("cambiosApertura").document(numero.toString()),
                mapOf(
                    "numero" to numero.toLong(),
                    "montoAnterior" to anterior,
                    "montoNuevo" to nuevo,
                    "perfilId" to perfilId,
                    "creadoEn" to Timestamp.now(),
                ),
            )
        }.commit().addOnFailureListener { Log.w(TAG, "Firestore rechazó el cambio $numero de la apertura", it) }
    }

    /** c2–c4 · Función cerrarCaja. */
    suspend fun cerrar(tiendaId: String, cajaId: String, perfilId: String, contado: Long, observaciones: String?): Resultado<Unit> =
        llamar("cerrarCaja", datosCierre(tiendaId, cajaId, perfilId, contado, observaciones))

    /** c4 · Función corregirCierre (máximo 2 veces). */
    suspend fun corregirCierre(tiendaId: String, cajaId: String, perfilId: String, contado: Long, observaciones: String?): Resultado<Unit> =
        llamar("corregirCierre", datosCierre(tiendaId, cajaId, perfilId, contado, observaciones))

    /** Hora de cierre de la tienda ("20:00"). Sin await: funciona sin internet. */
    fun guardarHoraCierre(tiendaId: String, hora: String) {
        db.document("tiendas/$tiendaId").update("horaCierre", hora)
            .addOnFailureListener { Log.w(TAG, "No se pudo guardar la hora de cierre", it) }
    }

    /** Copia la hora de cierre que puso otro celular de la tienda. */
    fun escucharHoraCierre(tiendaId: String, alCambiar: (String) -> Unit) {
        escuchaTienda?.remove()
        escuchaTienda = db.document("tiendas/$tiendaId").addSnapshotListener { snap, error ->
            if (error != null) return@addSnapshotListener Unit.also { Log.w(TAG, "No se pudo leer la tienda", error) }
            snap?.getString("horaCierre")?.let(alCambiar)
        }
    }

    fun detener() {
        escuchaTienda?.remove()
        escuchaTienda = null
    }

    private fun datosCierre(tiendaId: String, cajaId: String, perfilId: String, contado: Long, observaciones: String?) = mapOf(
        "tiendaId" to tiendaId,
        "cajaId" to cajaId,
        "perfilId" to perfilId,
        "efectivoContado" to contado,
        "observaciones" to observaciones?.trim()?.ifBlank { null },
    )

    private suspend fun llamar(nombre: String, datos: Map<String, Any?>): Resultado<Unit> = try {
        withTimeout(30_000) { funciones.getHttpsCallable(nombre).call(datos).await() }
        Resultado.Ok(Unit)
    } catch (e: TimeoutCancellationException) {
        Resultado.Error(SIN_CONEXION)
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseFunctionsException) {
        Resultado.Error(mensajeDeError(e), e.code.name.lowercase().replace('_', '-'))
    } catch (e: Exception) {
        Resultado.Error(mensajeDeError(e))
    }

    companion object {
        private const val TAG = "CajaRemota"
    }
}
