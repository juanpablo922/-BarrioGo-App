package com.barriogo.app.ai

import java.text.Normalizer

/** Utilidades de texto: quitar tildes, capitalizar y separar en palabras. */
object Txt {
    private val marks = Regex("\\p{Mn}+")

    fun stripAccents(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFD).replace(marks, "")

    fun cap(s: String): String = s.replaceFirstChar { it.uppercase() }

    /**
     * Pasa a minúsculas, quita tildes y signos, y separa en palabras.
     * Las comas que separan ideas se conservan como token "," (las que están dentro de un número, no).
     */
    fun tokenize(text: String): List<String> {
        var t = stripAccents(text.lowercase())
        t = t.replace("\$", " ")
        t = t.replace(Regex("[¿?¡!\"“”«»()]"), " ")
        t = t.replace(Regex("(?<!\\d),|,(?!\\d)"), " , ")
        t = t.replace(Regex("\\.(?!\\d)|(?<!\\d)\\."), " ")
        return t.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    }
}

/** Un número encontrado en la frase: [start, end) son posiciones de palabras. */
data class NumSpan(val value: Long, val start: Int, val end: Int, val magnitude: Boolean)

/**
 * Interpreta números en español hablado y escrito:
 * "8 mil", "ocho mil", "24.000", "veinticuatro mil", "mil quinientos", "cinco lucas", "8 mil 500".
 */
object SpanishNumbers {
    private val UNITS = mapOf(
        "cero" to 0, "un" to 1, "uno" to 1, "una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12,
        "trece" to 13, "catorce" to 14, "quince" to 15, "dieciseis" to 16, "diecisiete" to 17,
        "dieciocho" to 18, "diecinueve" to 19, "veinte" to 20, "veintiun" to 21, "veintiuno" to 21,
        "veintiuna" to 21, "veintidos" to 22, "veintitres" to 23, "veinticuatro" to 24, "veinticinco" to 25,
        "veintiseis" to 26, "veintisiete" to 27, "veintiocho" to 28, "veintinueve" to 29,
    )
    private val TENS = mapOf(
        "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50, "sesenta" to 60, "setenta" to 70,
        "ochenta" to 80, "noventa" to 90,
    )
    private val HUNDREDS = mapOf(
        "cien" to 100, "ciento" to 100, "doscientos" to 200, "doscientas" to 200, "trescientos" to 300,
        "trescientas" to 300, "cuatrocientos" to 400, "cuatrocientas" to 400, "quinientos" to 500,
        "quinientas" to 500, "seiscientos" to 600, "seiscientas" to 600, "setecientos" to 700,
        "setecientas" to 700, "ochocientos" to 800, "ochocientas" to 800, "novecientos" to 900,
        "novecientas" to 900,
    )
    private val THOUSAND = setOf("mil", "luca", "lucas")   // "lucas" = miles de pesos en el habla colombiana
    private val MILLION = setOf("millon", "millones")
    private val PESOS = setOf("peso", "pesos")
    private val digitsRe = Regex("\\d+([.,]\\d+)*")
    private val thousandsRe = Regex("\\d{1,3}([.,]\\d{3})+")
    private val smallDigitsRe = Regex("\\d{1,3}")

    /** Palabras que suelen anunciar el precio: "por 8 mil", "son 31 mil", "eran 12 mil". */
    val AMOUNT_KEYWORDS = setOf(
        "por", "son", "eran", "era", "fueron", "fue", "total", "valen", "vale", "costo", "costaron",
        "cuesta", "cobre", "quedaron",
    )

    fun isDigits(tok: String): Boolean = digitsRe.matches(tok)

    private fun parseDigits(tok: String): Pair<Long, Boolean> {
        if (thousandsRe.matches(tok)) return Pair(tok.replace(Regex("[.,]"), "").toLong(), true)
        return Pair(tok.replace(Regex("[.,].*"), "").toLong(), false)
    }

