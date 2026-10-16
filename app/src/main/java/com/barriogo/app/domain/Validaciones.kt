package com.barriogo.app.domain

/**
 * Validaciones del acceso y el registro. Son las mismas reglas que firestore.rules y que
 * functions/src/dominio/seguridad.ts del backend: si la app deja pasar algo que las reglas
 * rechazan, el registro falla con un error poco claro.
 * Kotlin puro (sin android.util.Patterns) para poder probarlo con JUnit.
 */
object Validaciones {
    private val CORREO = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private val CELULAR = Regex("3[0-9]{9}")
    private val MAYUSCULA = Regex("[A-ZÁÉÍÓÚÑ]")
    private val NUMERO_O_ESPECIAL = Regex("[0-9]|[^A-Za-zÁÉÍÓÚÑáéíóúñ0-9]")

    /** Los tres requisitos de R3 y RP3, en el orden en que se muestran. */
    val REQUISITOS_CONTRASENA = listOf("Al menos 8 caracteres", "Una letra mayúscula", "Un número o carácter especial")

    fun normalizarCorreo(correo: String) = correo.trim().lowercase()
    fun correoValido(correo: String) = normalizarCorreo(correo).let { it.length <= 254 && CORREO.matches(it) }

    /** Pantallas R3 y RP3. Lista vacía = cumple todo. */
    fun problemasContrasena(c: String): List<String> = buildList {
        if (c.length < 8) add(REQUISITOS_CONTRASENA[0])
        if (!MAYUSCULA.containsMatchIn(c)) add(REQUISITOS_CONTRASENA[1])
        if (!NUMERO_O_ESPECIAL.containsMatchIn(c)) add(REQUISITOS_CONTRASENA[2])
        if (c.length > 100) add("Máximo 100 caracteres")   // límite de la política de Firebase Auth
    }

    fun normalizarCelular(celular: String) = celular.filter(Char::isDigit)
    fun celularValido(celular: String) = CELULAR.matches(normalizarCelular(celular))

    fun textoValido(texto: String, maximo: Int) = texto.trim().length in 1..maximo
    fun claveValida(clave: String) = clave.length == 4 && clave.all(Char::isDigit)
    fun codigoValido(codigo: String) = codigo.length == 6 && codigo.all(Char::isDigit)

    /** Categorías de la app (BUSINESS_CATEGORIES) → ids de categoriasNegocio en Firestore. */
    val CATEGORIA_ID = mapOf(
        "Abarrotes" to "abarrotes",
        "Panadería" to "panaderia",
        "Farmacia" to "farmacia",
        "Ferretería" to "ferreteria",
        "Distribuidoras" to "distribuidoras",
        "Otros" to "otros",
    )

    /** La única categoría que pide escribir de qué es el negocio (permiteTextoLibre en Firestore). */
    const val CATEGORIA_OTROS = "Otros"

    /**
     * Categoría como la guarda el registro → (categoriaNegocioId, categoriaOtra) de Firestore.
     * "Abarrotes" → ("abarrotes", null). En "Otros" el registro guarda lo que escribió la persona:
     * "Papelería" → ("otros", "Papelería").
     */
    fun categoriaFirestore(categoria: String): Pair<String, String?> {
        val c = categoria.trim()
        return CATEGORIA_ID[c]?.let { it to null } ?: ("otros" to c)
    }

    /** Problema con la categoría elegida en R2, o null si sirve. */
    fun problemaCategoria(categoria: String): String? = when {
        categoria.isBlank() -> "Selecciona una categoría para continuar"
        categoria.trim() == CATEGORIA_OTROS -> "Escribe de qué es tu negocio"
        categoria.trim() !in CATEGORIA_ID && !textoValido(categoria, 60) -> "Escribe de qué es tu negocio en máximo 60 letras"
        else -> null
    }
}
