package com.barriogo.app.domain

import com.barriogo.app.data.MovementEntity
import com.barriogo.app.data.fiados.Cliente
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Cuánto se ha pagado de un fiado. Pagar no borra el fiado: pasa a abonado o pagado.
 * Misma regla que functions/src/dominio/fiados.ts → estadoPagoFiado().
 */
enum class EstadoPagoFiado(val texto: String) { PENDIENTE("Pendiente"), ABONADO("Abonado"), PAGADO("Pagado") }

/** Estado de pago de un fiado y lo que le falta por pagar. */
data class PagoFiado(val estado: EstadoPagoFiado, val porPagar: Long)

/**
 * Un cliente que todavía debe (F1 · Fiados).
 * [desde] es la fecha del primer fiado de la deuda actual: si el cliente ya había quedado a paz y
 * salvo, lo anterior no cuenta. [fiados] son los fiados registrados desde esa fecha.
 */
data class FiadoPendiente(
    val cliente: Cliente,
    val saldo: Long,
    val desde: Long,
    val fiados: Int,
    /** Último fiado o abono del cliente (0 si nunca ha tenido). Ordena "Clientes recientes". */
    val ultimo: Long = desde,
)

object Fiados {
    /**
     * Cupo con el que nace todo cliente: lo máximo que puede deber. Solo un administrador lo sube.
     * Mismo valor que CUPO_FIADO_POR_DEFECTO del backend y cupoFiado() de firestore.rules.
     */
    const val CUPO_POR_DEFECTO = 50_000L
    const val CUPO_MAXIMO = 1_000_000_000L

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

    /**
     * Estado de pago de cada fiado vigente del cliente (por id de movimiento). Primero los abonos
     * dirigidos a un fiado ([MovementEntity.fiadoId]) se aplican a ese fiado; lo que sobre y los
     * abonos sin fiado van del más viejo al más nuevo (FIFO). Misma cuenta que
     * calcularEstadoCliente() del backend: así la app lo muestra al instante, también sin internet.
     */
    fun pagosPorFiado(clienteId: Long, movimientos: List<MovementEntity>): Map<Long, PagoFiado> {
        val vigentes = movimientos.filter { esDeFiado(it) && it.clientId == clienteId }.sortedWith(compareBy({ it.createdAt }, { it.id }))
        val fiados = vigentes.filter { it.kind == MovementType.FIADO }
        // 1. Abonos dirigidos: a su fiado, hasta lo que ese fiado deba. El exceso pasa a la bolsa general.
        val dirigido = HashMap<Long, Long>()
        var porAplicar = 0L
        for (a in vigentes.filter { it.kind == MovementType.ABONO }) {
            val f = a.fiadoId?.let { id -> fiados.firstOrNull { it.remoteId == id } }
            if (f == null) { porAplicar += a.amount; continue }
            val cabe = (f.amount - (dirigido[f.id] ?: 0L)).coerceAtLeast(0)
            val aplicado = minOf(a.amount, cabe)
            dirigido[f.id] = (dirigido[f.id] ?: 0L) + aplicado
            porAplicar += a.amount - aplicado
        }
        // 2. Lo demás, FIFO sobre lo que quede de cada fiado.
        return fiados.associate { f ->
            val restaTrasDirigidos = f.amount - (dirigido[f.id] ?: 0L)
            val aplicado = minOf(porAplicar, restaTrasDirigidos)
            porAplicar -= aplicado
            val resta = restaTrasDirigidos - aplicado
            val estado = when {
                resta <= 0 -> EstadoPagoFiado.PAGADO
                resta < f.amount -> EstadoPagoFiado.ABONADO
                else -> EstadoPagoFiado.PENDIENTE
            }
            f.id to PagoFiado(estado, resta)
        }
    }

    /** Lo fiado menos lo abonado (fiados y abonos vigentes). Negativo = abonó de más. */
    fun balance(clienteId: Long, movimientos: List<MovementEntity>): Long =
        movimientos.filter { esDeFiado(it) && it.clientId == clienteId }
            .sumOf { if (it.kind == MovementType.FIADO) it.amount else -it.amount }

    /** Cuánto más se le puede fiar sin pasar su cupo. */
    fun disponible(f: FiadoPendiente): Long = (f.cliente.creditLimit - f.saldo).coerceAtLeast(0)

    /** Debe más que su cupo (lo calculamos aquí o lo marcó el servidor). */
    fun pasoElLimite(f: FiadoPendiente): Boolean = f.saldo > f.cliente.creditLimit || f.cliente.overLimit

