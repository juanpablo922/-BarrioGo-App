package com.barriogo.app.ui.nav

/** Todas las rutas de navegación de la app, agrupadas por módulo. */
object Routes {
    const val SPLASH = "splash"

    // Acceso y registro
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val RECUPERAR = "recuperar"
    const val CREATE_PIN = "create_pin"
    const val PROFILE_SELECTOR = "profile_selector"
    const val PIN_LOGIN = "pin_login/{profileId}"
    fun pinLogin(profileId: Long) = "pin_login/$profileId"
    const val CREATE_PROFILE = "create_profile"
    const val BIOMETRIC = "biometric"

    // Núcleo
    const val HOME = "home"
    const val CASH = "cash"
    /** K0 / K1. corregir=true entra directo a "Corregir la apertura". */
    const val OPEN_CASH = "open_cash?corregir={corregir}"
    fun openCash(corregir: Boolean = false) = "open_cash?corregir=$corregir"
    /** c2–c4. corregir=true corrige el último cierre (máximo 2 veces). */
    const val CLOSE_CASH = "close_cash?corregir={corregir}"
    fun closeCash(corregir: Boolean = false) = "close_cash?corregir=$corregir"
    const val ARQUEO = "arqueo"

    const val REGISTER_MOVEMENT = "register_movement"
    const val MOVEMENTS_BOOK = "movements_book"
    const val MOVEMENT_DETAIL = "movement_detail/{id}"
    fun movementDetail(id: Long) = "movement_detail/$id"

    /** G1. aviso = mensaje corto al volver de guardar ("Gasto guardado…"). */
    const val EXPENSES = "expenses?aviso={aviso}"
    fun expenses(aviso: String? = null) = if (aviso == null) "expenses" else "expenses?aviso=${android.net.Uri.encode(aviso)}"
    /** G2. Sin id registra; con id (documento de Firestore) edita ese gasto. */
    const val REGISTER_EXPENSE = "register_expense?id={id}"
    fun registerExpense(id: String? = null) = if (id == null) "register_expense" else "register_expense?id=$id"

    const val FIADOS = "fiados"
    /** Nuevo fiado en 3 pasos. Con clientId (desde la ficha) empieza en el paso 2. */
    const val NEW_FIADO = "new_fiado?clientId={clientId}"
    fun newFiado(clientId: Long? = null) = if (clientId == null) "new_fiado" else "new_fiado?clientId=$clientId"
    /** Ficha del cliente: cuánto debe y sus fiados y abonos. */
    const val CLIENTE_FIADO = "cliente_fiado/{clientId}"
    fun clienteFiado(clientId: Long) = "cliente_fiado/$clientId"
    /** Registrar pago. Con fiadoId (id en Firestore) el abono va a ese solo fiado. */
    const val REGISTER_PAYMENT = "register_payment/{clientId}?fiadoId={fiadoId}"
    fun registerPayment(clientId: Long, fiadoId: String? = null) =
        if (fiadoId == null) "register_payment/$clientId" else "register_payment/$clientId?fiadoId=$fiadoId"

    // IA: voz
    const val VOICE_CAPTURE = "voice_capture/{mode}"
    fun voiceCapture(mode: String) = "voice_capture/$mode"
    const val VOICE_CONFIRM = "voice_confirm"

    // IA: escáner de facturas
    const val SCAN_INVOICE = "scan_invoice"
    const val REVIEW_INVOICE = "review_invoice"
}
