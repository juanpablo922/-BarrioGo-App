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
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import com.barriogo.app.domain.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Contenedor de dependencias simple (sin Hilt) para mantener el proyecto fácil de compilar. */
class AppContainer(context: Context) {
    val db = AppDatabase.build(context)
    val repo = BarrioRepository(db)
    val store = SessionStore(context)

    /** Perfil que está atendiendo ahora. Se pierde al cerrar la app: siempre se pide la clave de nuevo. */
    val activeProfileId = MutableStateFlow<Long?>(null)

    suspend fun authenticateStore(email: String, pass: String): Boolean {
        // Soporte para cuenta de demostración rápida
        if (email.equals("tienda@barriogo.com", ignoreCase = true) && pass == "123456") {
            seedDemoData()
            return true
        }
        // Validación con la tienda real registrada por el usuario
        if (store.registered) {
            val emailMatches = email.equals(store.email, ignoreCase = true)
            val passMatches = store.checkPassword(pass)
            return emailMatches && passMatches
        }
        return false
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
        container = AppContainer(this)
    }
}
