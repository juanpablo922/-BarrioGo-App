package com.barriogo.app

import android.app.Application
import android.content.Context
import com.barriogo.app.data.AppDatabase
import com.barriogo.app.data.BarrioRepository
import com.barriogo.app.data.ClientEntity
import com.barriogo.app.data.Hasher
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
import com.barriogo.app.domain.PayMethod
import com.barriogo.app.domain.Role
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.functions.functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Contenedor de dependencias simple (sin Hilt) para mantener el proyecto fácil de compilar. */
class AppContainer(context: Context) {
    val db = AppDatabase.build(context)
    val repo = BarrioRepository(db)
    val store = SessionStore(context)

    /** Cuenta del dueño (Firebase Auth) y registro de la tienda en Firestore. */
    val authRepo = AuthRepository()

    /** Para trabajo que no depende de una pantalla (copiar perfiles de Firestore a Room). */
    val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Perfiles de la tienda: Firestore es la fuente de verdad, Room la copia que leen las pantallas. */
    val perfiles = PerfilesRepository(db, alcance)

    private val appContext = context.applicationContext

    /** Caja diaria (apertura, correcciones y cierre). Cada cambio vuelve a programar los avisos de cierre. */
    val caja = CajaRepository(db, store, alCambiarCaja = { AvisosCierre.reprogramar(appContext) })

    /** Gastos: registrar a mano, editar y anular. */
    val gastos = GastosRepository(db, store)

    /** Copia a Firestore las ventas registradas a mano (los gastos los copia [gastos]). */
    val movimientosRemotos = MovimientosRemotos()

    /** Perfil que atiende ahora, leído de Room. */
    suspend fun perfilActivo(): ProfileEntity? = activeProfileId.value?.let { repo.profile(it) }

    /** (tiendaId, perfilId en Firestore) si lo que haga [perfil] se copia a Firestore; null en la demo. */
    fun enLaNube(perfil: ProfileEntity?): Pair<String, String>? {
        val tienda = store.tiendaId ?: return null
        val remoto = perfil?.remoteId ?: return null
        return if (store.demo) null else tienda to remoto
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

    /** Empieza a sincronizar los perfiles de la tienda (cuentas reales; la demo es solo local). */
    fun sincronizarPerfiles() {
        val tienda = store.tiendaId
        if (!store.demo && tienda != null && authRepo.correoActual != null) {
            perfiles.sincronizar(tienda)
            caja.sincronizar()
        }
    }

    /** Perfil que está atendiendo ahora. Se pierde al cerrar la app: siempre se pide la clave de nuevo. */
    val activeProfileId = MutableStateFlow<Long?>(null)

    /**
     * L2 · Ingreso. Devuelve null si entra, o el mensaje de error para mostrar.
     * - Cuenta demo (tienda@barriogo.com / 123456): datos de ejemplo, solo en el celular.
     * - Cualquier otra: Firebase Auth (necesita internet la primera vez en cada celular).
     */
    suspend fun authenticateStore(email: String, pass: String): String? {
        if (email.trim().equals(DEMO_CORREO, ignoreCase = true) && pass == DEMO_CONTRASENA) {
            authRepo.salir()                 // la demo no se mezcla con una cuenta real
            seedDemoData()
            store.demo = true
            return null
        }
        return when (val r = authRepo.ingresar(email, pass)) {
            is Resultado.Ok -> {
                store.demo = false
                null
            }
            is Resultado.Error -> r.mensaje
        }
    }

    /** "Salir de la tienda": cierra la sesión de Firebase y la de la demo. */
    fun signOutOfStore() {
        activeProfileId.value = null
        olvidarAdmin()
        store.signedIn = false
        perfiles.detener()
        caja.detener()
        authRepo.salir()
    }

    companion object {
        const val DEMO_CORREO = "tienda@barriogo.com"
        const val DEMO_CONTRASENA = "123456"
    }

    suspend fun seedDemoData() {
        if (db.profileDao().count() == 0) {
            // Configurar datos de la tienda demo
            if (!store.registered) {
                store.storeName = "Tienda El Vecino"
                store.ownerName = "Pedro Gómez"
                store.email = "tienda@barriogo.com"
                store.phone = "3101234567"
                store.category = "Víveres y Abarrotes"
                store.setPassword("123456")
                store.registered = true
            }

            // Crear perfil Administrador (PIN 1234)
            val adminSalt = Hasher.newSalt()
            val adminId = db.profileDao().insert(
                ProfileEntity(
                    name = "Pedro Gómez",
                    role = Role.ADMIN.name,
                    pinHash = Hasher.hash(adminSalt, "1234"),
                    pinSalt = adminSalt,
                    biometric = true,
                )
            )

            // Crear perfil Vendedor (PIN 1111)
            val vendSalt = Hasher.newSalt()
            db.profileDao().insert(
                ProfileEntity(
                    name = "Ana Martínez",
                    role = Role.SELLER.name,
                    pinHash = Hasher.hash(vendSalt, "1111"),
                    pinSalt = vendSalt,
                    biometric = false,
                )
            )

            // Clientes frecuentes
            val c1 = db.clientDao().insert(ClientEntity(name = "Marta Ramírez", phone = "3109876543"))
            db.clientDao().insert(ClientEntity(name = "Jorge Martínez", phone = "3114567890"))
            db.clientDao().insert(ClientEntity(name = "Luis Rojas", phone = "3156789012"))
            db.clientDao().insert(ClientEntity(name = "Pedro Gómez", phone = "3123456789"))
            db.clientDao().insert(ClientEntity(name = "Rosa Hernández", phone = "3167890123"))

            // Apertura de caja del día
            val session = repo.openCash(150_000L, "Pedro Gómez")

            // Movimientos iniciales de ejemplo
            val now = System.currentTimeMillis()
            repo.insertMovement(
                MovementEntity(
                    type = MovementType.VENTA.name,
                    amount = 8_000L,
                    title = "2 Gaseosas",
                    detail = "Venta en efectivo",
                    quantity = 2,
                    payMethod = PayMethod.EFECTIVO.name,
                    profileId = adminId,
                    profileName = "Pedro Gómez",
                    sessionId = session.id,
                    createdAt = now - 3600_000L,
                )
            )
            repo.insertMovement(
                MovementEntity(
                    type = MovementType.VENTA.name,
                    amount = 12_000L,
                    title = "Pan y leche",
                    detail = "Venta Nequi/Transferencia",
                    quantity = 2,
                    payMethod = PayMethod.TRANSFERENCIA.name,
                    profileId = adminId,
                    profileName = "Pedro Gómez",
                    sessionId = session.id,
                    createdAt = now - 1800_000L,
                )
            )
            repo.insertMovement(
                MovementEntity(
                    type = MovementType.FIADO.name,
                    amount = 24_000L,
                    title = "Arroz y leche",
                    detail = "Fiado para Marta",
                    quantity = 2,
                    payMethod = PayMethod.EFECTIVO.name,
                    clientId = c1,
                    profileId = adminId,
                    profileName = "Pedro Gómez",
                    sessionId = session.id,
                    createdAt = now - 600_000L,
                )
            )
        }
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
