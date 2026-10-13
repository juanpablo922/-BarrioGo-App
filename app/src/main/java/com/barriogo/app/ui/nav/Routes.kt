package com.barriogo.app.ui.nav

import com.barriogo.app.ui.auth.login.LoginNotice

/** Todas las rutas de navegación de la app, agrupadas por módulo. */
object Routes {
    const val SPLASH = "splash"

    // Acceso (L2-L6)
    const val LOGIN = "login?notice={notice}"
    fun login(notice: LoginNotice? = null) = if (notice == null) "login" else "login?notice=${notice.name}"
    const val PROFILE_SELECTOR = "profile_selector"
    const val PIN_LOGIN = "pin_login/{profileId}"
    fun pinLogin(profileId: Long) = "pin_login/$profileId"
    const val FINGERPRINT_LOGIN = "fingerprint_login/{profileId}"
    fun fingerprintLogin(profileId: Long) = "fingerprint_login/$profileId"
    const val LOCKED = "locked/{profileId}"
    fun locked(profileId: Long) = "locked/$profileId"

    // Registro del negocio (R1-R4), un subgrafo que comparte RegisterViewModel
    const val REGISTER_GRAPH = "register"
    const val REGISTER_OWNER = "register/owner"
    const val REGISTER_CATEGORY = "register/category"
    const val REGISTER_PASSWORD = "register/password"
    const val REGISTER_SUCCESS = "register/success"

    // Primer perfil (R5-R7), subgrafo que comparte ProfileSetupViewModel
    const val SETUP_GRAPH = "setup"
    const val SETUP_PIN = "setup/pin"
    const val SETUP_PROFILE = "setup/profile"
    const val SETUP_FINGERPRINT = "setup/fingerprint"

    // Recuperar contraseña de la tienda (RP1-RP3)
    const val RECOVER_PASSWORD_GRAPH = "recover_password"
    const val RECOVER_PASSWORD_EMAIL = "recover_password/email"
    const val RECOVER_PASSWORD_CODE = "recover_password/code"
    const val RECOVER_PASSWORD_NEW = "recover_password/new"

    // Recuperar la clave de 4 números de un perfil (CP1-CP3)
    const val RECOVER_PIN_GRAPH = "recover_pin/{profileId}"
    fun recoverPin(profileId: Long) = "recover_pin/$profileId"
    const val RECOVER_PIN_EMAIL = "recover_pin/email"
    const val RECOVER_PIN_CODE = "recover_pin/code"
    const val RECOVER_PIN_NEW = "recover_pin/new"

    // Núcleo
    const val HOME = "home"
    const val CASH = "cash"
    const val OPEN_CASH = "open_cash"
    const val CLOSE_CASH = "close_cash"

    const val REGISTER_MOVEMENT = "register_movement"
    const val MOVEMENTS_BOOK = "movements_book"
    const val MOVEMENT_DETAIL = "movement_detail/{id}"
    fun movementDetail(id: Long) = "movement_detail/$id"

    const val EXPENSES = "expenses"
    const val REGISTER_EXPENSE = "register_expense"

    const val FIADOS = "fiados"
    const val NEW_FIADO = "new_fiado"
    const val REGISTER_PAYMENT = "register_payment/{clientId}"
    fun registerPayment(clientId: Long) = "register_payment/$clientId"

    // IA: voz
    const val VOICE_CAPTURE = "voice_capture/{mode}"
    fun voiceCapture(mode: String) = "voice_capture/$mode"
    const val VOICE_CONFIRM = "voice_confirm"

    // IA: escáner de facturas
    const val SCAN_INVOICE = "scan_invoice"
    const val REVIEW_INVOICE = "review_invoice"
}
