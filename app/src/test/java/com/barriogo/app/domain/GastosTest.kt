package com.barriogo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** G2 · Validaciones del gasto (mismas de firestore.rules) y G1 · filtros. */
class GastosTest {
    @Test fun camposObligatorios() {
        val p = ReglasGasto.problemas(null, 0, "  ")
        assertEquals(ReglasGasto.FALTA_TIPO, p[CampoGasto.TIPO])
        assertEquals(ReglasGasto.FALTA_MONTO, p[CampoGasto.MONTO])
        assertEquals(ReglasGasto.FALTA_DETALLE, p[CampoGasto.DETALLE])
        assertTrue(ReglasGasto.problemas(ExpenseKind.PROVEEDOR, 85_000, "Postobón").isEmpty())
    }

    @Test fun limites() {
        assertTrue(CampoGasto.MONTO in ReglasGasto.problemas(ExpenseKind.OTROS, 1_000_000_001, "x"))
        assertTrue(ReglasGasto.problemas(ExpenseKind.OTROS, 1_000_000_000, "x").isEmpty())
        assertTrue(CampoGasto.DETALLE in ReglasGasto.problemas(ExpenseKind.OTROS, 1, "a".repeat(201)))
        assertTrue(ReglasGasto.problemas(ExpenseKind.OTROS, 1, "a".repeat(200)).isEmpty())
    }

    @Test fun mensajeBajoElBotonEnOrden() {
        assertEquals("Elige el tipo de gasto", ReglasGasto.estadoBoton(null, 0, ""))
        assertEquals("Ahora escribe cuánto pagaste", ReglasGasto.estadoBoton(ExpenseKind.SERVICIOS, 0, ""))
        assertEquals("Falta el monto", ReglasGasto.estadoBoton(ExpenseKind.SERVICIOS, 0, "", intento = true))
        assertEquals("Falta en qué fue el gasto", ReglasGasto.estadoBoton(ExpenseKind.SERVICIOS, 38_000, ""))
        assertEquals("Todo listo, ya puedes guardar", ReglasGasto.estadoBoton(ExpenseKind.SERVICIOS, 38_000, "Recibo del gas"))
    }

    @Test fun editarExigeAlgunCambio() {
        val original = GastoEditable(ExpenseKind.PROVEEDOR, 85_000, "Postobón")
        assertEquals("Todavía no has cambiado nada", ReglasGasto.estadoBoton(ExpenseKind.PROVEEDOR, 85_000, "Postobón ", original))
        assertFalse(ReglasGasto.puedeGuardar(ExpenseKind.PROVEEDOR, 85_000, "Postobón", original))
        assertTrue(ReglasGasto.puedeGuardar(ExpenseKind.PROVEEDOR, 90_000, "Postobón", original))
        assertTrue(ReglasGasto.puedeGuardar(ExpenseKind.OTROS, 85_000, "Postobón", original))
    }

    @Test fun categoriaDelCatalogoDeFirestore() {
        assertEquals("proveedor", ReglasGasto.categoriaId(ExpenseKind.PROVEEDOR))
        assertEquals("servicios", ReglasGasto.categoriaId(ExpenseKind.SERVICIOS))
        assertEquals("otros", ReglasGasto.categoriaId(ExpenseKind.OTROS))
    }

    @Test fun tipoDeGastosViejos() {
        assertEquals(ExpenseKind.SERVICIOS, ReglasGasto.tipoDe("SERVICIOS", ""))
        assertEquals(ExpenseKind.PROVEEDOR, ReglasGasto.tipoDe(null, "Gasto en Proveedor"))
        assertEquals(ExpenseKind.OTROS, ReglasGasto.tipoDe(null, "Factura escaneada"))
    }

    @Test fun filtrosHoySemanaMes() {
        val jueves = LocalDate.of(2026, 10, 22)
        assertEquals(jueves, ReglasGasto.desde(PeriodoGastos.HOY, jueves))
        assertEquals(LocalDate.of(2026, 10, 19), ReglasGasto.desde(PeriodoGastos.SEMANA, jueves))
        assertEquals(LocalDate.of(2026, 10, 1), ReglasGasto.desde(PeriodoGastos.MES, jueves))
    }
}
