package com.barriogo.app.ai

/** Cliente mínimo que el agente necesita conocer. */
data class ClientRef(val id: Long, val name: String)

/** Coincidencia aproximada de nombres (distancia de edición), tolerante a errores de la transcripción. */
object ClientMatcher {
    const val THRESHOLD = 0.8

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1)
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            prev = cur
        }
        return prev[b.length]
    }

    /** Similitud entre 0 y 1 (1 = iguales). */
    fun similarity(a: String, b: String): Double {
        val x = Txt.stripAccents(a.lowercase().trim())
        val y = Txt.stripAccents(b.lowercase().trim())
        val m = maxOf(x.length, y.length)
        return if (m == 0) 1.0 else 1.0 - levenshtein(x, y).toDouble() / m
    }

    /** Busca a qué cliente se refiere una frase (soporta nombres sencillos y compuestos, ej: "Mateo Ramírez"). */
    fun matchPhrase(phrase: String, clients: List<ClientRef>): ClientRef? {
        val cleanPhrase = Txt.stripAccents(phrase.lowercase().trim())
        if (cleanPhrase.length < 3) return null

        var best: ClientRef? = null
        var bestScore = 0.0

        for (c in clients) {
            val cleanName = Txt.stripAccents(c.name.lowercase().trim())

            // 1. Comparar frase completa contra nombre completo
            val fullScore = similarity(cleanPhrase, cleanName)
            if (fullScore > bestScore) {
                bestScore = fullScore
                best = c
            }

            // 2. Comparar frase contra partes del nombre
            for (part in cleanName.split(" ")) {
                if (part.length < 3) continue
                val partScore = similarity(cleanPhrase, part)
                if (partScore > bestScore) {
                    bestScore = partScore
                    best = c
                }
            }
        }
        return if (bestScore >= THRESHOLD) best else null
    }

    /** Busca a qué cliente se refiere una palabra individual. */
    fun matchToken(token: String, clients: List<ClientRef>): ClientRef? = matchPhrase(token, clients)

    /**
     * Busca la mejor coincidencia probando primero ventanas de 3 palabras, 2 palabras y 1 palabra.
     * Devuelve Pair(ClienteCoincidente, CantidadDePalabrasConsumidas).
     */
    fun matchWindow(
        tokens: List<String>,
        startIndex: Int,
        clients: List<ClientRef>,
        excludeIndices: Set<Int>,
        delimiters: Set<String>,
    ): Pair<ClientRef, List<Int>>? {
        if (startIndex !in tokens.indices) return null

        val windowIndices = ArrayList<Int>()
        var k = startIndex
        while (k < tokens.size && windowIndices.size < 3 && k !in excludeIndices &&
            tokens[k] != "," && tokens[k] !in delimiters
        ) {
            windowIndices.add(k)
            k++
        }

        if (windowIndices.isEmpty()) return null

        // Probar primero la frase más larga (ej. 3 palabras, luego 2, luego 1)
        for (len in windowIndices.size downTo 1) {
            val subIndices = windowIndices.take(len)
            val phrase = subIndices.joinToString(" ") { tokens[it] }

            // 1. Priorizar coincidencia exacta o de alta similitud con el nombre completo
            for (c in clients) {
                val cleanName = Txt.stripAccents(c.name.lowercase().trim())
                val cleanPhrase = Txt.stripAccents(phrase.lowercase().trim())
                if (similarity(cleanPhrase, cleanName) >= THRESHOLD) {
                    return Pair(c, subIndices)
                }
            }
        }

        // 2. Si no hay coincidencia con el nombre completo, probar con matchPhrase
        for (len in windowIndices.size downTo 1) {
            val subIndices = windowIndices.take(len)
            val phrase = subIndices.joinToString(" ") { tokens[it] }
            val matched = matchPhrase(phrase, clients)
            if (matched != null) {
                return Pair(matched, subIndices)
            }
        }

        return null
    }
}
