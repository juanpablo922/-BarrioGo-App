package com.barriogo.app.domain

import com.barriogo.app.data.MovementEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

enum class Role(val label: String, val femaleLabel: String) {
    ADMIN("Administrador", "Administradora"),
    SELLER("Vendedor", "Vendedora"),
}

enum class MovementType { VENTA, GASTO, FIADO, ABONO, AJUSTE }

enum class ExpenseKind(val label: String, val detail: String) {
    PROVEEDOR("Proveedor", "Gasto proveedor"),
    SERVICIOS("Servicios", "Gasto servicios"),
    OTROS("Otros", "Gasto otros"),
}

enum class PayMethod(val label: String) {
    EFECTIVO("Efectivo"),
    TRANSFERENCIA("Transferencia"),
}

val BUSINESS_CATEGORIES = listOf("Abarrotes", "Panadería", "Farmacia", "Ferretería", "Distribuidoras", "Otros")

const val MAX_OPENING_CHANGES = 3

/** Formato de pesos colombianos sin decimales: "$ 125.000". */
object Money {
    fun group(n: Long): String {
        val s = abs(n).toString()
        val sb = StringBuilder()
        for ((i, ch) in s.withIndex()) {
            if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
            sb.append(ch)
        }
        return sb.toString()
    }

    fun cop(n: Long): String = (if (n < 0) "-" else "") + "$ " + group(n)

    fun signed(n: Long): String = (if (n < 0) "-" else "+") + "$ " + group(n)

    const val HIDDEN = "$ ••••••"
}

object Dates {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val locale = Locale("es", "CO")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val longDayFmt = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", locale)
    private val shortFullFmt = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", locale)
    private val shortDayFmt = DateTimeFormatter.ofPattern("d MMM yyyy", locale)

    fun time(millis: Long): String = Instant.ofEpochMilli(millis).atZone(zone).format(timeFmt)
    fun longDay(millis: Long = System.currentTimeMillis()): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(longDayFmt).replaceFirstChar { it.uppercase() }
    fun full(millis: Long = System.currentTimeMillis()): String = Instant.ofEpochMilli(millis).atZone(zone).format(shortFullFmt)
    fun day(millis: Long): String = Instant.ofEpochMilli(millis).atZone(zone).format(shortDayFmt)
    fun localDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    fun startOfToday(): Long = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    fun startOfDaysAgo(days: Long): Long = LocalDate.now(zone).minusDays(days).atStartOfDay(zone).toInstant().toEpochMilli()
}

// ---- Ayudas sobre MovementEntity ----
val MovementEntity.kind: MovementType get() = MovementType.valueOf(type)
val MovementEntity.pay: PayMethod get() = PayMethod.valueOf(payMethod)

/** Efecto del movimiento sobre el efectivo de la caja (fiado no mueve caja; transferencia tampoco). */
fun MovementEntity.cashDelta(): Long {
    if (voided) return 0
    return when (kind) {
        MovementType.VENTA, MovementType.ABONO -> if (pay == PayMethod.EFECTIVO) amount else 0
        MovementType.GASTO -> -amount
        MovementType.AJUSTE -> amount
        MovementType.FIADO -> 0
    }
}

/** Texto principal para mostrar: "2 × Gaseosa" o el título. */
fun MovementEntity.label(): String = if (kind == MovementType.VENTA && quantity > 1) "$quantity × $title" else title
