package com.barriogo.app.data.perfiles

import android.os.Build
import android.util.Log
import com.barriogo.app.data.AppDatabase
import com.barriogo.app.data.ClaveSegura
import com.barriogo.app.data.Hasher
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.data.auth.AuthRepository
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.data.auth.mensajeDeError
import com.barriogo.app.domain.Role
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.functions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Perfiles de la tienda (L3, L4, R5–R6, P1).
 *
 * Firestore (tiendas/{t}/perfiles) es la fuente de verdad: se escucha en tiempo real y se copia a
 * Room, que es lo que leen las pantallas. Así funciona sin internet (la caché de Firestore y Room
 * conservan los perfiles) y todos los celulares de la tienda ven los mismos perfiles.
 * Los perfiles los crea la función crearPerfil del servidor, que guarda la clave con hash PBKDF2.
 */
class PerfilesRepository(
    private val room: AppDatabase,
    private val alcance: CoroutineScope,
    private val db: FirebaseFirestore = Firebase.firestore,
    private val funciones: FirebaseFunctions = Firebase.functions(AuthRepository.REGION),
) {
    private var escucha: ListenerRegistration? = null
    private var tiendaEscuchada: String? = null

    /** Empieza a copiar los perfiles activos de la tienda a Room. Llamarla varias veces no duplica. */
    fun sincronizar(tiendaId: String) {
        if (tiendaEscuchada == tiendaId && escucha != null) return
        detener()
        tiendaEscuchada = tiendaId
        escucha = db.collection("tiendas/$tiendaId/perfiles")
            .whereEqualTo("eliminadoEn", null)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "No se pudieron leer los perfiles", error)
                    return@addSnapshotListener
                }
                val docs = snap?.documents ?: return@addSnapshotListener
                alcance.launch { copiarARoom(docs) }
            }
    }

    fun detener() {
        escucha?.remove()
        escucha = null
        tiendaEscuchada = null
    }

    private suspend fun copiarARoom(docs: List<DocumentSnapshot>) {
        val dao = room.profileDao()
        for (d in docs) {
            val nombre = d.getString("nombre") ?: continue
            val claveHash = d.getString("claveHash") ?: continue
            val rol = if (d.getString("rol") == ROL_ADMIN) Role.ADMIN.name else Role.SELLER.name
            val local = dao.byRemoteId(d.id)
            if (local == null) {
                dao.insert(ProfileEntity(name = nombre, role = rol, pinHash = claveHash, pinSalt = "", remoteId = d.id))
            } else if (local.name != nombre || local.role != rol || local.pinHash != claveHash) {
                // La huella es de este celular: se conserva.
                dao.update(local.copy(name = nombre, role = rol, pinHash = claveHash, pinSalt = ""))
            }
        }
        dao.deleteAllExcept(docs.map { it.id })
    }

    /**
     * R6 / P1 · Crea el perfil en el servidor y su copia local. El primero de la tienda debe ser
     * administrador; los siguientes los crea un administrador ([actor] = su id en Firestore).
     * Devuelve el id local (Room) del perfil nuevo.
     */
    suspend fun crear(tiendaId: String, nombre: String, rol: Role, clave: String, actor: String?): Resultado<Long> = try {
        val datos = buildMap<String, Any> {
            put("tiendaId", tiendaId)
            put("nombre", nombre.trim())
            put("rol", if (rol == Role.ADMIN) ROL_ADMIN else ROL_VENDEDOR)
            put("clave", clave)
            if (actor != null) put("actorPerfilId", actor)
        }
        val r = funciones.getHttpsCallable("crearPerfil").call(datos).await().getData() as Map<*, *>
        val remoteId = r["perfilId"] as String
        // Copia local de inmediato (la escucha de Firestore la confirma después, sin duplicar).
        val hash = withContext(Dispatchers.Default) { ClaveSegura.hash(clave) }
        val dao = room.profileDao()
        val existente = dao.byRemoteId(remoteId)
        val id = existente?.id ?: dao.insert(
            ProfileEntity(name = nombre.trim(), role = rol.name, pinHash = hash, pinSalt = "", remoteId = remoteId),
        )
        Resultado.Ok(id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseFunctionsException) {
        Resultado.Error(mensajeDeError(e), e.code.name.lowercase().replace('_', '-'))
    } catch (e: Exception) {
        Resultado.Error(mensajeDeError(e))
    }

    /**
     * P2 · Cambia nombre y/o rol con la función actualizarPerfil ([actor] = id en Firestore del
     * administrador que lo hace). Solo se envía lo que cambió. Un perfil sin [ProfileEntity.remoteId]
     * (versión anterior) cambia solo en el celular.
     */
    suspend fun actualizar(tiendaId: String?, perfil: ProfileEntity, nombre: String, rol: Role, actor: String?): Resultado<Unit> =
        conServidor {
            val nombreNuevo = nombre.trim()
            val remoteId = perfil.remoteId
            if (remoteId != null && tiendaId != null) {
                val datos = buildMap<String, Any> {
                    put("tiendaId", tiendaId)
                    put("perfilId", remoteId)
                    put("actorPerfilId", actor ?: return@conServidor SIN_ADMIN)
                    if (nombreNuevo != perfil.name) put("nombre", nombreNuevo)
                    if (rol.name != perfil.role) put("rol", if (rol == Role.ADMIN) ROL_ADMIN else ROL_VENDEDOR)
                }
                if (datos.size > 3) funciones.getHttpsCallable("actualizarPerfil").call(datos).await()
            }
            room.profileDao().update(perfil.copy(name = nombreNuevo, role = rol.name))
            Resultado.Ok(Unit)
        }

    /** P2 · Borrado lógico con la función eliminarPerfil: deja de entrar, sus ventas siguen con su nombre. */
    suspend fun eliminar(tiendaId: String?, perfil: ProfileEntity, actor: String?): Resultado<Unit> = conServidor {
        val remoteId = perfil.remoteId
        if (remoteId != null && tiendaId != null) {
            val datos = mapOf("tiendaId" to tiendaId, "perfilId" to remoteId, "actorPerfilId" to (actor ?: return@conServidor SIN_ADMIN))
            funciones.getHttpsCallable("eliminarPerfil").call(datos).await()
        }
        room.profileDao().delete(perfil.id)
        Resultado.Ok(Unit)
    }

    private suspend fun <T> conServidor(bloque: suspend () -> Resultado<T>): Resultado<T> = try {
        bloque()
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseFunctionsException) {
        Resultado.Error(mensajeDeError(e), e.code.name.lowercase().replace('_', '-'))
    } catch (e: Exception) {
        Resultado.Error(mensajeDeError(e))
    }

    /**
     * Huella activada o no para este perfil en este celular. Se guarda en Room (lo usa la pantalla
     * del PIN) y, si es un perfil de Firestore, en tiendas/{t}/dispositivos/{celular}/accesos/{perfil}.
     * Funciona sin internet: Firestore sincroniza cuando vuelve la señal.
     */
    suspend fun guardarHuella(perfilId: Long, activa: Boolean, tiendaId: String?, dispositivoId: String) {
        val dao = room.profileDao()
        val perfil = dao.byId(perfilId) ?: return
        dao.update(perfil.copy(biometric = activa))
        val remoteId = perfil.remoteId ?: return
        if (tiendaId == null) return
        val dispositivo = db.document("tiendas/$tiendaId/dispositivos/$dispositivoId")
        val batch = db.batch()
        batch.set(
            dispositivo,
            mapOf(
                "identificador" to dispositivoId,
                "modelo" to "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(120),
                "sesionActiva" to true,
                "ultimaSincronizacion" to Timestamp.now(),
            ),
            SetOptions.merge(),
        )
        batch.set(
            dispositivo.collection("accesos").document(remoteId),
            mapOf("huellaActiva" to activa, "intentosFallidos" to 0L),
            SetOptions.merge(),
        )
        batch.commit().addOnFailureListener { Log.w(TAG, "No se pudo respaldar la huella", it) }
    }

    companion object {
        private const val TAG = "PerfilesRepository"
        private const val ROL_ADMIN = "administrador"
        private const val ROL_VENDEDOR = "vendedor"
        private val SIN_ADMIN = Resultado.Error("Primero entra con la clave de un administrador.", "permission-denied")

        /**
         * L4 · ¿La clave corresponde al perfil? Sin internet.
         * Perfiles de Firestore y nuevos: PBKDF2 (ClaveSegura). Perfiles locales antiguos: Hasher.
         * Un perfil sin clave guardada nunca entra.
         */
        suspend fun claveCorrecta(perfil: ProfileEntity, clave: String): Boolean = withContext(Dispatchers.Default) {
            when {
                perfil.pinHash.startsWith("pbkdf2_sha256$") -> ClaveSegura.verificar(clave, perfil.pinHash)
                perfil.pinHash.isNotBlank() && perfil.pinSalt.isNotBlank() -> Hasher.matches(perfil.pinSalt, clave, perfil.pinHash)
                else -> false
            }
        }
    }
}
