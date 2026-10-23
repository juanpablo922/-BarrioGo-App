package com.barriogo.app.domain

import com.barriogo.app.data.MovementEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Mismos casos que "caja" en functions/test/dominio.test.ts del backend, más la caja diaria y los avisos. */
class CajaTest {
    private val hoy = LocalDate.of(2026, 10, 23)
    private val ayer = hoy.minusDays(1)

    private fun mov(tipo: MovementType, monto: Long, pago: PayMethod = PayMethod.EFECTIVO, anulado: Boolean = false) = MovementEntity(
        type = tipo.name, amount = monto, title = "x", detail = "", payMethod = pago.name,
        profileId = 1, profileName = "Don Juan", sessionId = 1, createdAt = 0, voided = anulado,
    )

    @Test fun estadoDelDia() {
        assertEquals(EstadoCaja.SIN_ABRIR, ReglasCaja.estado(hoy, null, null))
        assertEquals(EstadoCaja.SIN_ABRIR, ReglasCaja.estado(hoy, null, ayer))
        assertEquals(EstadoCaja.ABIERTA, ReglasCaja.estado(hoy, hoy, ayer))
        assertEquals(EstadoCaja.ABIERTA_DIA_ANTERIOR, ReglasCaja.estado(hoy, ayer, null))
        assertEquals(EstadoCaja.CERRADA_HOY, ReglasCaja.estado(hoy, null, hoy))
    }

    @Test fun cadaCuentaTieneSuPropiaCaja() {
        // Dos cuentas en el mismo celular no comparten datos: la clave es la tienda de Firestore.
        assertEquals("tienda-A", CuentaLocal.de("tienda-A"))
        assertEquals("tienda-B", CuentaLocal.de("tienda-B"))
        assertEquals(CuentaLocal.NINGUNA, CuentaLocal.de(null))
        assertEquals(CuentaLocal.NINGUNA, CuentaLocal.de(" "))
    }

    @Test fun soloSeRegistraConLaCajaDeHoyAbierta() {
        assertTrue(ReglasCaja.permiteRegistrar(EstadoCaja.ABIERTA))
        EstadoCaja.entries.filter { it != EstadoCaja.ABIERTA }.forEach { assertFalse(ReglasCaja.permiteRegistrar(it)) }
    }

    @Test fun idDeCajaUnoPorDia() {
        assertEquals("2026-10-23-1", ReglasCaja.idCaja(hoy))
    }

    @Test fun aperturaMayorACeroYDistintaAlCorregir() {
        assertEquals("La apertura no puede quedar en $ 0", ReglasCaja.problemaApertura(0))
        assertNotNull(ReglasCaja.problemaApertura(1_000_000_001))
        assertNull(ReglasCaja.problemaApertura(200_000))
        assertEquals("Escribe un valor distinto al actual", ReglasCaja.problemaApertura(200_000, actual = 200_000))
        assertNull(ReglasCaja.problemaApertura(250_000, actual = 200_000))
    }

    @Test fun cambiosPermitidos() {
        assertEquals(3, ReglasCaja.cambiosAperturaRestantes(0))
        assertEquals(0, ReglasCaja.cambiosAperturaRestantes(3))
        assertEquals(0, ReglasCaja.cambiosAperturaRestantes(5))
        assertEquals(2, ReglasCaja.cambiosCierreRestantes(0))
        assertEquals(1, ReglasCaja.cambiosCierreRestantes(1))
        assertEquals(0, ReglasCaja.cambiosCierreRestantes(2))
    }

    @Test fun conteoDelCierre() {
        assertEquals("Escribe cuánto efectivo contaste", ReglasCaja.problemaConteo(null, ""))
        assertNull(ReglasCaja.problemaConteo(0, ""))       // la caja puede quedar vacía
        assertNull(ReglasCaja.problemaConteo(540_000, "Billete de $ 5.000 no registrado"))
        assertNotNull(ReglasCaja.problemaConteo(1, "x".repeat(501)))
        assertTrue(ReglasCaja.hayCambiosEnCierre(540_000, "", 548_000, ""))
        assertTrue(ReglasCaja.hayCambiosEnCierre(540_000, "", 540_000, "Se contó mal"))
        assertFalse(ReglasCaja.hayCambiosEnCierre(540_000, "nota", 540_000, " nota "))
    }

