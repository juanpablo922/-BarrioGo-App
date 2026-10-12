package com.barriogo.app.ai

import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod

/** Propuesta de movimiento armada por el agente a partir de lo que dijo el tendero. */
data class MovementDraft(
    val type: MovementType,              // VENTA o FIADO
    val quantity: Int = 1,               // solo ventas: "2 × Gaseosa"
    val items: List<String> = emptyList(),
    val amount: Long = 0,
    val clientId: Long? = null,
    val clientName: String? = null,      // nombre del cliente existente o el que se entendió
    val heardClient: String? = null,     // nombre entendido que NO está en la lista de clientes
    val payMethod: PayMethod = PayMethod.EFECTIVO,
) {
    val itemText: String get() = ItemsText.join(items)

    /** Problemas que impiden registrar sin corregir (el agente pregunta en vez de adivinar). */
    val issues: Set<DraftIssue>
        get() {
            val s = HashSet<DraftIssue>()
            if (amount <= 0) s.add(DraftIssue.AMOUNT_MISSING)
            if (type == MovementType.FIADO && clientId == null) s.add(DraftIssue.CLIENT_UNKNOWN)
            if (type == MovementType.FIADO && items.isEmpty()) s.add(DraftIssue.ITEM_MISSING)
            return s
        }
}

enum class DraftIssue { AMOUNT_MISSING, CLIENT_UNKNOWN, ITEM_MISSING }

sealed interface AgentResult {
    data class Ok(val draft: MovementDraft) : AgentResult
    data class NotUnderstood(val reason: String) : AgentResult
}

object ItemsText {
    /** ["arroz","leche","panela"] -> "arroz, leche y panela" */
    fun join(items: List<String>): String = when {
        items.isEmpty() -> ""
        items.size == 1 -> items[0]
        else -> items.dropLast(1).joinToString(", ") + " y " + items.last()
    }
}

/**
 * Agente de captura de movimientos (parte "interpreta" y "valida" del ciclo percepción-decisión-acción).
 * Enfoque: extracción de información con reglas + coincidencia aproximada de nombres.
 * No guarda nada: solo propone. La confirmación siempre la da el usuario.
 */
object MovementAgent {
    private val STOP = setOf(
        "vendi", "vendimos", "vendio", "vendida", "vendido", "venta", "ventas", "se", "fue", "fueron", "eran", "era",
        "son", "es", "por", "de", "del", "la", "el", "los", "las", "en", "a", "al", "para", "le", "les", "fio", "fie",
        "fiado", "fiar", "fiale", "fiamos", "fiada", "credito", "llevo", "lleva", "llevaron", "compro", "compraron",
        "agrega", "agregale", "agregar", "anade", "anadele", "suma", "sumale", "ponle", "mas", "total", "pesos", "peso",
        "cobre", "cobrar", "valen", "vale", "costo", "costaron", "cuesta", "registra", "registrar", "anota", "anotar",
        "anotame", "hoy", "yo", "me", "transferencia", "nequi", "daviplata", "bancolombia", "pse", "tarjeta", "cambia",
        "cambiale", "queda", "quedo", "debe", "don", "dona", "sr", "sra",
    )
    private val FIADO_WORDS = setOf("fiado", "fiar", "fio", "fie", "fiale", "fiamos", "fiada", "credito")
    private val GASTO_WORDS = setOf("gasto", "gastos", "gastamos", "pague", "pagamos", "pago", "compra", "compramos", "salida", "salio", "egreso")
    private val SALE_TRIGGERS = setOf("vendi", "vendimos", "vendio", "venta", "ventas", "vendida", "vendido", "ingreso", "ingresos", "entro")
    private val ADD_WORDS = setOf("agrega", "agregale", "agregar", "anade", "anadele", "suma", "sumale", "ponle", "mas")
    private val TRANSFER_WORDS = setOf("transferencia", "nequi", "daviplata", "bancolombia", "pse", "tarjeta")

    /** "gaseosas" -> "gaseosa", "panes" -> "pan", "tomates" -> "tomate". */
    fun singular(w: String): String {
        if (w.length > 4 && w.endsWith("es") && w[w.length - 3] in "nrlzdj") return w.dropLast(2)
        return if (w.length > 3 && w.endsWith("s") && !w.endsWith("ss")) w.dropLast(1) else w
    }

    private fun isItemToken(tokens: List<String>, i: Int, exclude: Set<Int>): Boolean {
        val t = tokens[i]
        return i !in exclude && t !in STOP && t != "," && t != "y" && !SpanishNumbers.isDigits(t)
    }

    /** Agrupa las palabras que quedan en productos: "arroz y leche" -> ["arroz","leche"]; "libra de arroz" queda junto. */
    private fun splitItems(tokens: List<String>, exclude: Set<Int>): List<String> {
        val groups = ArrayList<String>()
        var cur = ArrayList<String>()
        for (i in tokens.indices) {
            val t = tokens[i]
            if ((t == "de" || t == "del") && cur.isNotEmpty() && i + 1 < tokens.size && isItemToken(tokens, i + 1, exclude)) {
                cur.add(t)
                continue
            }
            if (i in exclude || t in STOP || SpanishNumbers.isDigits(t)) continue
            if (t == "," || t == "y") {
                if (cur.isNotEmpty()) { groups.add(cur.joinToString(" ")); cur = ArrayList() }
                continue
            }
            cur.add(t)
        }
        if (cur.isNotEmpty()) groups.add(cur.joinToString(" "))
        return groups
    }