    /** Llegó a su cupo: ya no se le puede fiar más. */
    fun enElLimite(f: FiadoPendiente): Boolean = f.saldo >= f.cliente.creditLimit || f.cliente.overLimit

    /**
     * Aviso de cupo al fiar: null si [monto] cabe en el cupo. La app no deja pasar del límite y le
     * dice al vendedor o al administrador por qué.
     */
    fun problemaCupo(nombre: String, saldo: Long, monto: Long, cupo: Long): String? = when {
        saldo + monto <= cupo -> null
        saldo >= cupo -> "$nombre ya llegó a su límite de fiado (${Money.cop(cupo)}). Para fiarle más, que abone o que un administrador le suba el cupo."
        else -> "$nombre debe ${Money.cop(saldo)} y su límite es ${Money.cop(cupo)}: le puedes fiar hasta ${Money.cop(cupo - saldo)}."
    }

    /** Cambiar el cupo (solo administrador): nunca por debajo del cupo por defecto. "" = sin cambios. */
    fun problemaNuevoCupo(actual: Long, nuevo: Long): String? = when {
        nuevo < CUPO_POR_DEFECTO -> "El cupo no puede ser menor a ${Money.cop(CUPO_POR_DEFECTO)}"
        nuevo > CUPO_MAXIMO -> "El cupo no puede pasar de ${Money.cop(CUPO_MAXIMO)}"
        nuevo == actual -> ""
        else -> null
    }

    /**
     * Formulario del cliente: nombre obligatorio, celular obligatorio (7 a 10 dígitos) y dirección
     * opcional (hasta 200 letras). Igual que firestore.rules → clienteValido().
     */
    fun problemasCliente(nombre: String, celular: String, direccion: String): Map<String, String> = buildMap {
        if (nombre.trim().length < 2) put("nombre", "Escribe el nombre del cliente")
        if (celular.isEmpty()) put("celular", "Escribe el celular del cliente")
        else if (!celular.all(Char::isDigit) || celular.length !in 7..10) put("celular", "El celular debe tener entre 7 y 10 dígitos")
        if (direccion.trim().length > 200) put("direccion", "La dirección puede tener hasta 200 letras")
    }

    /**
     * Editar el valor de un fiado: no puede quedar por debajo de lo que el cliente ya abonó, ni
     * pasar su cupo si sube. [balance] es lo fiado menos lo abonado antes del cambio.
     */
    fun problemaEditarFiado(nombre: String, actual: Long, nuevo: Long, balance: Long, cupo: Long): String? {
        if (nuevo <= 0) return "Escribe un monto mayor a cero"
        val despues = balance - actual + nuevo
        if (despues < 0) return "$nombre ya abonó más que eso: este fiado no puede quedar en menos de ${Money.cop(actual - balance)}"
        if (nuevo > actual && despues > cupo) return problemaCupo(nombre, balance - actual, nuevo, cupo)
        return null
    }

    /** Editar el valor de un abono: no puede abonar más de lo que debe. */
    fun problemaEditarAbono(actual: Long, nuevo: Long, balance: Long): String? = when {
        nuevo <= 0 -> "Escribe un monto mayor a cero"
        balance + actual - nuevo < 0 -> "No puede abonar más de lo que debe (${Money.cop(balance + actual)})"
        else -> null
    }

    /**
     * Anular un fiado registrado por error: si ya tiene abonos aplicados, el cliente quedaría con
     * saldo a favor. Primero se corrigen o anulan esos abonos.
     */
    fun problemaAnularFiado(monto: Long, balance: Long): String? =
        if (balance - monto < 0) "Este fiado ya tiene abonos. Primero corrige o anula los abonos del cliente." else null

    /** Estado de cuenta de todos los clientes, también los que no deben nada (saldo 0). */
    fun cuentas(clientes: List<Cliente>, movimientos: List<MovementEntity>): List<FiadoPendiente> {
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
    fun pendientes(clientes: List<Cliente>, movimientos: List<MovementEntity>): List<FiadoPendiente> =
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

    /** Abonar a un solo fiado: null si cabe. Con 0 no hay mensaje (el botón solo queda apagado). */
    fun problemaAbonoAFiado(monto: Long, porPagar: Long): String? = when {
        monto <= 0 -> ""
        porPagar <= 0 -> "Este fiado ya está pagado"
        monto > porPagar -> "De este fiado solo faltan ${Money.cop(porPagar)}. Para abonar más, abona a toda la deuda."
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
