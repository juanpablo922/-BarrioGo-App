package com.barriogo.app.domain

import com.barriogo.app.data.ClientEntity
import com.barriogo.app.data.MovementEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** F1 · Fiados: saldo pendiente por cliente, búsqueda y textos. */
class FiadosTest {
    private val zona = ZoneId.of("America/Bogota")
    private val marta = ClientEntity(1, "Marta Ramírez", "300 123 4567")
    private val pedro = ClientEntity(2, "Pedro Gómez", "311 555 0000")
    private val rosa = ClientEntity(3, "Rosa Hernández")

    private fun dia(mes: Int, d: Int) = LocalDate.of(2026, mes, d).atStartOfDay(zona).toInstant().toEpochMilli()

    private fun mov(tipo: MovementType, cliente: Long, monto: Long, fecha: Long, anulado: Boolean = false) = MovementEntity(
        type = tipo.name, amount = monto, title = "", detail = "", clientId = cliente,
        profileId = 1, profileName = "Admin", createdAt = fecha, voided = anulado,
    )

    @Test fun saldoYOrden() {
        val movs = listOf(
            mov(MovementType.FIADO, 1, 5_000, dia(3, 2)),
            mov(MovementType.FIADO, 1, 7_000, dia(3, 4)),
            mov(MovementType.FIADO, 2, 8_000, dia(3, 5)),
            mov(MovementType.FIADO, 3, 4_000, dia(3, 6)),
            mov(MovementType.ABONO, 3, 4_000, dia(3, 7)),
            mov(MovementType.VENTA, 3, 9_000, dia(3, 7)),
        )
        val p = Fiados.pendientes(listOf(marta, pedro, rosa), movs)
        assertEquals(listOf(1L, 2L), p.map { it.cliente.id })
        assertEquals(12_000L, p[0].saldo)
        assertEquals(2, p[0].fiados)
    }

    @Test fun anuladosNoCuentan() {
        val movs = listOf(mov(MovementType.FIADO, 2, 8_000, dia(3, 5), anulado = true))
        assertTrue(Fiados.pendientes(listOf(pedro), movs).isEmpty())
    }

    @Test fun deudaNuevaDespuesDePagarTodo() {
        val movs = listOf(
            mov(MovementType.FIADO, 1, 5_000, dia(2, 1)),
            mov(MovementType.ABONO, 1, 5_000, dia(2, 10)),
            mov(MovementType.FIADO, 1, 3_000, dia(3, 2)),
            mov(MovementType.FIADO, 1, 2_000, dia(3, 3)),
            mov(MovementType.ABONO, 1, 1_000, dia(3, 4)),
        )
        val f = Fiados.pendientes(listOf(marta), movs).single()
        assertEquals(4_000L, f.saldo)
        assertEquals(dia(3, 2), f.desde)
        assertEquals("Desde el 2 de marzo · 2 fiados", Fiados.subtitulo(f, zona))
    }

    @Test fun buscarPorNombreOTelefono() {
        val movs = listOf(mov(MovementType.FIADO, 1, 1_000, dia(3, 2)), mov(MovementType.FIADO, 2, 1_000, dia(3, 2)))
        val p = Fiados.pendientes(listOf(marta, pedro), movs)
        assertEquals(listOf(1L), Fiados.filtrar(p, "ramirez").map { it.cliente.id })
        assertEquals(listOf(2L), Fiados.filtrar(p, "3115550").map { it.cliente.id })
        assertEquals(2, Fiados.filtrar(p, "  ").size)
    }

    @Test fun recientesIncluyeClientesSinDeuda() {
        val movs = listOf(
            mov(MovementType.FIADO, 1, 5_000, dia(3, 2)),
            mov(MovementType.FIADO, 3, 4_000, dia(3, 6)),
            mov(MovementType.ABONO, 3, 4_000, dia(3, 7)),
        )
        val r = Fiados.recientes(Fiados.cuentas(listOf(marta, pedro, rosa), movs))
        assertEquals(listOf(3L, 1L, 2L), r.map { it.cliente.id })
        assertEquals(0L, r[0].saldo)
    }

