package com.barriogo.app.domain

import com.barriogo.app.data.ClientEntity
import com.barriogo.app.data.MovementEntity
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Un cliente que todavía debe (F1 · Fiados).
 * [desde] es la fecha del primer fiado de la deuda actual: si el cliente ya había quedado a paz y
 * salvo, lo anterior no cuenta. [fiados] son los fiados registrados desde esa fecha.
 */
data class FiadoPendiente(
    val cliente: ClientEntity,
    val saldo: Long,
    val desde: Long,
    val fiados: Int,
    /** Último fiado o abono del cliente (0 si nunca ha tenido). Ordena "Clientes recientes". */
    val ultimo: Long = desde,
)

object Fiados {
    private val zonaLocal: ZoneId get() = ZoneId.systemDefault()
    private val fechaCorta = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "CO"))
    private val hora = DateTimeFormatter.ofPattern("H:mm")
    private val dias = listOf("Lun.", "Mar.", "Mié.", "Jue.", "Vie.", "Sáb.", "Dom.")
    private val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

    /**
     * Fiados y abonos de un cliente, del más nuevo al más viejo. Con [conAnulados] también salen los
     * anulados (el historial los muestra tachados, como Movimientos).
     */
    fun movimientosDe(clienteId: Long, movimientos: List<MovementEntity>, conAnulados: Boolean = false): List<MovementEntity> =
        movimientos.filter { (conAnulados || !it.voided) && esDeFiadoAunqueAnulado(it) && it.clientId == clienteId }
            .sortedByDescending { it.createdAt }

    private fun esDeFiadoAunqueAnulado(m: MovementEntity) =
        m.clientId != null && (m.kind == MovementType.FIADO || m.kind == MovementType.ABONO)

    private fun esDeFiado(m: MovementEntity) = !m.voided && esDeFiadoAunqueAnulado(m)

    /**
     * Fiados y abonos vigentes de la deuda actual del cliente (desde que dejó de estar a paz y
     * salvo), del más viejo al más nuevo. Vacío si no debe nada.
     */
    fun deudaActual(clienteId: Long, movimientos: List<MovementEntity>): List<MovementEntity> {
        var saldo = 0L
        val deuda = mutableListOf<MovementEntity>()
        for (m in movimientos.filter { esDeFiado(it) && it.clientId == clienteId }.sortedBy { it.createdAt }) {
            if (m.kind == MovementType.FIADO) {
                if (saldo <= 0) { deuda.clear(); saldo = 0 }
                saldo += m.amount
            } else {
                saldo -= m.amount
            }
            deuda += m
        }
        return if (saldo > 0) deuda else emptyList()
    }

    /** "2 mar" */
    fun fechaCorta(millis: Long, zona: ZoneId = zonaLocal): String {
        val f = Instant.ofEpochMilli(millis).atZone(zona)
        return "${f.dayOfMonth} ${meses[f.monthValue - 1]}"
    }

    /** Hoja "Fiado pendiente": "Arroz y panela" o "Abono". */
    fun conceptoCorto(m: MovementEntity): String =
        if (m.kind == MovementType.ABONO) "Abono" else m.detail.trim().ifEmpty { "Fiado" }.replaceFirstChar { it.uppercase() }

    /** "Marta Ramírez pagó. Entraron $ 12.000 a tu caja." */
    fun mensajePagado(nombre: String, monto: Long): String = "$nombre pagó. Entraron ${Money.cop(monto)} a tu caja."

    /** Estado de cuenta de todos los clientes, también los que no deben nada (saldo 0). */
    fun cuentas(clientes: List<ClientEntity>, movimientos: List<MovementEntity>): List<FiadoPendiente> {
        val porCliente = movimientos.filter(::esDeFiado).groupBy { it.clientId }
        return clientes.map { c ->
            var saldo = 0L
            var desde = 0L
            var fiados = 0
            var ultimo = 0L
            for (m in porCliente[c.id].orEmpty().sortedBy { it.createdAt }) {
                if (m.kind == MovementType.FIADO) {
                    // Empieza una deuda nueva
                    if (saldo <= 0) { desde = m.createdAt; fiados = 0; saldo = 0 }
                    saldo += m.amount
                    fiados++
                } else {
                    saldo -= m.amount
                }
                ultimo = m.createdAt
            }
            if (saldo > 0) FiadoPendiente(c, saldo, desde, fiados, ultimo) else FiadoPendiente(c, 0, 0, 0, ultimo)
        }
    }

    /** Clientes con saldo pendiente, del que más debe al que menos. Los anulados no cuentan. */
    fun pendientes(clientes: List<ClientEntity>, movimientos: List<MovementEntity>): List<FiadoPendiente> =
        cuentas(clientes, movimientos).filter { it.saldo > 0 }.sortedByDescending { it.saldo }

    /** "Clientes recientes" de Seleccionar cliente: primero los que tuvieron fiados o abonos hace menos. */
    fun recientes(cuentas: List<FiadoPendiente>): List<FiadoPendiente> =
        cuentas.sortedWith(compareByDescending<FiadoPendiente> { it.ultimo }.thenByDescending { it.cliente.id })

    /** "DM" para Doña Marta, "M" si solo hay un nombre. */
    fun iniciales(nombre: String): String =
        nombre.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }

    /** "Hoy · 9:02" o "Sáb. 5 sep · 11:00". */
    fun cuando(millis: Long, ahora: Long = System.currentTimeMillis(), zona: ZoneId = zonaLocal): String {
        val f = Instant.ofEpochMilli(millis).atZone(zona)
        val dia = if (f.toLocalDate() == Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()) "Hoy"
        else "${dias[f.dayOfWeek.value - 1]} ${f.dayOfMonth} ${meses[f.monthValue - 1]}"
        return "$dia · ${f.format(hora)}"
    }

    /** "8:15", sin el cero de adelante, como en el Figma. */
    fun hora(millis: Long, zona: ZoneId = zonaLocal): String = Instant.ofEpochMilli(millis).atZone(zona).format(hora)

    /** Título de un movimiento en la ficha del cliente: lo que se llevó, o "Abono en efectivo". */
    fun titulo(m: MovementEntity): String = when (m.kind) {
        MovementType.ABONO -> if (m.pay == PayMethod.TRANSFERENCIA) "Abono por transferencia" else "Abono en efectivo"
        else -> m.detail.trim().ifEmpty { "Fiado" }.replaceFirstChar { it.uppercase() }
    }

    /** "Marta Ramírez se llevó leche, pan, huevos y arroz por $ 15.000." */
    fun mensajeGuardado(nombre: String, queSeLlevo: String, monto: Long): String {
        val que = queSeLlevo.trim().trimEnd('.')
        return if (que.isEmpty()) "Le fiaste a $nombre ${Money.cop(monto)}."
        else "$nombre se llevó ${que.replaceFirstChar { it.lowercase() }} por ${Money.cop(monto)}."
    }

    /** Registrar pago: null si el abono se puede guardar. Con 0 no hay mensaje (el botón solo queda apagado). */
    fun problemaAbono(monto: Long, saldo: Long): String? = when {
        monto <= 0 -> ""
        saldo <= 0 -> "Este cliente no tiene saldo pendiente"
        monto > saldo -> "No puede abonar más de lo que debe (${Money.cop(saldo)})"
        else -> null
    }

    /** "Abono de $ 20.000 recibido de Marta Ramírez." */
    fun mensajePago(nombre: String, monto: Long): String = "Abono de ${Money.cop(monto)} recibido de $nombre."

    /** "2 fiados pendientes" */
    fun fiadosPendientes(cantidad: Int): String = when (cantidad) {
        0 -> "Sin fiados pendientes"
        1 -> "1 fiado pendiente"
        else -> "$cantidad fiados pendientes"
    }

    /** Busca por nombre (sin importar tildes ni mayúsculas) o por los dígitos del teléfono. */
    fun filtrar(lista: List<FiadoPendiente>, texto: String): List<FiadoPendiente> {
        val q = sinTildes(texto.trim())
        if (q.isEmpty()) return lista
        val digitos = q.filter { it.isDigit() }
        return lista.filter { f ->
            sinTildes(f.cliente.name).contains(q) ||
                (digitos.isNotEmpty() && f.cliente.phone.filter { it.isDigit() }.contains(digitos))
        }
    }

    /** "Desde el 2 de marzo · 3 fiados" */
    fun subtitulo(f: FiadoPendiente, zona: ZoneId = ZoneId.systemDefault()): String {
        val fecha = Instant.ofEpochMilli(f.desde).atZone(zona).format(fechaCorta)
        return "Desde el $fecha · ${f.fiados} ${if (f.fiados == 1) "fiado" else "fiados"}"
    }

    /** "3 clientes con fiado pendiente" */
    fun resumenClientes(cantidad: Int): String = when (cantidad) {
        0 -> "Nadie te debe"
        1 -> "1 cliente con fiado pendiente"
        else -> "$cantidad clientes con fiado pendiente"
    }

    private fun sinTildes(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()
}
