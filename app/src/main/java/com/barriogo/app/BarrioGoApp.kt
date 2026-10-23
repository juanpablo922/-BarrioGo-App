package com.barriogo.app

import android.app.Application
import android.content.Context
import com.barriogo.app.data.AppDatabase
import com.barriogo.app.data.BarrioRepository
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.data.SessionStore
import com.barriogo.app.data.auth.AuthRepository
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.data.caja.CajaRepository
import com.barriogo.app.data.movimientos.GastosRepository
import com.barriogo.app.data.movimientos.MovimientosRemotos
import com.barriogo.app.data.perfiles.PerfilesRepository
import com.barriogo.app.notificaciones.AvisosCierre
import kotlinx.coroutines.SupervisorJob
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.ReglasGasto
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.functions.functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** Contenedor de dependencias simple (sin Hilt) para mantener el proyecto fácil de compilar. */
class AppContainer(context: Context) {
    val db = AppDatabase.build(context)
    val store = SessionStore(context)

    /** Cuenta del dueño (Firebase Auth) y registro de la tienda en Firestore. */
    val authRepo = AuthRepository()

    /** Para trabajo que no depende de una pantalla (escuchas de Firestore, copiar perfiles a Room). */
    val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Perfiles de la tienda: Firestore es la fuente de verdad, Room la copia que leen las pantallas. */
    val perfiles = PerfilesRepository(db, alcance)

    private val appContext = context.applicationContext

    /** Ventas y gastos en Firestore (lectura en tiempo real y escrituras). */
    val movimientosRemotos = MovimientosRemotos()

    /** Caja diaria leída de Firestore. Cada cambio vuelve a programar los avisos de cierre. */
    val caja = CajaRepository(db, store, alcance, remotos = movimientosRemotos, alCambiarCaja = { AvisosCierre.reprogramar(appContext) })

    /** Gastos leídos y escritos en Firestore: registrar a mano, editar y anular. */
    val gastos = GastosRepository(store, caja, alcance, movimientosRemotos)

    /** Perfiles, clientes y el libro (ventas, fiados y abonos de Room + gastos de Firestore). */
    val repo = BarrioRepository(db, store.cuenta, gastos.gastos)

    init {
        // Al entrar con otra cuenta cambia la caja que se mira: los avisos de cierre se recalculan para ella.
        alcance.launch { store.cuenta.drop(1).collect { AvisosCierre.reprogramar(appContext) } }
    }

    /** Perfil que atiende ahora, leído de Room. */
    suspend fun perfilActivo(): ProfileEntity? = activeProfileId.value?.let { repo.profile(it) }

    /** (tiendaId, perfilId en Firestore) del perfil, o null si todavía no llegó de Firestore. */
    fun enLaNube(perfil: ProfileEntity?): Pair<String, String>? {
        val tienda = store.tiendaId?.takeIf { it.isNotBlank() } ?: return null
        val remoto = perfil?.remoteId ?: return null
        return tienda to remoto
    }

    /**
     * Registra un movimiento en la caja abierta (M1, voz, escáner, fiados):
     * - Gasto: va a Firestore (GastosRepository), que es donde se leen los gastos.
     * - Venta, fiado, abono: van a Room unidos a la caja de Firestore; las ventas además se copian a Firestore.
     */
    suspend fun registrarMovimiento(m: MovementEntity): Resultado<Unit> {
        val perfil = perfilActivo() ?: return Resultado.Error("Vuelve a entrar con tu clave para continuar.")
        if (m.type == MovementType.GASTO.name) {
            return when (val r = gastos.registrar(ReglasGasto.tipoDe(m.category, m.detail), m.amount, m.title, perfil)) {
                is Resultado.Ok -> Resultado.Ok(Unit)
                is Resultado.Error -> r
            }
        }
        val abierta = caja.cajaAbiertaUnaVez() ?: return Resultado.Error("Primero abre la caja del día.", "failed-precondition")
        val nube = enLaNube(perfil)
        val tipo = MovementType.valueOf(m.type)
        val remoteId = if (nube != null && movimientosRemotos.sincroniza(tipo)) movimientosRemotos.nuevoId(nube.first) else null
        val nuevo = m.copy(id = 0, cajaId = abierta.id, remoteId = remoteId, profileId = perfil.id, profileName = perfil.name)
        val guardado = nuevo.copy(id = repo.insertMovement(nuevo))
        if (nube != null && remoteId != null) movimientosRemotos.crear(nube.first, remoteId, guardado, abierta.id, nube.second)
        return Resultado.Ok(Unit)
    }

    /** R5 → R6: la clave de 4 números que se acaba de escribir, mientras se llena "Crea tu perfil". */
    var claveEnCurso: String? = null

    /**
     * Último administrador que entró con su clave en este celular y la hora.
     * Durante 10 minutos puede crear, editar y eliminar perfiles aunque se vuelva al selector.
     */
    private var adminVerificado: Pair<ProfileEntity, Long>? = null

    fun recordarAdmin(perfil: ProfileEntity) {
        if (perfil.isAdmin) adminVerificado = perfil to System.currentTimeMillis()
    }

    fun olvidarAdmin() {
        adminVerificado = null
    }

    /** Administrador verificado hace menos de 10 minutos, o null. */
    fun adminVigente(): ProfileEntity? =
        adminVerificado?.takeIf { System.currentTimeMillis() - it.second < 10 * 60 * 1000 }?.first

    /** Id en Firestore del administrador vigente (lo que piden las funciones como actorPerfilId). */
    fun adminParaCrearPerfiles(): String? = adminVigente()?.remoteId

    /** Empieza a sincronizar los perfiles y la hora de cierre de la tienda. */
    fun sincronizarPerfiles() {
        val tienda = store.tiendaId
        if (tienda != null && authRepo.correoActual != null) {
            perfiles.sincronizar(tienda)
            caja.sincronizar()
        }
    }

    /** Perfil que está atendiendo ahora. Se pierde al cerrar la app: siempre se pide la clave de nuevo. */
    val activeProfileId = MutableStateFlow<Long?>(null)

    /** L2 · Ingreso con Firebase Auth (necesita internet la primera vez en cada celular). Devuelve el error o null. */
    suspend fun authenticateStore(email: String, pass: String): String? =
        when (val r = authRepo.ingresar(email, pass)) {
            is Resultado.Ok -> null
            is Resultado.Error -> r.mensaje
        }

    /** "Salir de la tienda": cierra la sesión de Firebase. */
    fun signOutOfStore() {
        activeProfileId.value = null
        olvidarAdmin()
        store.signedIn = false
        perfiles.detener()
        caja.detener()
        authRepo.salir()
    }
}

class BarrioGoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Antes de cualquier uso de Firestore: caché persistente para trabajar sin internet.
        Firebase.firestore.firestoreSettings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings { setSizeBytes(200L * 1024 * 1024) })
        }
        if (BuildConfig.USAR_EMULADORES) {             // 10.0.2.2 = el PC visto desde el emulador de Android
            Firebase.auth.useEmulator("10.0.2.2", 9099)
            Firebase.firestore.useEmulator("10.0.2.2", 8080)
            Firebase.functions(AuthRepository.REGION).useEmulator("10.0.2.2", 5001)
        }
        container = AppContainer(this)
        AvisosCierre.crearCanal(this)
        AvisosCierre.reprogramar(this)
    }
}
