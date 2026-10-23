package com.barriogo.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

enum class CampoGasto { TIPO, MONTO, DETALLE }

/** G1 · Filtros Hoy / Semana / Mes. */
enum class PeriodoGastos(val etiqueta: String) { HOY("Hoy"), SEMANA("Semana"), MES("Mes") }

/**
 * G2 · Registrar y editar gastos. Mismas reglas que firestore.rules → movimientoBienFormado()
 * para un gasto: categoría del catálogo, monto entero entre 1 y 1.000 millones y descripción de 1 a 200.
 */
object ReglasGasto {
    const val DETALLE_MAXIMO = 200

    const val FALTA_TIPO = "Primero elige el tipo de gasto"
    const val FALTA_MONTO = "Escribe un monto mayor a cero"
    const val FALTA_DETALLE = "Escribe en qué fue el gasto"

    /** Id en el catálogo categoriasGasto de Firestore ("proveedor", "servicios", "otros"). */
    fun categoriaId(tipo: ExpenseKind): String = tipo.name.lowercase()

    fun problemas(tipo: ExpenseKind?, monto: Long, detalle: String): Map<CampoGasto, String> = buildMap {
        if (tipo == null) put(CampoGasto.TIPO, FALTA_TIPO)
        when {
            monto <= 0 -> put(CampoGasto.MONTO, FALTA_MONTO)
            monto > ReglasCaja.MONTO_MAXIMO -> put(CampoGasto.MONTO, "El monto no puede pasar de ${Money.cop(ReglasCaja.MONTO_MAXIMO)}")
        }
        val d = detalle.trim()
        when {
            d.isEmpty() -> put(CampoGasto.DETALLE, FALTA_DETALLE)
            d.length > DETALLE_MAXIMO -> put(CampoGasto.DETALLE, "Escribe máximo $DETALLE_MAXIMO letras")
        }
    }

    fun hayCambios(original: GastoEditable, tipo: ExpenseKind?, monto: Long, detalle: String) =
        original.tipo != tipo || original.monto != monto || original.detalle.trim() != detalle.trim()

    /**
     * Mensaje bajo "Guardar gasto" / "Guardar cambios" (Figma G2), en el orden en que se llena el formulario.
     * Antes de intentar guardar guía el siguiente paso; después ([intento]) dice qué falta.
     */
    fun estadoBoton(tipo: ExpenseKind?, monto: Long, detalle: String, original: GastoEditable? = null, intento: Boolean = false): String {
        val p = problemas(tipo, monto, detalle)
        return when {
            CampoGasto.TIPO in p -> "Elige el tipo de gasto"
            CampoGasto.MONTO in p -> when {
                monto > 0 -> p.getValue(CampoGasto.MONTO)
                intento -> "Falta el monto"
                else -> "Ahora escribe cuánto pagaste"
            }
            CampoGasto.DETALLE in p -> if (detalle.isBlank()) "Falta en qué fue el gasto" else p.getValue(CampoGasto.DETALLE)
            original != null && !hayCambios(original, tipo, monto, detalle) -> "Todavía no has cambiado nada"
            else -> "Todo listo, ya puedes guardar"
        }
    }

    fun puedeGuardar(tipo: ExpenseKind?, monto: Long, detalle: String, original: GastoEditable? = null) =
        problemas(tipo, monto, detalle).isEmpty() && (original == null || hayCambios(original, tipo, monto, detalle))

    /** Primer día que entra en el filtro (la semana empieza el lunes). */
    fun desde(periodo: PeriodoGastos, hoy: LocalDate): LocalDate = when (periodo) {
        PeriodoGastos.HOY -> hoy
        PeriodoGastos.SEMANA -> hoy.with(DayOfWeek.MONDAY)
        PeriodoGastos.MES -> hoy.withDayOfMonth(1)
    }

    /** Tipo guardado en el movimiento; los gastos viejos solo tenían el texto "Gasto proveedor". */
    fun tipoDe(categoria: String?, detalle: String): ExpenseKind =
        categoria?.let { c -> ExpenseKind.entries.firstOrNull { it.name == c } }
            ?: ExpenseKind.entries.firstOrNull { detalle.contains(it.label, ignoreCase = true) }
            ?: ExpenseKind.OTROS
}

/** Lo que se puede cambiar de un gasto ya guardado. */
data class GastoEditable(val tipo: ExpenseKind, val monto: Long, val detalle: String)
