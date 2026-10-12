package com.barriogo.app.ai

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class InvoiceItem(val name: String, val qty: Int, val amount: Long)

enum class InvoiceIssue { UNREADABLE, SUPPLIER_MISSING, SUM_MISMATCH, DATE_MISSING }

/** Lo que el agente entendió de una factura de proveedor. */
data class InvoiceDraft(
    val supplier: String?,
    val number: String?,
    val dateMillis: Long?,
    val total: Long,
    val items: List<InvoiceItem>,
) {
    val itemsSum: Long get() = items.sumOf { it.amount }

    val issues: Set<InvoiceIssue>
        get() {
            val s = HashSet<InvoiceIssue>()
            if (total <= 0 && items.isEmpty()) s.add(InvoiceIssue.UNREADABLE)
            if (supplier.isNullOrBlank()) s.add(InvoiceIssue.SUPPLIER_MISSING)
            if (dateMillis == null) s.add(InvoiceIssue.DATE_MISSING)
            if (items.isNotEmpty() && total > 0 && itemsSum != total) s.add(InvoiceIssue.SUM_MISMATCH)
            return s
        }

    /** Diferencia entre el total de la factura y la suma del detalle (positiva = falta detalle). */
    val missing: Long get() = total - itemsSum
}

/**
 * Convierte el texto que devuelve el OCR (ML Kit) en una propuesta de gasto de proveedor.
 * Es una heurística de reglas: proveedor (primeras líneas), número, fecha, líneas "concepto ... valor" y total.
 */
object InvoiceAgent {
    private val amountRe = Regex("\\d{1,3}(?:[.,]\\d{3})+|\\d+")
    private val decimalsRe = Regex(",\\d{2}(?!\\d)")
    private val headerRe = Regex("nit|factura|fecha|tel|cel|direcci|cliente|www|http|@|regimen|resoluci|cajero|hora|nro|numero", RegexOption.IGNORE_CASE)
    private val skipItemRe = Regex("total|iva|cambio|efectivo|nit|factura|fecha|tel|gracias|cajero|hora|descuento|propina|recibido|tarjeta|sub", RegexOption.IGNORE_CASE)
    private val companyRe = Regex("tienda|distribuidora|s\\.?a\\.?s|ltda|drogueria|panader|ferreter|comercial|supermercado|mini|abarrotes", RegexOption.IGNORE_CASE)
    private val itemRe = Regex("^(.+?)(?:\\s+[xX]\\s*(\\d+))?\\s+\\$?\\s*(\\d{1,3}(?:[.,]\\d{3})+|\\d+)\\s*$")
    private val trailingPricesRe = Regex("(\\s+\\$?\\d{1,3}(?:[.,]\\d{3})+|\\s+\\d{3,})+$")
    private val leadQtyRe = Regex("^(\\d{1,2})\\s+(.+)$")
    private val numberRe = Regex(
        "factura(?:\\s+de\\s+venta)?[^0-9A-Za-z]{0,10}(?:no\\.?|n[°º]|nro\\.?|numero)?[^0-9A-Za-z]{0,6}([A-Za-z]{0,3}-?\\d{3,10})",
        RegexOption.IGNORE_CASE,
    )
    private val dateDmy = Regex("(\\d{1,2})[/\\-.](\\d{1,2})[/\\-.](\\d{4})")
    private val dateYmd = Regex("(\\d{4})[/\\-](\\d{1,2})[/\\-](\\d{1,2})")
    private val dateText = Regex("(\\d{1,2})\\s+([A-Za-z]{3,4})\\.?\\s+(\\d{4})")
    private val dateLineRe = Regex("\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{4}|\\d{4}[/\\-]\\d{1,2}[/\\-]\\d{1,2}|\\d{1,2}\\s+[A-Za-z]{3,4}\\.?\\s+\\d{4}")
    private val timeRe = Regex("(\\d{1,2}):(\\d{2})")
    private val months = mapOf(
        "ene" to 1, "feb" to 2, "mar" to 3, "abr" to 4, "may" to 5, "jun" to 6, "jul" to 7, "ago" to 8,
        "sep" to 9, "sept" to 9, "oct" to 10, "nov" to 11, "dic" to 12,
    )

    fun amounts(line: String): List<Long> {
        val clean = line.replace(decimalsRe, "")
        return amountRe.findAll(clean).map { it.value.replace(Regex("[.,]"), "").toLong() }.toList()
    }

