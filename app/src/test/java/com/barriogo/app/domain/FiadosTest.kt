package com.barriogo.app.domain

import com.barriogo.app.data.fiados.Cliente
import com.barriogo.app.data.MovementEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** F1 · Fiados: saldo pendiente por cliente, búsqueda y textos. */
class FiadosTest {
    private val zona = ZoneId.of("America/Bogota")
    private val marta = Cliente(1, "c1", "Marta Ramírez", "300 123 4567")
    private val pedro = Cliente(2, "c2", "Pedro Gómez", "311 555 0000")
    private val rosa = Cliente(3, "c3", "Rosa Hernández")

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

    @Test fun estadoDePagoFifo() {
        // Igual que el backend: el abono salda primero el fiado más viejo; pagar no borra el fiado.
        val movs = listOf(
            mov(MovementType.FIADO, 1, 10_000, dia(3, 2)).copy(id = 1),
            mov(MovementType.FIADO, 1, 5_000, dia(3, 3)).copy(id = 2),
            mov(MovementType.FIADO, 1, 4_000, dia(3, 4)).copy(id = 3),
            mov(MovementType.ABONO, 1, 12_000, dia(3, 5)).copy(id = 4),
            mov(MovementType.FIADO, 1, 9_000, dia(3, 5), anulado = true).copy(id = 5),
        )
        val pagos = Fiados.pagosPorFiado(1, movs)
        assertEquals(PagoFiado(EstadoPagoFiado.PAGADO, 0), pagos[1])
        assertEquals(PagoFiado(EstadoPagoFiado.ABONADO, 3_000), pagos[2])
        assertEquals(PagoFiado(EstadoPagoFiado.PENDIENTE, 4_000), pagos[3])
        assertEquals(null, pagos[5])
        assertEquals(7_000L, Fiados.balance(1, movs))
    }

    @Test fun abonoAUnSoloFiado() {
        // Tres fiados; el abono dirigido salda el de 5.000 y deja los otros intactos. Igual que el backend.
        val movs = listOf(
            mov(MovementType.FIADO, 1, 20_000, dia(3, 2)).copy(id = 1, remoteId = "f1"),
            mov(MovementType.FIADO, 1, 5_000, dia(3, 3)).copy(id = 2, remoteId = "f2"),
            mov(MovementType.FIADO, 1, 25_000, dia(3, 4)).copy(id = 3, remoteId = "f3"),
            mov(MovementType.ABONO, 1, 5_000, dia(3, 5)).copy(id = 4, fiadoId = "f2"),
        )
        val pagos = Fiados.pagosPorFiado(1, movs)
        assertEquals(PagoFiado(EstadoPagoFiado.PENDIENTE, 20_000), pagos[1])
        assertEquals(PagoFiado(EstadoPagoFiado.PAGADO, 0), pagos[2])
        assertEquals(PagoFiado(EstadoPagoFiado.PENDIENTE, 25_000), pagos[3])
        // Lo que sobra de un abono dirigido baja la deuda del más viejo
        val sobra = Fiados.pagosPorFiado(1, movs + mov(MovementType.ABONO, 1, 3_000, dia(3, 6)).copy(id = 5, fiadoId = "f2"))
        assertEquals(PagoFiado(EstadoPagoFiado.ABONADO, 17_000), sobra[1])
        assertEquals("", Fiados.problemaAbonoAFiado(0, 5_000))
        assertEquals(null, Fiados.problemaAbonoAFiado(5_000, 5_000))
        assertEquals("Este fiado ya está pagado", Fiados.problemaAbonoAFiado(1_000, 0))
        assertTrue(Fiados.problemaAbonoAFiado(6_000, 5_000)!!.startsWith("De este fiado solo faltan $ 5.000"))
    }

    @Test fun cupoDeFiado() {
        assertEquals(50_000L, Fiados.CUPO_POR_DEFECTO)
        assertEquals(null, Fiados.problemaCupo("Marta", 30_000, 20_000, 50_000))
        assertEquals(
            "Marta debe $ 40.000 y su límite es $ 50.000: le puedes fiar hasta $ 10.000.",
            Fiados.problemaCupo("Marta", 40_000, 15_000, 50_000),
        )
        assertTrue(Fiados.problemaCupo("Marta", 50_000, 1, 50_000)!!.startsWith("Marta ya llegó a su límite"))
        val enElLimite = FiadoPendiente(marta, 50_000, dia(3, 2), 2)
        assertTrue(Fiados.enElLimite(enElLimite))
        assertEquals(false, Fiados.pasoElLimite(enElLimite))
        assertTrue(Fiados.pasoElLimite(enElLimite.copy(saldo = 51_000)))
        assertTrue(Fiados.pasoElLimite(enElLimite.copy(saldo = 10_000, cliente = marta.copy(overLimit = true))))
        assertEquals(0L, Fiados.disponible(enElLimite.copy(saldo = 60_000)))
    }

    @Test fun cambiarCupo() {
        assertEquals("El cupo no puede ser menor a $ 50.000", Fiados.problemaNuevoCupo(50_000, 40_000))
        assertEquals("", Fiados.problemaNuevoCupo(50_000, 50_000))
        assertEquals(null, Fiados.problemaNuevoCupo(50_000, 120_000))
    }

    @Test fun datosDelCliente() {
        assertTrue(Fiados.problemasCliente("Marta Ramírez", "3004567890", "").isEmpty())
        assertEquals(setOf("nombre", "celular"), Fiados.problemasCliente("M", "", "").keys)
        assertEquals("El celular debe tener entre 7 y 10 dígitos", Fiados.problemasCliente("Marta", "300", "")["celular"])
        assertEquals("La dirección puede tener hasta 200 letras", Fiados.problemasCliente("Marta", "3004567890", "x".repeat(201))["direccion"])
    }

    @Test fun editarYAnular() {
        // Fió 10.000 y 5.000 y abonó 12.000: el balance es 3.000.
        assertEquals(null, Fiados.problemaEditarFiado("Marta", 5_000, 8_000, 3_000, 50_000))
        assertEquals(
            "Marta ya abonó más que eso: este fiado no puede quedar en menos de $ 2.000",
            Fiados.problemaEditarFiado("Marta", 5_000, 1_000, 3_000, 50_000),
        )
        assertTrue(Fiados.problemaEditarFiado("Marta", 5_000, 60_000, 3_000, 50_000)!!.contains("límite"))
        assertEquals(null, Fiados.problemaEditarAbono(12_000, 15_000, 3_000))
        assertEquals("No puede abonar más de lo que debe ($ 15.000)", Fiados.problemaEditarAbono(12_000, 16_000, 3_000))
        assertEquals(null, Fiados.problemaAnularFiado(3_000, 3_000))
        assertTrue(Fiados.problemaAnularFiado(5_000, 3_000) != null)
    }
}
