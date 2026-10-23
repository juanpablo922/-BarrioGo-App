package com.barriogo.app.domain

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** c4 · Corregir cierre: máximo 2 veces (functions/src/dominio/caja.ts → MAXIMO_CAMBIOS_CIERRE). */
const val MAX_CLOSING_CHANGES = 2

/**
 * Cómo está la caja HOY. Sin caja abierta hoy no se registra nada: ni ventas, ni gastos, ni fiados.
 * - [ABIERTA_DIA_ANTERIOR]: quedó abierta la de un día pasado; hay que cerrarla antes de abrir la de hoy.
 * - [CERRADA_HOY]: la caja de hoy ya se cerró; solo se puede corregir el cierre.
 */
enum class EstadoCaja { SIN_ABRIR, ABIERTA, ABIERTA_DIA_ANTERIOR, CERRADA_HOY }

enum class ResultadoArqueo(val etiqueta: String) { CUADRADO("Cuadrado"), FALTANTE("Faltante"), SOBRANTE("Sobrante") }

data class Arqueo(val diferencia: Long, val resultado: ResultadoArqueo)

/** Aviso de cierre: falta poco para la hora de cierre o ya pasó y la caja sigue abierta. */
sealed interface AvisoCierre {
    data object Ninguno : AvisoCierre
    data class Pronto(val minutosFaltan: Long) : AvisoCierre
    data class Vencido(val minutosPasados: Long) : AvisoCierre
}

/**
 * Totales de una caja con la misma cuenta que functions/src/dominio/caja.ts → calcularTotalesCaja():
 * ventas y abonos en efectivo entran, gastos salen, fiados y transferencias no mueven el efectivo.
 */
data class TotalesCaja(
    val apertura: Long,
    val ventas: Long,
    val ventasEfectivo: Long,
    val gastos: Long,
    val fiados: Long,
    val abonos: Long,
    val abonosEfectivo: Long,
    val ajustes: Long,
    val entradas: Long,
    val salidas: Long,
    val saldoEsperado: Long,
) {
    val ventasTransferencia: Long get() = ventas - ventasEfectivo
}

/**
 * Reglas de apertura, cierre y hora de cierre. Kotlin puro para probarlo con JUnit.
 * Son las mismas de firestore.rules y de functions/src/dominio/caja.ts del backend.
 */
object ReglasCaja {
    /** Igual que montoValido() en firestore.rules. */
    const val MONTO_MAXIMO = 1_000_000_000L
    const val OBSERVACIONES_MAXIMO = 500
    const val HORA_CIERRE_POR_DEFECTO = "20:00"
    /** El primer aviso sale 30 minutos antes de la hora de cierre. */
    const val MINUTOS_AVISO_ANTES = 30L
    /** Pasada la hora, se repite el aviso cada 30 minutos mientras la caja siga abierta. */
    const val MINUTOS_ENTRE_AVISOS = 30L

    private val HORA = Regex("([01][0-9]|2[0-3]):[0-5][0-9]")
    private val formatoHora = DateTimeFormatter.ofPattern("h:mm a", Locale("es", "CO"))

    /** Id de la caja en Firestore: uno por día ({fecha}-1), así dos celulares sin internet no abren dos cajas. */
    fun idCaja(dia: LocalDate): String = "$dia-1"

    fun estado(hoy: LocalDate, fechaAbierta: LocalDate?, fechaUltimoCierre: LocalDate?): EstadoCaja = when {
        fechaAbierta == hoy -> EstadoCaja.ABIERTA
        fechaAbierta != null -> EstadoCaja.ABIERTA_DIA_ANTERIOR
        fechaUltimoCierre == hoy -> EstadoCaja.CERRADA_HOY
        else -> EstadoCaja.SIN_ABRIR
    }

    fun permiteRegistrar(estado: EstadoCaja) = estado == EstadoCaja.ABIERTA

    fun cambiosAperturaRestantes(usados: Int) = (MAX_OPENING_CHANGES - usados).coerceAtLeast(0)
    fun cambiosCierreRestantes(usados: Int) = (MAX_CLOSING_CHANGES - usados).coerceAtLeast(0)

    /** K0 / K1 · Problema con el monto de apertura, o null si sirve. [actual] solo al corregir. */
    fun problemaApertura(monto: Long, actual: Long? = null): String? = when {
        monto <= 0 -> "La apertura no puede quedar en $ 0"
        monto > MONTO_MAXIMO -> "El monto no puede pasar de ${Money.cop(MONTO_MAXIMO)}"
        actual != null && monto == actual -> "Escribe un valor distinto al actual"
        else -> null
    }

    /** c2 · Efectivo contado (puede ser $ 0 si la caja quedó vacía) y observaciones. */
    fun problemaConteo(contado: Long?, observaciones: String): String? = when {
        contado == null -> "Escribe cuánto efectivo contaste"
        contado < 0 -> "El efectivo contado no puede ser negativo"
        contado > MONTO_MAXIMO -> "El monto no puede pasar de ${Money.cop(MONTO_MAXIMO)}"
        observaciones.trim().length > OBSERVACIONES_MAXIMO -> "Las observaciones pueden tener máximo $OBSERVACIONES_MAXIMO letras"
        else -> null
    }