    @Test fun arqueo() {
        assertEquals(Arqueo(-8_000, ResultadoArqueo.FALTANTE), ReglasCaja.arqueo(548_000, 540_000))
        assertEquals(ResultadoArqueo.CUADRADO, ReglasCaja.arqueo(548_000, 548_000).resultado)
        assertEquals(ResultadoArqueo.SOBRANTE, ReglasCaja.arqueo(548_000, 550_000).resultado)
    }

    @Test fun totalesComoElServidor() {
        // 200.000 + 423.000 − 75.000 + 30.000 = 578.000 (fiados y transferencias no mueven el efectivo)
        val t = ReglasCaja.totales(
            200_000,
            listOf(
                mov(MovementType.VENTA, 423_000),
                mov(MovementType.VENTA, 50_000, PayMethod.TRANSFERENCIA),
                mov(MovementType.GASTO, 75_000),
                mov(MovementType.ABONO, 30_000),
                mov(MovementType.FIADO, 15_000),
                mov(MovementType.GASTO, 999_999, anulado = true),
            ),
        )
        assertEquals(578_000, t.saldoEsperado)
        assertEquals(473_000, t.ventas)
        assertEquals(423_000, t.ventasEfectivo)
        assertEquals(50_000, t.ventasTransferencia)
        assertEquals(75_000, t.gastos)
        assertEquals(15_000, t.fiados)
        assertEquals(453_000, t.entradas)
        assertEquals(75_000, t.salidas)
    }

    @Test fun horaDeCierre() {
        assertTrue(ReglasCaja.horaValida("20:00"))
        assertTrue(ReglasCaja.horaValida("07:30"))
        assertFalse(ReglasCaja.horaValida("24:00"))
        assertFalse(ReglasCaja.horaValida("8:00"))
        assertEquals(LocalTime.of(20, 0), ReglasCaja.hora(null))
        assertEquals(LocalTime.of(20, 0), ReglasCaja.hora("mal"))
        assertEquals("21:05", ReglasCaja.horaTexto(LocalTime.of(21, 5)))
    }

    @Test fun avisosDeCierre() {
        val ocho = LocalTime.of(20, 0)
        assertEquals(AvisoCierre.Ninguno, ReglasCaja.aviso(hoy.atTime(19, 0), ocho, hoy))
        assertEquals(AvisoCierre.Pronto(30), ReglasCaja.aviso(hoy.atTime(19, 30), ocho, hoy))
        assertEquals(AvisoCierre.Vencido(45), ReglasCaja.aviso(hoy.atTime(20, 45), ocho, hoy))
        assertEquals(AvisoCierre.Ninguno, ReglasCaja.aviso(hoy.atTime(21, 0), ocho, null))     // sin caja abierta no se avisa
        // La caja de ayer sigue abierta: ya está vencida aunque hoy sea temprano.
        assertTrue(ReglasCaja.aviso(hoy.atTime(8, 0), ocho, ayer) is AvisoCierre.Vencido)
        assertNotNull(ReglasCaja.textoAviso(AvisoCierre.Vencido(90), ocho))
        assertNull(ReglasCaja.textoAviso(AvisoCierre.Ninguno, ocho))
    }

    @Test fun proximoAviso() {
        val ocho = LocalTime.of(20, 0)
        assertEquals(hoy.atTime(19, 30), ReglasCaja.proximoAviso(hoy.atTime(9, 0), ocho, hoy))
        assertEquals(hoy.atTime(20, 0), ReglasCaja.proximoAviso(hoy.atTime(19, 40), ocho, hoy))
        assertEquals(LocalDateTime.of(hoy, LocalTime.of(21, 10)), ReglasCaja.proximoAviso(hoy.atTime(20, 40), ocho, hoy))
        assertNull(ReglasCaja.proximoAviso(hoy.atTime(9, 0), ocho, null))
    }
}