    private fun parseDate(lines: List<String>): Long? {
        for (l in lines) {
            var d: LocalDate? = null
            val a = dateDmy.find(l)
            if (a != null) d = safeDate(a.groupValues[3].toInt(), a.groupValues[2].toInt(), a.groupValues[1].toInt())
            if (d == null) {
                val b = dateYmd.find(l)
                if (b != null) d = safeDate(b.groupValues[1].toInt(), b.groupValues[2].toInt(), b.groupValues[3].toInt())
            }
            if (d == null) {
                val c = dateText.find(l)
                if (c != null) {
                    val m = months[Txt.stripAccents(c.groupValues[2].lowercase())]
                    if (m != null) d = safeDate(c.groupValues[3].toInt(), m, c.groupValues[1].toInt())
                }
            }
            if (d != null) {
                val t = timeRe.find(l)
                val time = if (t != null) safeTime(t.groupValues[1].toInt(), t.groupValues[2].toInt()) else LocalTime.NOON
                return LocalDateTime.of(d, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
        return null
    }

    private fun safeDate(y: Int, m: Int, d: Int): LocalDate? = try { LocalDate.of(y, m, d) } catch (e: Exception) { null }
    private fun safeTime(h: Int, m: Int): LocalTime = try { LocalTime.of(h, m) } catch (e: Exception) { LocalTime.NOON }

    private fun titleCase(s: String): String =
        s.split(" ").joinToString(" ") { w -> if (w.contains('.')) w else w.lowercase().replaceFirstChar { it.uppercase() } }

    fun parse(text: String): InvoiceDraft {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var total = 0L
        val countKeywords = Regex("item|articul|cant|prod|unid|pieza|paquete", RegexOption.IGNORE_CASE)
        val totalKeywords = Regex("total|pagar|neto|importe|valor\\s+a|gran\\s+total", RegexOption.IGNORE_CASE)

        for (i in lines.indices) {
            val l = lines[i]
            val ln = Txt.stripAccents(l.lowercase())

            // Descartar líneas de conteo como "TOTAL ITEMS: 1" o "TOTAL ARTICULOS: 1"
            if (countKeywords.containsMatchIn(ln)) continue

            if (totalKeywords.containsMatchIn(ln) && !ln.contains("subtotal")) {
                // 1. Buscar montos en la misma línea
                val a = amounts(l).filter { it >= 100 }
                if (a.isNotEmpty()) {
                    total = maxOf(total, a.max())
                } else if (i + 1 < lines.size) {
                    // 2. Si la palabra "TOTAL" está sola, revisar la línea inmediatamente siguiente
                    val nextL = lines[i + 1]
                    val nextLn = Txt.stripAccents(nextL.lowercase())
                    if (!countKeywords.containsMatchIn(nextLn)) {
                        val nextA = amounts(nextL).filter { it >= 100 }
                        if (nextA.isNotEmpty()) {
                            total = maxOf(total, nextA.max())
                        }
                    }
                }
            }
        }

        // Si no se encontró total explícito con palabra clave, buscar el monto más alto en el documento >= 500
        if (total == 0L) {
            val allAmounts = lines.flatMap { l ->
                val ln = Txt.stripAccents(l.lowercase())
                if (countKeywords.containsMatchIn(ln)) emptyList() else amounts(l)
            }.filter { it >= 500 }
            if (allAmounts.isNotEmpty()) {
                total = allAmounts.max()
            }
        }

        val number = numberRe.find(text)?.groupValues?.get(1)
        val date = parseDate(lines)

        var supplier: String? = null
        val cands = lines.take(6).filter { l ->
            l.count { it.isLetter() } >= 3 && !headerRe.containsMatchIn(l) && l.count { it.isDigit() } <= l.length / 2
        }
        val preferred = cands.firstOrNull { companyRe.containsMatchIn(it) }
        supplier = preferred ?: cands.firstOrNull()
        if (supplier != null && supplier == supplier.uppercase()) supplier = titleCase(supplier)

        val items = ArrayList<InvoiceItem>()
        for (l in lines) {
            if (skipItemRe.containsMatchIn(Txt.stripAccents(l)) || dateLineRe.containsMatchIn(l)) continue
            val m = itemRe.find(l) ?: continue
            var name = m.groupValues[1].trim()
            var qty = if (m.groupValues[2].isNotEmpty()) m.groupValues[2].toInt() else 1
            val amt = m.groupValues[3].replace(Regex("[.,]"), "").toLong()
            if (amt < 100) continue
            name = name.replace(trailingPricesRe, "").trim()
            val lead = leadQtyRe.find(name)
            if (lead != null && m.groupValues[2].isEmpty()) {
                qty = lead.groupValues[1].toInt()
                name = lead.groupValues[2]
            }
            if (name.count { it.isLetter() } < 2) continue
            items.add(InvoiceItem(name.lowercase().replaceFirstChar { it.uppercase() }, qty, amt))
        }

        if (total == 0L && items.isNotEmpty()) total = items.sumOf { it.amount }
        return InvoiceDraft(supplier, number, date, total, items)
    }
}
