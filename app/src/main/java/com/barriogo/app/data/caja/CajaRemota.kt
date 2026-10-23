package com.barriogo.app.data.caja

import android.util.Log
import com.barriogo.app.data.auth.AuthRepository
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.data.auth.SIN_CONEXION
import com.barriogo.app.data.auth.mensajeDeError
import com.barriogo.app.domain.ResultadoArqueo
import com.barriogo.app.domain.TotalesCaja
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.functions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.Date

/**
 * Caja en Firestore (docs/INTEGRACION_ANDROID.md §6 y §10 del backend). Es la fuente de la caja:
 * las pantallas escuchan tiendas/{t}/cajas en tiempo real. Sin internet, Firestore responde con su
 * caché y deja las escrituras pendientes hasta que vuelva la señal.
 * - Abrir, corregir la apertura y el arqueo parcial: escritura directa (las valida firestore.rules).
 * - Cerrar y corregir el cierre: funciones cerrarCaja y corregirCierre (necesitan internet).
 */
class CajaRemota(
    private val db: FirebaseFirestore = Firebase.firestore,
    private val funciones: FirebaseFunctions = Firebase.functions(AuthRepository.REGION),
) {
    private var escuchaTienda: ListenerRegistration? = null

    /** Últimas cajas de la tienda (la más nueva primero). Con eso se sabe la abierta y la última cerrada. */
    private fun ultimasCajas(tiendaId: String): Query =
        db.collection("tiendas/$tiendaId/cajas").orderBy("fechaOperacion", Query.Direction.DESCENDING).limit(LIMITE_CAJAS)

    fun cajas(tiendaId: String): Flow<List<Caja>> = callbackFlow {
        val escucha = ultimasCajas(tiendaId).addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
            if (error != null) {
                Log.w(TAG, "No se pudieron leer las cajas", error)
                return@addSnapshotListener
            }
            trySend(snap?.documents.orEmpty().mapNotNull(::aCaja))
        }
        awaitClose { escucha.remove() }
    }

    /** Lectura única (al entrar con la clave y en los avisos). Sin internet, usa la caché. */
    suspend fun cajasUnaVez(tiendaId: String): List<Caja> {
        val consulta = ultimasCajas(tiendaId)
        val snap = try {
            withTimeout(8_000) { consulta.get().await() }
        } catch (e: TimeoutCancellationException) {
            runCatching { consulta.get(Source.CACHE).await() }.getOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching { consulta.get(Source.CACHE).await() }.getOrNull()
        }
        return snap?.documents.orEmpty().mapNotNull(::aCaja)
    }

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

    /** a1 · Arqueo parcial (no cierra la caja). */
    fun guardarArqueo(
        tiendaId: String, cajaId: String, esperado: Long, contado: Long, resultado: ResultadoArqueo,
        observaciones: String, perfilId: String, perfilNombre: String,
    ) {
        db.collection("tiendas/$tiendaId/cajas/$cajaId/arqueos").document().set(
            mapOf(
                "tipo" to "parcial",
                "saldoEsperado" to esperado,
                "efectivoContado" to contado,
                "diferencia" to contado - esperado,
                "resultado" to resultado.name.lowercase(),
                "observaciones" to observaciones.trim().ifBlank { null },
                "perfilId" to perfilId,
                "perfilNombre" to perfilNombre,
                "realizadoEn" to Timestamp.now(),
            ),
        ).addOnFailureListener { Log.w(TAG, "Firestore rechazó el arqueo", it) }
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
        /** Con una caja por día, 10 alcanzan para ver la abierta (aunque sea de días atrás) y la última cerrada. */
        private const val LIMITE_CAJAS = 10L

        /** Documento de Firestore → Caja. null si le faltan los campos básicos. */
        fun aCaja(d: DocumentSnapshot): Caja? {
            val fecha = d.getString("fechaOperacion") ?: return null
            val apertura = d.getLong("montoApertura") ?: return null
            @Suppress("UNCHECKED_CAST")
            val cierre = d.get("cierre") as? Map<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val totales = (cierre?.get("totales") as? Map<String, Any?>)?.let { t -> totales(apertura, t) }
            return Caja(
                id = d.id,
                operationDate = fecha,
                openingAmount = apertura,
                editsUsed = d.getLong("cambiosApertura")?.toInt() ?: 0,
                openedBy = d.getString("abiertaPorNombre").orEmpty(),
                openedAt = d.getTimestamp("abiertaEn")?.toDate()?.time ?: 0L,
                isOpen = d.getString("estado") == "abierta",
                closedAt = d.getTimestamp("cerradaEn")?.toDate()?.time,
                closedBy = d.getString("cerradaPorNombre"),
                countedAmount = (cierre?.get("efectivoContado") as? Number)?.toLong(),
                expectedAtClose = totales?.saldoEsperado,
                closeNote = cierre?.get("observaciones") as? String ?: "",
                closeEditsUsed = d.getLong("cambiosCierre")?.toInt() ?: 0,
                totalesCierre = totales,
                sinSincronizar = d.metadata.hasPendingWrites(),
            )
        }

        private fun totales(apertura: Long, t: Map<String, Any?>): TotalesCaja {
            fun n(campo: String) = (t[campo] as? Number)?.toLong() ?: 0L
            return TotalesCaja(
                apertura = apertura, ventas = n("ventas"), ventasEfectivo = n("ventasEfectivo"), gastos = n("gastos"),
                fiados = n("fiados"), abonos = n("abonos"), abonosEfectivo = n("abonosEfectivo"), ajustes = n("ajustes"),
                entradas = n("entradas"), salidas = n("salidas"), saldoEsperado = n("saldoEsperado"),
            )
        }
    }
}
