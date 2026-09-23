package com.barriogo.app.ai

import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentTest {
    private val clients = listOf(
        ClientRef(1, "Marta Ramírez"), ClientRef(2, "Jorge Martínez"), ClientRef(3, "Luis Rojas"),
        ClientRef(4, "Pedro Gómez"), ClientRef(5, "Rosa Hernández"),
    )

    private fun ok(text: String, mode: MovementType = MovementType.VENTA): MovementDraft {
        val r = MovementAgent.parse(text, mode, clients)
        assertTrue("Se esperaba propuesta para: $text", r is AgentResult.Ok)
        return (r as AgentResult.Ok).draft
    }

    // ---------- Números ----------
    private fun amountOf(text: String): Long? {
        val tokens = Txt.tokenize(text)
        return SpanishNumbers.pickAmount(SpanishNumbers.find(tokens), tokens)?.value
    }

    @Test fun numeros_en_palabras_y_digitos() {
        assertEquals(8000L, amountOf("por 8 mil"))
        assertEquals(8000L, amountOf("por ocho mil"))
        assertEquals(24000L, amountOf("veinticuatro mil"))
        assertEquals(1500L, amountOf("mil quinientos"))
        assertEquals(2500L, amountOf("dos mil quinientos"))
        assertEquals(12000L, amountOf("son 12.000"))
        assertEquals(3500L, amountOf("por 3500"))
        assertEquals(5000L, amountOf("cinco lucas"))
        assertEquals(8500L, amountOf("8 mil 500"))
        assertEquals(20000L, amountOf("veinte mil pesos"))
    }

    // ---------- Ventas (ejemplos del prototipo) ----------
    @Test fun venta_ejemplo_del_prototipo() {
        val d = ok("Vendí 2 gaseosas por 8 mil")
        assertEquals(MovementType.VENTA, d.type)
        assertEquals(2, d.quantity)
        assertEquals(listOf("Gaseosa"), d.items)
        assertEquals(8000L, d.amount)
        assertEquals(PayMethod.EFECTIVO, d.payMethod)
        assertTrue(d.issues.isEmpty())
    }

    @Test fun venta_con_numeros_en_palabras() {
        val d = ok("vendi dos gaseosas por ocho mil")
        assertEquals(2, d.quantity)
        assertEquals(8000L, d.amount)
    }

    @Test fun venta_sin_monto_pide_correccion() {
        val d = ok("Vendí 2 gaseosas")
        assertEquals(0L, d.amount)
        assertTrue(DraftIssue.AMOUNT_MISSING in d.issues)
    }

    @Test fun venta_plural_es() {
        assertEquals(listOf("Pan"), ok("Vendí 3 panes 6000").items)
        assertEquals(listOf("Tomate"), ok("vendí 3 tomates por 6 mil").items)
    }

    @Test fun venta_por_transferencia() {
        assertEquals(PayMethod.TRANSFERENCIA, ok("vendí 2 gaseosas por 8.000 por nequi").payMethod)
    }

    @Test fun ruido_no_se_entiende() {
        assertTrue(MovementAgent.parse("hmm", MovementType.VENTA, clients) is AgentResult.NotUnderstood)
        assertTrue(MovementAgent.parse("", MovementType.VENTA, clients) is AgentResult.NotUnderstood)
    }

    // ---------- Fiados ----------
    @Test fun fiado_ejemplo_del_prototipo() {
        val d = ok("Fiado para Marta, arroz y leche, 24 mil", MovementType.FIADO)
        assertEquals(MovementType.FIADO, d.type)
        assertEquals(1L, d.clientId)
        assertEquals(listOf("Arroz", "Leche"), d.items)
        assertEquals(24000L, d.amount)
        assertTrue(d.issues.isEmpty())
    }

    @Test fun fiado_le_fio_a_pedro() {
        val d = ok("Le fío a Pedro 12 mil en gaseosas", MovementType.FIADO)
        assertEquals(4L, d.clientId)
        assertEquals(12000L, d.amount)
        assertEquals(listOf("Gaseosas"), d.items)
    }

    @Test fun fiado_se_detecta_aunque_el_modo_sea_venta() {
        assertEquals(MovementType.FIADO, ok("fíale a Rosa 5 mil de pan").type)
    }

    @Test fun fiado_cliente_que_no_existe() {
        val d = ok("Fiado para Martina, arroz y leche, 24 mil", MovementType.FIADO)
        assertNull(d.clientId)
        assertEquals("Martina", d.heardClient)
        assertTrue(DraftIssue.CLIENT_UNKNOWN in d.issues)
    }

    // ---------- Edición por voz ----------
    @Test fun editar_venta_por_voz() {
        val base = MovementDraft(MovementType.VENTA, 2, listOf("Gaseosa"), 8000)
        val d = MovementAgent.applyEdit(base, "Eran 3 gaseosas, 12 mil")
        assertEquals(3, d.quantity)
        assertEquals(12000L, d.amount)
    }

    @Test fun editar_fiado_agregando_producto() {
        val base = MovementDraft(MovementType.FIADO, items = listOf("Arroz", "Leche"), amount = 24000, clientId = 1, clientName = "Marta Ramírez")
        val d = MovementAgent.applyEdit(base, "Agrégale una panela, son 31 mil")
        assertEquals(listOf("Arroz", "Leche", "Panela"), d.items)
        assertEquals(31000L, d.amount)
    }

    // ---------- Facturas ----------
    @Test fun factura_del_prototipo() {
        val txt = """
            TIENDA EL VECINO
            NIT 900.123.456-7
            Factura No. 00482
            Fecha: 15/09/2026 14:32
            Arroz x2   28.000
            Leche x4   16.000
            Gaseosas x6  24.000
            Otros 17.000
            TOTAL 85.000
        """.trimIndent()
        val d = InvoiceAgent.parse(txt)
        assertEquals("Tienda El Vecino", d.supplier)
        assertEquals("00482", d.number)
        assertEquals(85000L, d.total)
        assertEquals(4, d.items.size)
        assertEquals(85000L, d.itemsSum)
        assertTrue(d.issues.isEmpty())
    }

    @Test fun factura_cuya_suma_no_cuadra() {
        val txt = """
            TIENDA EL VECINO
            Arroz x2   28.000
            Leche x4   16.000
            Gaseosas x6  24.000
            TOTAL 85.000
        """.trimIndent()
        val d = InvoiceAgent.parse(txt)
        assertTrue(InvoiceIssue.SUM_MISMATCH in d.issues)
        assertEquals(17000L, d.missing)
    }

    @Test fun factura_con_cantidad_y_precio_unitario() {
        val txt = """
            Distribuidora La 14 S.A.S
            FACTURA DE VENTA FV-1234
            15 sep 2026
            2 Arroz Diana 14.000 28.000
            4 Leche entera 4.000 16.000
            Subtotal 44.000
            IVA 0
            TOTAL A PAGAR ${'$'} 44.000,00
        """.trimIndent()
        val d = InvoiceAgent.parse(txt)
        assertEquals("FV-1234", d.number)
        assertEquals(44000L, d.total)
        assertEquals(2, d.items.size)
        assertEquals(2, d.items[0].qty)
        assertTrue(InvoiceIssue.SUM_MISMATCH !in d.issues)
    }

    @Test fun factura_ilegible() {
        val d = InvoiceAgent.parse("borroso xx\nlal 1")
        assertTrue(InvoiceIssue.UNREADABLE in d.issues)
    }

    // ---------- Mini corpus de evaluación (base para las métricas del documento) ----------
    @Test fun corpus_exactitud_de_monto_y_tipo() {
        data class Case(val text: String, val mode: MovementType, val type: MovementType, val amount: Long)
        val corpus = listOf(
            Case("Vendí 2 gaseosas por 8 mil", MovementType.VENTA, MovementType.VENTA, 8000),
            Case("vendí un pollo por veinte mil pesos", MovementType.VENTA, MovementType.VENTA, 20000),
            Case("vendí pan y leche por 12 mil", MovementType.VENTA, MovementType.VENTA, 12000),
            Case("vendí cinco lucas de arroz", MovementType.VENTA, MovementType.VENTA, 5000),
            Case("Fiado para Marta, arroz y leche, 24 mil", MovementType.FIADO, MovementType.FIADO, 24000),
            Case("Le fío a Pedro 12 mil en gaseosas", MovementType.FIADO, MovementType.FIADO, 12000),
            Case("fíale a Rosa 5 mil de pan", MovementType.VENTA, MovementType.FIADO, 5000),
            Case("Marta se llevó arroz y leche por 24 mil", MovementType.FIADO, MovementType.FIADO, 24000),
        )
        var correct = 0
        for (c in corpus) {
            val r = MovementAgent.parse(c.text, c.mode, clients)
            if (r is AgentResult.Ok && r.draft.type == c.type && r.draft.amount == c.amount) correct++
        }
        println("Exactitud tipo+monto: $correct/${corpus.size}")
        assertEquals(corpus.size, correct)
    }
}
