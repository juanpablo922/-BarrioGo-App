package com.barriogo.app

import android.app.Application
import android.content.Context
import com.barriogo.app.data.AppDatabase
import com.barriogo.app.data.BarrioRepository
import com.barriogo.app.data.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow

/** Contenedor de dependencias simple (sin Hilt) para mantener el proyecto fácil de compilar. */
class AppContainer(context: Context) {
    val db = AppDatabase.build(context)
    val repo = BarrioRepository(db)
    val store = SessionStore(context)

    /** Perfil que está atendiendo ahora. Se pierde al cerrar la app: siempre se pide la clave de nuevo. */
    val activeProfileId = MutableStateFlow<Long?>(null)
}

class BarrioGoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
