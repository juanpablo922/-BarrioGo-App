package com.barriogo.app.ai

import com.barriogo.app.data.MovementEntity
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import com.barriogo.app.domain.titleWithClient
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

    @Test fun fiado_cliente_con_nombre_y_apellido_que_no_existe() {
        val d = ok("Fiado para Mateo Ramírez, arroz y leche, 24 mil", MovementType.FIADO)
        assertNull(d.clientId)
        assertEquals("Mateo Ramirez", d.heardClient)
        assertTrue(DraftIssue.CLIENT_UNKNOWN in d.issues)
    }

    @Test fun fiado_cliente_con_nombre_y_apellido_existente() {
        val d = ok("Le fío a Marta Ramírez 12 mil en gaseosas", MovementType.FIADO)
        assertEquals(1L, d.clientId)
        assertEquals(12000L, d.amount)
        assertEquals(listOf("Gaseosas"), d.items)
    }

    @Test fun fiado_directo_sin_preposicion_y_monto_pegado() {
        val d = ok("Laura Estefanía 14mil de pollo", MovementType.FIADO)
        assertNull(d.clientId)
        assertEquals("Laura Estefania", d.heardClient)
        assertEquals(14000L, d.amount)
        assertEquals(listOf("Pollo"), d.items)
    }

    @Test fun fiado_con_verbo_se_llevo() {
        val d = ok("Laura Estefanía se llevó 14 mil de pollo", MovementType.FIADO)
        assertNull(d.clientId)
        assertEquals("Laura Estefania", d.heardClient)
        assertEquals(14000L, d.amount)
        assertEquals(listOf("Pollo"), d.items)
    }

    // ---------- Varios valores en una frase ----------
    private val conLuisa = clients + ClientRef(6, "Luisa Castro")

    private fun okCon(text: String, mode: MovementType = MovementType.VENTA, lista: List<ClientRef> = conLuisa): MovementDraft =
        (MovementAgent.parse(text, mode, lista) as AgentResult.Ok).draft

    @Test fun venta_con_dos_valores_y_cliente_existente() {
        val d = okCon("Venta de 15mil de leche y 25mil de agua a Luisa Castro")
        assertEquals(MovementType.VENTA, d.type)
        assertEquals(40000L, d.amount)
        assertEquals(listOf("Leche", "Agua"), d.items)
        assertEquals("Luisa Castro", d.clientName)
        assertEquals(listOf<Pair<String?, Long>>("Leche" to 15000L, "Agua" to 25000L), d.desglose)
        assertTrue(d.issues.isEmpty())
    }

    @Test fun venta_con_cliente_que_no_existe_no_tiene_problemas() {
        val d = okCon("Venta de 15mil de leche y 25mil de agua a Luisa Castro", lista = clients)
        assertEquals(40000L, d.amount)
        assertEquals(listOf("Leche", "Agua"), d.items)
        assertNull(d.clientId)
        assertEquals("Luisa Castro", d.clientName)
        assertTrue(d.issues.isEmpty())
    }

    @Test fun venta_varias_formas_de_decir_los_valores() {
        assertEquals(40000L, okCon("Vendí leche por 15 mil y agua por 25 mil").amount)
        assertEquals(40000L, okCon("Vendí leche 15 mil agua 25 mil").amount)
        assertEquals(40000L, okCon("venta de leche y agua, 15 mil y 25 mil").amount)
        assertEquals(40000L, okCon("Venta de 15 mil 25 mil").amount)
        assertEquals(40000L, okCon("Venta de quince mil de leche y veinticinco mil de agua").amount)
        assertEquals(45000L, okCon("Venta de 15.000 de leche, 25.000 de agua y 5.000 de pan").amount)
    }

    @Test fun un_total_dicho_no_se_suma_otra_vez() {
        assertEquals(40000L, okCon("Vendí 15 mil de leche y 25 mil de agua, son 40 mil").amount)
        assertEquals(40000L, okCon("Vendí 15 mil de leche y 25 mil de agua, en total 40 mil").amount)
        assertEquals(40000L, okCon("Vendí 15 mil de leche y 25 mil de agua, 40 mil en total").amount)
    }

    @Test fun precio_unitario_y_billete_no_se_suman() {
        assertEquals(10000L, okCon("Vendí 2 arroz de 5 mil por 10 mil").amount)
        assertEquals(10000L, okCon("Vendí 2 arroz a 5 mil cada uno por 10 mil").amount)
        assertEquals(5000L, okCon("Vendí pan por 5 mil, me pagó con 10 mil").amount)
        assertEquals(8500L, okCon("Vendí arroz por 8 mil 500").amount)
    }

    @Test fun cantidades_no_son_valores() {
        val d = okCon("Vendí 2 gaseosas por 8 mil a Luisa Castro")
        assertEquals(2, d.quantity)
        assertEquals(8000L, d.amount)
        assertEquals(listOf("Gaseosa"), d.items)
        assertEquals("Luisa Castro", d.clientName)
    }

    @Test fun cliente_en_venta_otras_formas() {
        assertEquals("Luisa Castro", okCon("Vendí leche por 15 mil a Luisa Castro").clientName)
        assertEquals("Luisa Castro", okCon("Luisa Castro compró leche por 15 mil").clientName)
        assertEquals(listOf("Leche"), okCon("Luisa Castro compró leche por 15 mil").items)
        assertEquals("Juan", okCon("Venta de 10 mil de pan a don Juan", lista = clients).clientName)
        val soloCliente = okCon("Venta de 40 mil a Luisa Castro")
        assertEquals("Luisa Castro", soloCliente.clientName)
        assertEquals(listOf("Venta"), soloCliente.items)
    }

    @Test fun venta_sin_cliente_no_inventa_uno() {
        assertNull(okCon("Vendí 2 gaseosas por 8 mil").clientName)
        assertNull(okCon("Vendí 2 arroz a 5 mil").clientName)
        assertNull(okCon("Vendí leche a domicilio por 5 mil").clientName)
        assertNull(okCon("Venta de arroz por 5 mil para llevar").clientName)
    }

    @Test fun lo_que_se_dice_manda_sobre_la_pestana() {
        val frase = "Venta de 15 mil de leche y 25 mil de agua a Luisa Castro"
        assertEquals(MovementType.VENTA, okCon(frase, MovementType.FIADO).type)
        assertEquals(MovementType.VENTA, okCon(frase, MovementType.GASTO).type)
        assertEquals(MovementType.GASTO, okCon("Gasto de 8 mil en bolsas", MovementType.FIADO).type)
        assertEquals(MovementType.FIADO, okCon("Fiado de 8 mil de pan a Luisa", MovementType.GASTO).type)
    }

    @Test fun la_pestana_manda_si_la_frase_no_lo_dice() {
        assertEquals(MovementType.FIADO, okCon("Luisa Castro compró leche por 15 mil", MovementType.FIADO).type)
        assertEquals(MovementType.FIADO, okCon("Marta pago 5 mil de pan", MovementType.FIADO).type)
        assertEquals(MovementType.GASTO, okCon("Luz 50 mil", MovementType.GASTO).type)
    }

    @Test fun venta_con_tarjeta_de_credito_no_es_fiado() {
        val d = okCon("Venta de 10 mil de pan con tarjeta de crédito")
        assertEquals(MovementType.VENTA, d.type)
        assertEquals(PayMethod.TRANSFERENCIA, d.payMethod)
    }

    @Test fun fiado_con_dos_valores() {
        val d = okCon("Fiado de 15 mil de leche y 25 mil de agua a Luisa Castro", MovementType.FIADO)
        assertEquals(40000L, d.amount)
        assertEquals(6L, d.clientId)
        assertEquals(listOf("Leche", "Agua"), d.items)
    }

    @Test fun gasto_con_dos_valores_sin_la_palabra_gasto_en_el_concepto() {
        val d = okCon("Gasto de 15 mil de leche y 25 mil de agua")
        assertEquals(MovementType.GASTO, d.type)
        assertEquals(40000L, d.amount)
        assertEquals(listOf("Leche", "Agua"), d.items)
        assertEquals(listOf("Bolsas"), okCon("Gasto de 8 mil en bolsas").items)
    }

    @Test fun titulo_en_movimientos_con_cliente() {
        fun m(type: MovementType, title: String, detail: String = "", cliente: String? = null, qty: Int = 1) =
            MovementEntity(type = type.name, amount = 1, title = title, detail = detail, quantity = qty, clientName = cliente, profileId = 0, profileName = "", createdAt = 0)
        assertEquals("Luisa Castro · Leche y Agua", m(MovementType.VENTA, "Leche y Agua", cliente = "Luisa Castro").titleWithClient())
        assertEquals("Luisa Castro · 2 × Gaseosa", m(MovementType.VENTA, "Gaseosa", cliente = "Luisa Castro", qty = 2).titleWithClient())
        assertEquals("Luisa Castro · Leche", m(MovementType.FIADO, "Fiado", detail = "Leche", cliente = "Luisa Castro").titleWithClient())
        assertEquals("Luisa Castro · Abono", m(MovementType.ABONO, "Abono", cliente = "Luisa Castro").titleWithClient())
        assertEquals("Leche", m(MovementType.VENTA, "Leche").titleWithClient())
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
