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
        val x = Txt.stripAccents(a.lowercase())
        val y = Txt.stripAccents(b.lowercase())
        val m = maxOf(x.length, y.length)
        return if (m == 0) 1.0 else 1.0 - levenshtein(x, y).toDouble() / m
    }

    /** Busca a qué cliente se refiere una palabra (compara con cada parte del nombre: "Marta" ~ "Marta Ramírez"). */
    fun matchToken(token: String, clients: List<ClientRef>): ClientRef? {
        if (token.length < 3) return null
        var best: ClientRef? = null
        var bestScore = 0.0
        for (c in clients) {
            for (part in Txt.stripAccents(c.name.lowercase()).split(" ")) {
                if (part.isEmpty()) continue
                val s = similarity(token, part)
                if (s > bestScore) { bestScore = s; best = c }
            }
        }
        return if (bestScore >= THRESHOLD) best else null
    }
}
