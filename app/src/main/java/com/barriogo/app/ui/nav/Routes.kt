package com.barriogo.app.ui.nav

/** Todas las rutas de navegación de la app, agrupadas por módulo. */
object Routes {
    const val SPLASH = "splash"

    // Acceso y registro
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CREATE_PIN = "create_pin"
    const val PROFILE_SELECTOR = "profile_selector"
    const val PIN_LOGIN = "pin_login/{profileId}"
    fun pinLogin(profileId: Long) = "pin_login/$profileId"
    const val CREATE_PROFILE = "create_profile"
    const val BIOMETRIC = "biometric"

    // Núcleo
    const val HOME = "home"
    const val CASH = "cash"
    const val OPEN_CASH = "open_cash"
    const val CLOSE_CASH = "close_cash"
    const val ARQUEO = "arqueo"

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