    /** c4 · Corregir cierre: tiene que cambiar algo. */
    fun hayCambiosEnCierre(contadoAntes: Long, notaAntes: String, contado: Long, nota: String) =
        contadoAntes != contado || notaAntes.trim() != nota.trim()

    fun arqueo(esperado: Long, contado: Long): Arqueo {
        val diferencia = contado - esperado
        val resultado = when {
            diferencia == 0L -> ResultadoArqueo.CUADRADO
            diferencia < 0 -> ResultadoArqueo.FALTANTE
            else -> ResultadoArqueo.SOBRANTE
        }
        return Arqueo(diferencia, resultado)
    }

    fun totales(apertura: Long, movimientos: List<com.barriogo.app.data.MovementEntity>): TotalesCaja {
        var ventas = 0L; var ventasEfectivo = 0L; var gastos = 0L; var fiados = 0L
        var abonos = 0L; var abonosEfectivo = 0L; var ajustes = 0L; var entradas = 0L; var salidas = 0L
        for (m in movimientos) {
            if (m.voided) continue
            val delta = m.cashDelta()
            if (delta > 0) entradas += delta
            if (delta < 0) salidas -= delta
            when (m.kind) {
                MovementType.VENTA -> { ventas += m.amount; ventasEfectivo += delta }
                MovementType.GASTO -> gastos += m.amount
                MovementType.FIADO -> fiados += m.amount
                MovementType.ABONO -> { abonos += m.amount; abonosEfectivo += delta }
                MovementType.AJUSTE -> ajustes += delta
            }
        }
        return TotalesCaja(
            apertura = apertura, ventas = ventas, ventasEfectivo = ventasEfectivo, gastos = gastos, fiados = fiados,
            abonos = abonos, abonosEfectivo = abonosEfectivo, ajustes = ajustes, entradas = entradas, salidas = salidas,
            saldoEsperado = apertura + entradas - salidas,
        )
    }

    // ── Hora de cierre ───────────────────────────────────────────────────────

    fun horaValida(hora: String) = HORA.matches(hora)

    fun hora(texto: String?): LocalTime =
        LocalTime.parse(texto?.takeIf { horaValida(it) } ?: HORA_CIERRE_POR_DEFECTO)

    /** "20:00" → "8:00 p. m." */
    fun horaLegible(hora: LocalTime): String = hora.format(formatoHora)

    /** "20:00" para guardar (24 horas, siempre con dos dígitos). */
    fun horaTexto(hora: LocalTime): String = "%02d:%02d".format(hora.hour, hora.minute)

    /**
     * ¿Hay que avisar? Se cuenta desde el día en que se abrió la caja: una caja de ayer que sigue
     * abierta ya está vencida aunque hoy todavía no sea la hora de cierre.
     */
    fun aviso(ahora: LocalDateTime, horaCierre: LocalTime, fechaCajaAbierta: LocalDate?): AvisoCierre {
        if (fechaCajaAbierta == null) return AvisoCierre.Ninguno
        val minutos = Duration.between(ahora, fechaCajaAbierta.atTime(horaCierre)).toMinutes()
        return when {
            minutos > MINUTOS_AVISO_ANTES -> AvisoCierre.Ninguno
            minutos > 0 -> AvisoCierre.Pronto(minutos)
            else -> AvisoCierre.Vencido(-minutos)
        }
    }

    /** Cuándo programar el siguiente aviso, o null si no hay caja abierta. */
    fun proximoAviso(ahora: LocalDateTime, horaCierre: LocalTime, fechaCajaAbierta: LocalDate?): LocalDateTime? {
        if (fechaCajaAbierta == null) return null
        val cierre = fechaCajaAbierta.atTime(horaCierre)
        val antes = cierre.minusMinutes(MINUTOS_AVISO_ANTES)
        return when {
            ahora.isBefore(antes) -> antes
            ahora.isBefore(cierre) -> cierre
            else -> ahora.plusMinutes(MINUTOS_ENTRE_AVISOS)
        }
    }

    /** Texto del aviso (notificación y franja en Inicio / Caja). */
    fun textoAviso(aviso: AvisoCierre, horaCierre: LocalTime): String? = when (aviso) {
        AvisoCierre.Ninguno -> null
        is AvisoCierre.Pronto -> "La caja se cierra a las ${horaLegible(horaCierre)}. Faltan ${duracion(aviso.minutosFaltan)}."
        is AvisoCierre.Vencido -> "La caja sigue abierta. Ya pasaron ${duracion(aviso.minutosPasados)} de la hora de cierre (${horaLegible(horaCierre)})."
    }

    private fun duracion(minutos: Long): String {
        val h = minutos / 60
        val m = minutos % 60
        return when {
            h == 0L -> if (m == 1L) "1 minuto" else "$m minutos"
            m == 0L -> if (h == 1L) "1 hora" else "$h horas"
            else -> "${h} h ${m} min"
        }
    }
}