    /** Número menor a 1000 que sigue a "mil": "8 mil 500" o "ocho mil quinientos". Devuelve (valor, nueva posición). */
    private fun smallAfter(tokens: List<String>, j: Int): Pair<Long, Int> {
        if (j >= tokens.size) return Pair(0L, j)
        val first = tokens[j]
        if (smallDigitsRe.matches(first)) return Pair(first.toLong(), j + 1)
        var cur = 0
        var k = j
        var got = false
        while (k < tokens.size) {
            val t = tokens[k]
            if (t in UNITS) cur += UNITS.getValue(t)
            else if (t in TENS) cur += TENS.getValue(t)
            else if (t in HUNDREDS) cur += HUNDREDS.getValue(t)
            else if (t == "y" && got && k + 1 < tokens.size && tokens[k + 1] in UNITS) { k += 1; continue }
            else break
            got = true
            k += 1
        }
        return if (got && cur < 1000) Pair(cur.toLong(), k) else Pair(0L, j)
    }

    fun find(tokens: List<String>): List<NumSpan> {
        val spans = ArrayList<NumSpan>()
        val n = tokens.size
        var i = 0
        while (i < n) {
            val t = tokens[i]
            if (isDigits(t)) {
                val parsed = parseDigits(t)
                var v = parsed.first
                var mag = parsed.second
                var j = i + 1
                if (j < n && tokens[j] in THOUSAND) {
                    v *= 1000
                    mag = true
                    j += 1
                    val extra = smallAfter(tokens, j)
                    if (extra.first != 0L) { v += extra.first; j = extra.second }
                } else if (j < n && tokens[j] in MILLION) {
                    v *= 1_000_000
                    mag = true
                    j += 1
                }
                if (j < n && tokens[j] in PESOS) j += 1
                if (v >= 1000) mag = true
                spans.add(NumSpan(v, i, j, mag))
                i = j
                continue
            }
            if (t in UNITS || t in TENS || t in HUNDREDS || t in THOUSAND || t in MILLION) {
                var total = 0L
                var cur = 0
                var j = i
                var got = false
                var mag = false
                while (j < n) {
                    val w = tokens[j]
                    if (w in UNITS) cur += UNITS.getValue(w)
                    else if (w in TENS) cur += TENS.getValue(w)
                    else if (w in HUNDREDS) cur += HUNDREDS.getValue(w)
                    else if (w == "y" && got && j + 1 < n && tokens[j + 1] in UNITS && cur % 10 == 0 && cur >= 30) { j += 1; continue }
                    else if (w in THOUSAND) { total += (if (cur != 0) cur else 1) * 1000L; cur = 0; mag = true }
                    else if (w in MILLION) { total += (if (cur != 0) cur else 1) * 1_000_000L; cur = 0; mag = true }
                    else break
                    got = true
                    j += 1
                }
                if (got) {
                    val v = total + cur
                    if (j < n && tokens[j] in PESOS) j += 1
                    if (v >= 1000) mag = true
                    spans.add(NumSpan(v, i, j, mag))
                    i = j
                    continue
                }
            }
            i += 1
        }
        return spans
    }

    /**
     * Elige cuál número es el precio.
     * 1) Entre los que "parecen dinero" (usan mil/lucas o valen 100 o más) prefiere el que sigue a "por", "son", "eran"...
     * 2) Si no hay ninguno, y hay dos o más números, toma el último.
     * 3) Un solo número pequeño ("vendí 2 gaseosas") se considera cantidad, no precio: devuelve null.
     */
    fun pickAmount(spans: List<NumSpan>, tokens: List<String>): NumSpan? {
        val cands = spans.filter { it.magnitude || it.value >= 100 }
        if (cands.isNotEmpty()) {
            val kw = cands.filter { it.start > 0 && tokens[it.start - 1] in AMOUNT_KEYWORDS }
            return if (kw.isNotEmpty()) kw.last() else cands.last()
        }
        if (spans.size >= 2) return spans.last()
        return null
    }
}