    @Test fun movimientosDelCliente() {
        val movs = listOf(
            mov(MovementType.FIADO, 1, 5_000, dia(3, 2)),
            mov(MovementType.VENTA, 1, 9_000, dia(3, 3)),
            mov(MovementType.ABONO, 1, 1_000, dia(3, 4)),
            mov(MovementType.FIADO, 2, 1_000, dia(3, 5)),
        )
        assertEquals(listOf(dia(3, 4), dia(3, 2)), Fiados.movimientosDe(1, movs).map { it.createdAt })
    }

    @Test fun anuladosEnElHistorial() {
        val movs = listOf(
            mov(MovementType.FIADO, 1, 5_000, dia(3, 2), anulado = true),
            mov(MovementType.FIADO, 1, 4_000, dia(3, 4)),
        )
        assertEquals(1, Fiados.movimientosDe(1, movs).size)
        assertEquals(2, Fiados.movimientosDe(1, movs, conAnulados = true).size)
    }

    @Test fun deudaActualEmpiezaDespuesDeQuedarAPazYSalvo() {
        val viejo = mov(MovementType.FIADO, 1, 5_000, dia(2, 1))
        val pago = mov(MovementType.ABONO, 1, 5_000, dia(2, 3))
        val f1 = mov(MovementType.FIADO, 1, 5_000, dia(3, 2))
        val f2 = mov(MovementType.FIADO, 1, 4_000, dia(3, 4))
        val abono = mov(MovementType.ABONO, 1, 2_000, dia(3, 5))
        val otro = mov(MovementType.FIADO, 2, 8_000, dia(3, 5))
        val anulado = mov(MovementType.FIADO, 1, 1_000, dia(3, 6), anulado = true)
        assertEquals(listOf(f1, f2, abono), Fiados.deudaActual(1, listOf(f2, viejo, abono, otro, pago, f1, anulado)))
        // A paz y salvo: no hay deuda que mostrar
        assertEquals(emptyList<MovementEntity>(), Fiados.deudaActual(1, listOf(viejo, pago)))
    }

    @Test fun textosDeLaHoja() {
        assertEquals("2 mar", Fiados.fechaCorta(dia(3, 2), zona))
        assertEquals("Arroz y panela", Fiados.conceptoCorto(mov(MovementType.FIADO, 1, 5_000, dia(3, 2)).copy(detail = "arroz y panela")))
        assertEquals("Fiado", Fiados.conceptoCorto(mov(MovementType.FIADO, 1, 5_000, dia(3, 2))))
        assertEquals("Abono", Fiados.conceptoCorto(mov(MovementType.ABONO, 1, 5_000, dia(3, 2))))
        assertEquals("Marta Ramírez pagó. Entraron $ 12.000 a tu caja.", Fiados.mensajePagado("Marta Ramírez", 12_000))
    }

    @Test fun fechasYMensajes() {
        val ahora = dia(9, 5) + 15 * 3_600_000L
        assertEquals("Hoy · 9:02", Fiados.cuando(dia(9, 5) + 9 * 3_600_000L + 2 * 60_000L, ahora, zona))
        assertEquals("Vie. 4 sep · 11:00", Fiados.cuando(dia(9, 4) + 11 * 3_600_000L, ahora, zona))
        assertEquals("DM", Fiados.iniciales("Doña Marta"))
        assertEquals("Marta Ramírez se llevó leche, pan por $ 15.000.", Fiados.mensajeGuardado("Marta Ramírez", "Leche, pan", 15_000))
        assertEquals("Abono en efectivo", Fiados.titulo(mov(MovementType.ABONO, 1, 1_000, dia(3, 4))))
    }

    @Test fun validarAbono() {
        assertEquals("", Fiados.problemaAbono(0, 50_000))
        assertEquals(null, Fiados.problemaAbono(20_000, 50_000))
        assertEquals(null, Fiados.problemaAbono(50_000, 50_000))
        assertEquals("No puede abonar más de lo que debe ($ 50.000)", Fiados.problemaAbono(60_000, 50_000))
        assertEquals("Este cliente no tiene saldo pendiente", Fiados.problemaAbono(1_000, 0))
        assertEquals("Abono de $ 20.000 recibido de Marta Ramírez.", Fiados.mensajePago("Marta Ramírez", 20_000))
    }

    @Test fun textos() {
        assertEquals("1 cliente con fiado pendiente", Fiados.resumenClientes(1))
        assertEquals("3 clientes con fiado pendiente", Fiados.resumenClientes(3))
    }
}
