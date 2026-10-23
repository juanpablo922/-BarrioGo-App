package com.barriogo.app.data.caja

import com.barriogo.app.domain.TotalesCaja

/**
 * Caja diaria tal como está en Firestore (tiendas/{t}/cajas/{id}). Es lo que leen las pantallas:
 * ya no hay copia de la caja en Room. Los nombres de los campos siguen los de la pantalla
 * (apertura, cierre…); entre paréntesis, el campo de Firestore.
 */
data class Caja(
    /** Id del documento: "{AAAA-MM-DD}-1", uno por día. */
    val id: String,
    /** Día de la caja (fechaOperacion). */
    val operationDate: String,
    /** Dinero con el que se abrió (montoApertura). */
    val openingAmount: Long,
    /** Cambios de la apertura ya usados, máximo 3 (cambiosApertura). */
    val editsUsed: Int,
    /** Quién la abrió (abiertaPorNombre). */
    val openedBy: String,
    /** Cuándo se abrió (abiertaEn), en milisegundos. */
    val openedAt: Long,
    /** estado == "abierta". */
    val isOpen: Boolean,
    /** Cuándo se cerró (cerradaEn). */
    val closedAt: Long? = null,
    /** Quién la cerró (cerradaPorNombre). */
    val closedBy: String? = null,
    /** Efectivo contado al cerrar (cierre.efectivoContado). */
    val countedAmount: Long? = null,
    /** Saldo esperado que calculó el servidor al cerrar (cierre.totales.saldoEsperado). */
    val expectedAtClose: Long? = null,
    /** Observaciones del cierre (cierre.observaciones). */
    val closeNote: String = "",
    /** Correcciones del cierre ya usadas, máximo 2 (cambiosCierre). */
    val closeEditsUsed: Int = 0,
    /** Totales del comprobante que calculó el servidor (cierre.totales). */
    val totalesCierre: TotalesCaja? = null,
    /** true mientras la apertura o un cambio todavía no sube a Firestore (sin internet). */
    val sinSincronizar: Boolean = false,
)
