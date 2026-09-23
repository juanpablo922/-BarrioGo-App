package com.barriogo.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Datos de la tienda y preferencias (SharedPreferences). Todo queda en el celular. */
class SessionStore(context: Context) {
    private val sp = context.getSharedPreferences("barriogo_store", Context.MODE_PRIVATE)

    private val _dark = MutableStateFlow(sp.getBoolean("dark", true))
    val darkTheme: StateFlow<Boolean> = _dark
    fun setDark(v: Boolean) { sp.edit().putBoolean("dark", v).apply(); _dark.value = v }

    private val _hide = MutableStateFlow(sp.getBoolean("hide", false))
    val hideAmounts: StateFlow<Boolean> = _hide
    fun setHide(v: Boolean) { sp.edit().putBoolean("hide", v).apply(); _hide.value = v }

    var registered: Boolean
        get() = sp.getBoolean("registered", false)
        set(v) { sp.edit().putBoolean("registered", v).apply() }

    /** false después de "Salir de la tienda": para volver hay que ingresar con correo y contraseña. */
    var signedIn: Boolean
        get() = sp.getBoolean("signedIn", false)
        set(v) { sp.edit().putBoolean("signedIn", v).apply() }

    var ownerName: String
        get() = sp.getString("owner", "") ?: ""
        set(v) { sp.edit().putString("owner", v).apply() }
    var storeName: String
        get() = sp.getString("store", "") ?: ""
        set(v) { sp.edit().putString("store", v).apply() }
    var address: String
        get() = sp.getString("address", "") ?: ""
        set(v) { sp.edit().putString("address", v).apply() }
    var email: String
        get() = sp.getString("email", "") ?: ""
        set(v) { sp.edit().putString("email", v).apply() }
    var phone: String
        get() = sp.getString("phone", "") ?: ""
        set(v) { sp.edit().putString("phone", v).apply() }
    var category: String
        get() = sp.getString("category", "") ?: ""
        set(v) { sp.edit().putString("category", v).apply() }
    var passSalt: String
        get() = sp.getString("passSalt", "") ?: ""
        set(v) { sp.edit().putString("passSalt", v).apply() }
    var passHash: String
        get() = sp.getString("passHash", "") ?: ""
        set(v) { sp.edit().putString("passHash", v).apply() }

    fun setPassword(password: String) {
        val salt = Hasher.newSalt()
        passSalt = salt
        passHash = Hasher.hash(salt, password)
    }

    fun checkPassword(password: String): Boolean = passHash.isNotEmpty() && Hasher.matches(passSalt, password, passHash)

    // ---- Intentos fallidos de PIN por perfil ----
    fun failedAttempts(profileId: Long): Int = sp.getInt("fails_$profileId", 0)
    fun setFailedAttempts(profileId: Long, n: Int) { sp.edit().putInt("fails_$profileId", n).apply() }
    fun lockUntil(profileId: Long): Long = sp.getLong("lock_$profileId", 0L)
    fun setLockUntil(profileId: Long, t: Long) { sp.edit().putLong("lock_$profileId", t).apply() }
}
