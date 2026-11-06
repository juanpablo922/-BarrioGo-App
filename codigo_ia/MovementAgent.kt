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
    val valores: List<Long> = emptyList(), // lo que suma [amount] cuando se dijeron varios valores
) {
    val itemText: String get() = ItemsText.join(items)

    /** Con varios valores: ("Leche", 15000), ("Agua", 25000). Sin producto si no hay uno por cada valor. */
    val desglose: List<Pair<String?, Long>>
        get() = if (valores.size < 2) emptyList()
        else valores.mapIndexed { i, v -> (if (items.size == valores.size) items[i] else null) to v }

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
    private val GASTO_WORDS = setOf("gasto", "gastos", "gastamos", "gaste", "pague", "pagamos", "pago", "compra", "compramos", "compre", "salida", "salio", "egreso")
    private val SALE_TRIGGERS = setOf("vendi", "vendimos", "vendio", "venta", "ventas", "vendida", "vendido", "ingreso", "ingresos", "entro")
    /** Dichas en la frase mandan sobre la pestaña elegida. "pago" o "compra" no: también aparecen en ventas y fiados. */
    private val SALE_SAID = SALE_TRIGGERS - "entro"
    private val GASTO_SAID = setOf("gasto", "gastos", "gastamos", "gaste")
    private val ADD_WORDS = setOf("agrega", "agregale", "agregar", "anade", "anadele", "suma", "sumale", "ponle", "mas")
    private val TRANSFER_WORDS = setOf("transferencia", "nequi", "daviplata", "bancolombia", "pse", "tarjeta")
    private val CLIENT_DELIMITERS = setOf(
        "en", "de", "del", "por", "valen", "vale", "costo", "costaron", "cuesta", "pesos", "peso", "mil",
        "un", "una", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez",
        "fio", "fiado", "fiar", "cobre", "cobrar", "llevo", "lleva", "llevaron", "compro", "compraron",
        "debe", "pidio", "pidieron", "cuanto", "cuantos", "se", "y",
    )
    private val CLIENT_TITLES = setOf("la", "el", "don", "dona", "sr", "sra", "senor", "senora")
    private val NOT_A_CLIENT = setOf("domicilio", "contado", "llevar", "buen", "precio", "mitad", "cuenta", "credito", "nombre")
    private val BUY_VERBS = setOf("compro", "compraron", "llevo", "lleva", "llevaron")
    private val DECLARED_TOTAL = setOf("total", "son", "eran", "fueron", "quedaron")

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
     * Los valores de la frase, uno por cada cosa: "15 mil de leche y 25 mil de agua" son dos y se suman.
     * Solo cuenta uno cuando se dice el total ("..., son 40 mil"), cuando el segundo corrige al primero sin
     * otro producto en medio ("2 arroz de 5 mil por 10 mil"), y no cuentan el billete con que pagaron
     * ("con 50 mil") ni el precio de cada uno ("5 mil cada uno").
     */
    private fun amountSpans(tokens: List<String>, spans: List<NumSpan>): List<NumSpan> {
        val money = spans.filter { s ->
            (s.magnitude || s.value >= 100) && tokens.getOrNull(s.start - 1) != "con" && tokens.getOrNull(s.end) != "cada"
        }
        if (money.size < 2) return listOfNotNull(money.firstOrNull() ?: SpanishNumbers.pickAmount(spans, tokens))
        val declared = money.filter { s ->
            tokens.getOrNull(s.start - 1) in DECLARED_TOTAL || tokens.getOrNull(s.end) == "total" ||
                (tokens.getOrNull(s.end) == "en" && tokens.getOrNull(s.end + 1) == "total")
        }
        if (declared.size == 1) return declared
        val lines = ArrayList<NumSpan>()
        for (s in money) {
            val prev = lines.lastOrNull()
            val sameLine = prev != null && prev.end < s.start && (prev.end until s.start).all { tokens[it] in STOP && tokens[it] != "mas" }
            if (sameLine) lines[lines.lastIndex] = s else lines.add(s)
        }
        return lines
    }

    /** Palabras que dicen el tipo al empezar ("Gasto de...", "Ingreso de..."): no son parte del concepto. */
    private fun leadingTrigger(tokens: List<String>, exclude: Set<Int>, triggers: Set<String>): Set<Int> {
        val first = tokens.indices.firstOrNull { it !in exclude && tokens[it] !in STOP } ?: return emptySet()
        return if (tokens[first] in triggers) setOf(first) else emptySet()
    }

    private class HeardClient(val client: ClientRef?, val heard: String?, val indices: Collection<Int>)

    private fun clientAt(start: Int, tokens: List<String>, clients: List<ClientRef>, exclude: Set<Int>): HeardClient? {
        ClientMatcher.matchWindow(tokens, start, clients, exclude, CLIENT_DELIMITERS)?.let { return HeardClient(it.first, null, it.second) }
        val window = ArrayList<Int>()
        var k = start
        while (k < tokens.size && window.size < 3 && k !in exclude && tokens[k] != "," && tokens[k] !in CLIENT_DELIMITERS && tokens[k] !in STOP) {
            window.add(k)
            k++
        }
        if (window.isEmpty()) return null
        return HeardClient(null, window.joinToString(" ") { Txt.cap(tokens[it]) }, window)
    }

    /** ¿A quién se le vendió? "... a Luisa Castro", "... para don Juan" o "Luisa Castro compró ...". */
    private fun saleClient(tokens: List<String>, clients: List<ClientRef>, exclude: Set<Int>): HeardClient? {
        for (i in tokens.indices) {
            val t = tokens[i]
            if (t != "para" && (i == 0 || (t != "a" && t != "al"))) continue
            var start = i + 1
            while (tokens.getOrNull(start) in CLIENT_TITLES) start++
            val first = tokens.getOrNull(start) ?: continue
            if (start in exclude || first == "," || first in STOP || first in NOT_A_CLIENT) continue
            val found = clientAt(start, tokens, clients, exclude) ?: continue
            return HeardClient(found.client, found.heard, found.indices + (i + 1 until start))
        }
        val verb = tokens.indexOfFirst { it in BUY_VERBS }
        val nameEnd = if (verb > 0 && tokens[verb - 1] == "se") verb - 1 else verb
        if (nameEnd in 1..3 && (0 until nameEnd).all { it !in exclude && tokens[it] != "," && tokens[it] !in STOP }) {
            return clientAt(0, tokens, clients, exclude)
        }
        return null
    }

    /**
     * Interpreta una frase. [mode] es lo que el usuario eligió en la pantalla (Venta, Gasto o Fiado),
     * pero lo que dice la frase manda: "fiado", "gasto" o "venta" cambian el tipo.
     */
    fun parse(text: String, mode: MovementType, clients: List<ClientRef>): AgentResult {
        val tokens = Txt.tokenize(text)
        if (tokens.isEmpty()) return AgentResult.NotUnderstood("No se escuchó nada")

        // "tarjeta de crédito" es un medio de pago, no un fiado
        val saidFiado = tokens.any { it in FIADO_WORDS && !(it == "credito" && "tarjeta" in tokens) }
        val saidOther = tokens.firstOrNull { it in SALE_SAID || it in GASTO_SAID }
        val isFiado = saidFiado || (saidOther == null && mode == MovementType.FIADO)
        val isGasto = !isFiado && (
            if (saidOther != null) saidOther in GASTO_SAID else mode == MovementType.GASTO || tokens.any { it in GASTO_WORDS }
        )

        val spans = SpanishNumbers.find(tokens)
        val amounts = amountSpans(tokens, spans)
        val exclude = HashSet<Int>()
        for (s in spans) for (k in s.start until s.end) exclude.add(k)
        val pay = if (tokens.any { it in TRANSFER_WORDS }) PayMethod.TRANSFERENCIA else PayMethod.EFECTIVO
        val amount = amounts.sumOf { it.value }
        val valores = amounts.map { it.value }

        // ---- Caso Gasto ----
        if (isGasto) {
            val hasGastoSignal = spans.isNotEmpty() || tokens.any { it in GASTO_WORDS }
            if (!hasGastoSignal) return AgentResult.NotUnderstood("No parece un gasto")
            val words = splitItems(tokens, exclude + leadingTrigger(tokens, exclude, GASTO_WORDS))
            var desc = words.map { Txt.cap(it) }
            if (desc.isEmpty()) desc = listOf("Gasto de tienda")
            return AgentResult.Ok(MovementDraft(MovementType.GASTO, 1, desc, amount, payMethod = pay, valores = valores))
        }

        // ---- Caso Venta / Ingreso ----
        if (!isFiado) {
            val hasSignal = spans.isNotEmpty() || tokens.any { it in SALE_TRIGGERS }
            if (!hasSignal) return AgentResult.NotUnderstood("No parece una venta")
            var qty = 1
            for (s in spans) {
                if (s in amounts) continue
                if (s.value in 1L..999L) { qty = s.value.toInt(); break }
            }
            val buyer = saleClient(tokens, clients, exclude)
            var items = splitItems(tokens, exclude + leadingTrigger(tokens, exclude, SALE_TRIGGERS) + buyer?.indices.orEmpty())
            if (qty > 1) items = items.map { if (it.contains(" ")) it else singular(it) }
            var names = items.map { Txt.cap(it) }
            if (names.isEmpty() && amount > 0) names = listOf("Venta")
            if (names.isEmpty() && amount == 0L) return AgentResult.NotUnderstood("No entendí qué se vendió")
            return AgentResult.Ok(
                MovementDraft(
                    MovementType.VENTA, qty, names, amount,
                    clientId = buyer?.client?.id,
                    clientName = buyer?.client?.name ?: buyer?.heard,
                    heardClient = buyer?.heard,
                    payMethod = pay,
                    valores = valores,
                )
            )
        }

        // ---- Fiado: ¿a quién? ----
        val ACTION_VERBS = setOf("se", "llevo", "lleva", "llevaron", "debe", "pidio", "compro", "compraron", "fio", "fiar")

        var candIdx = -1

        // Heurística 1: Preposiciones explícitas de cliente ("a", "para", "al", o "de" tras "fiado")
        for (i in tokens.indices) {
            val t = tokens[i]
            val isDeClient = t == "de" && i > 0 && tokens[i - 1] in FIADO_WORDS
            val cond = (t == "para" || t == "a" || t == "al" || isDeClient) && i + 1 < tokens.size && i > 0 || (t == "para" && i + 1 < tokens.size)
            if (cond && (i + 1) !in exclude && tokens[i + 1] != ",") { candIdx = i + 1; break }
        }

        // Heurística 2: Tras palabras gatillo de fiado (ej. "fiado Mateo Ramírez...")
        if (candIdx < 0) {
            for (i in tokens.indices) {
                if (tokens[i] in FIADO_WORDS && i + 1 < tokens.size && (i + 1) !in exclude) {
                    candIdx = i + 1
                    break
                }
            }
        }

        // Heurística 3: Verbos de acción al medio (ej. "Laura Estefanía se llevó 14 mil...")
        if (candIdx < 0) {
            for (i in tokens.indices) {
                if (tokens[i] in ACTION_VERBS && i > 0 && 0 !in exclude) {
                    candIdx = 0
                    break
                }
            }
        }

        // Heurística 4: Modo Fiado activo y frase directa (ej. "Laura Estefanía 14mil de pollo")
        if (candIdx < 0 && (mode == MovementType.FIADO || tokens.any { it in FIADO_WORDS })) {
            for (i in tokens.indices) {
                if (i !in exclude && tokens[i] != "," && tokens[i] !in STOP) {
                    candIdx = i
                    break
                }
            }
        }

        val nameIdx = HashSet<Int>()
        var client: ClientRef? = null
        var heard: String? = null

        if (candIdx >= 0) {
            val matchResult = ClientMatcher.matchWindow(tokens, candIdx, clients, exclude, CLIENT_DELIMITERS)
            if (matchResult != null) {
                client = matchResult.first
                nameIdx.addAll(matchResult.second)
            } else {
                // No coincidió con ningún cliente existente: capturar el Nombre + Apellido completo
                val windowIndices = ArrayList<Int>()
                var k = candIdx
                while (k < tokens.size && windowIndices.size < 3 && k !in exclude &&
                    tokens[k] != "," && tokens[k] !in CLIENT_DELIMITERS
                ) {
                    windowIndices.add(k)
                    k++
                }
                if (windowIndices.isNotEmpty()) {
                    heard = windowIndices.joinToString(" ") { Txt.cap(tokens[it]) }
                    nameIdx.addAll(windowIndices)
                }
            }
        } else {
            // Escaneo general por si el nombre está en cualquier otra posición
            for (i in tokens.indices) {
                if (i in exclude || tokens[i] in STOP || tokens[i] == ",") continue
                val matchResult = ClientMatcher.matchWindow(tokens, i, clients, exclude, CLIENT_DELIMITERS)
                if (matchResult != null) {
                    client = matchResult.first
                    nameIdx.addAll(matchResult.second)
                    break
                }
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
                valores = valores,
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
