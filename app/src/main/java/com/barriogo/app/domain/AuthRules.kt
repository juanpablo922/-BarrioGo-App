package com.barriogo.app.domain

/** Reglas de la contraseña de la tienda que muestran R3 y RP3. */
object PasswordPolicy {
    const val MIN_LENGTH = 8

    data class Check(val label: String, val met: Boolean)

    fun checks(password: String): List<Check> = listOf(
        Check("Al menos $MIN_LENGTH caracteres", password.length >= MIN_LENGTH),
        Check("Una letra mayúscula", password.any { it.isUpperCase() }),
        Check("Un número o carácter especial", password.any { it.isDigit() || !it.isLetterOrDigit() }),
    )

    fun isValid(password: String): Boolean = checks(password).all { it.met }
}

/** Validaciones de formulario compartidas por ingreso, registro y recuperación. */
object FormRules {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isEmail(value: String): Boolean = EMAIL.matches(value.trim())

    /** Celular colombiano: 10 dígitos empezando por 3 (se ignoran espacios). */
    fun isPhone(value: String): Boolean = value.filter(Char::isDigit).let { it.length == 10 && it.startsWith("3") }

    /** Oculta el correo para mostrarlo en pantalla: "juanperez@gmail.com" → "j••••••z@gmail.com". */
    fun maskEmail(email: String): String {
        val at = email.indexOf('@')
        if (at <= 1) return email
        val user = email.substring(0, at)
        return user.first() + "•".repeat((user.length - 2).coerceAtLeast(1)) + user.last() + email.substring(at)
    }
}

/** Bloqueo de la clave de 4 números: 3 intentos y 30 segundos de espera. */
object PinPolicy {
    const val LENGTH = 4
    const val MAX_ATTEMPTS = 3
    const val LOCK_MS = 30_000L
}