    /**
     * Interpreta una frase. [mode] es lo que el usuario eligió en la pantalla (Venta, Gasto o Fiado);
     * si dice "fiado", "gasto", "ingreso", el agente se adapta al tipo correspondiente.
     */
    fun parse(text: String, mode: MovementType, clients: List<ClientRef>): AgentResult {
        val tokens = Txt.tokenize(text)
        if (tokens.isEmpty()) return AgentResult.NotUnderstood("No se escuchó nada")

        val isFiado = mode == MovementType.FIADO || tokens.any { it in FIADO_WORDS }
        val isGasto = !isFiado && (mode == MovementType.GASTO || tokens.any { it in GASTO_WORDS })

        val spans = SpanishNumbers.find(tokens)
        val amountSpan = SpanishNumbers.pickAmount(spans, tokens)
        val exclude = HashSet<Int>()
        for (s in spans) for (k in s.start until s.end) exclude.add(k)
        val pay = if (tokens.any { it in TRANSFER_WORDS }) PayMethod.TRANSFERENCIA else PayMethod.EFECTIVO
        val amount = amountSpan?.value ?: 0L

        // ---- Caso Gasto ----
        if (isGasto) {
            val hasGastoSignal = spans.isNotEmpty() || tokens.any { it in GASTO_WORDS }
            if (!hasGastoSignal) return AgentResult.NotUnderstood("No parece un gasto")
            val words = splitItems(tokens, exclude)
            var desc = words.map { Txt.cap(it) }
            if (desc.isEmpty()) desc = listOf("Gasto de tienda")
            return AgentResult.Ok(MovementDraft(MovementType.GASTO, 1, desc, amount, payMethod = pay))
        }

        // ---- Caso Venta / Ingreso ----
        if (!isFiado) {
            val hasSignal = spans.isNotEmpty() || tokens.any { it in SALE_TRIGGERS }
            if (!hasSignal) return AgentResult.NotUnderstood("No parece una venta")
            var qty = 1
            for (s in spans) {
                if (s === amountSpan) continue
                if (s.value in 1L..999L) { qty = s.value.toInt(); break }
            }
            var items = splitItems(tokens, exclude)
            if (qty > 1) items = items.map { if (it.contains(" ")) it else singular(it) }
            var names = items.map { Txt.cap(it) }
            if (names.isEmpty() && amount > 0) names = listOf("Venta")
            if (names.isEmpty() && amount == 0L) return AgentResult.NotUnderstood("No entendí qué se vendió")
            return AgentResult.Ok(MovementDraft(MovementType.VENTA, qty, names, amount, payMethod = pay))
        }

        // ---- Fiado: ¿a quién? ----
        var candIdx = -1
        for (i in tokens.indices) {
            val t = tokens[i]
            val cond = (t == "para" || t == "a" || t == "al") && i + 1 < tokens.size && i > 0 || (t == "para" && i + 1 < tokens.size)
            if (cond && (i + 1) !in exclude && tokens[i + 1] != ",") { candIdx = i + 1; break }
        }
        val nameIdx = HashSet<Int>()
        var client: ClientRef? = null
        var heard: String? = null
        if (candIdx >= 0) {
            val window = ArrayList<Int>()
            var k = candIdx
            while (k < tokens.size && window.size < 2 && tokens[k] != "," && k !in exclude &&
                tokens[k] != "en" && tokens[k] != "de" && tokens[k] != "por"
            ) { window.add(k); k += 1 }
            for (w in window) {
                val c = ClientMatcher.matchToken(tokens[w], clients)
                if (c != null) { client = c; nameIdx.add(w); break }
            }
            if (client == null && window.isNotEmpty()) {
                heard = Txt.cap(tokens[window[0]])
                nameIdx.add(window[0])
            }
        } else {
            for (i in tokens.indices) {
                val t = tokens[i]
                if (i in exclude || t in STOP || t == ",") continue
                val c = ClientMatcher.matchToken(t, clients)
                if (c != null) { client = c; nameIdx.add(i); break }
            }
        }
        val hasSignal = spans.isNotEmpty() || client != null || candIdx >= 0 || tokens.any { it in FIADO_WORDS }
        if (!hasSignal) return AgentResult.NotUnderstood("No parece un fiado")
        val itemNames = splitItems(tokens, exclude + nameIdx).map { Txt.cap(it) }
        return AgentResult.Ok(
            MovementDraft(
                type = MovementType.FIADO,
                items = itemNames,
                amount = amount,
                clientId = client?.id,
                clientName = client?.name ?: heard,
                heardClient = if (client == null) heard else null,
                payMethod = pay,
            )
        )
    }

    /**
     * Edición por voz ("eran 3 gaseosas, 12 mil", "agrégale una panela, son 31 mil"):
     * solo cambia lo que se dijo; el resto queda igual.
     */
    fun applyEdit(current: MovementDraft, text: String): MovementDraft {
        val tokens = Txt.tokenize(text)
        if (tokens.isEmpty()) return current
        val spans = SpanishNumbers.find(tokens)
        val amountSpan = SpanishNumbers.pickAmount(spans, tokens)
        val exclude = HashSet<Int>()
        for (s in spans) for (k in s.start until s.end) exclude.add(k)
        var d = current
        if (amountSpan != null) d = d.copy(amount = amountSpan.value)
        val words = splitItems(tokens, exclude)
        if (current.type == MovementType.VENTA) {
            for (s in spans) {
                if (s === amountSpan) continue
                if (s.value in 1L..999L) { d = d.copy(quantity = s.value.toInt()); break }
            }
            if (words.isNotEmpty()) {
                d = d.copy(items = words.map { if (d.quantity > 1 && !it.contains(" ")) Txt.cap(singular(it)) else Txt.cap(it) })
            }
        } else if (words.isNotEmpty()) {
            val added = words.map { Txt.cap(it) }
            d = if (tokens.any { it in ADD_WORDS }) d.copy(items = current.items + added) else d.copy(items = added)
        }
        return d
    }
}
