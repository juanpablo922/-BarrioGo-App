package com.barriogo.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.barriogo.app.domain.PayMethod
import com.barriogo.app.domain.Role

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String,
    val pinHash: String,
    val pinSalt: String,
    /** Huella activada para este perfil EN ESTE CELULAR (la huella misma nunca sale del celular). */
    val biometric: Boolean = false,
    /**
     * Id del perfil en Firestore (tiendas/{t}/perfiles/{remoteId}). Los perfiles de una cuenta real
     * son una copia local de Firestore; null = perfil local de una versión anterior.
     */
    val remoteId: String? = null,
) {
    val isAdmin: Boolean get() = role == Role.ADMIN.name
    val initial: String get() = name.trim().firstOrNull()?.uppercase() ?: "?"
}

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
)

@Entity(tableName = "movements")
data class MovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    /** Siempre positivo, salvo AJUSTE (puede ser negativo). */
    val amount: Long,
    val title: String,
    val detail: String,
    val quantity: Int = 1,
    val payMethod: String = PayMethod.EFECTIVO.name,
    val clientId: Long? = null,
    val profileId: Long,
    val profileName: String,
    val sessionId: Long? = null,
    val createdAt: Long,
    val voided: Boolean = false,
    /** JSON con el detalle de una factura escaneada (proveedor, número, conceptos). */
    val extra: String = "",
    /** Cuándo se eliminó (anuló). Sigue en el historial, tachado, pero ya no cuenta en los totales. */
    val voidedAt: Long? = null,
    /** Última vez que se corrigió el valor o el concepto. */
    val editedAt: Long? = null,
    /** Valor con el que se registró la primera vez, si luego se corrigió. */
    val originalAmount: Long? = null,
    /** Id en Firestore (tiendas/{t}/movimientos/{remoteId}). null = solo en el celular (fiados y abonos). */
    val remoteId: String? = null,
    /** Tipo de gasto (ExpenseKind.name): PROVEEDOR, SERVICIOS u OTROS. Solo en gastos. */
    val category: String? = null,
    /** Tienda dueña del movimiento (tiendas/{cuenta}). Cada cuenta del celular ve solo lo suyo. */
    val cuenta: String = "",
    /** Caja de Firestore (tiendas/{t}/cajas/{cajaId}) en la que se registró. */
    val cajaId: String? = null,
)

/**
 * Cajas de versiones anteriores (v3–v4), cuando la caja vivía en el celular. Ya no se usan:
 * la caja se lee de Firestore (data/caja/Caja.kt). La tabla se conserva para no perder datos.
 */
@Entity(tableName = "cash_sessions")
data class CashSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val openedAt: Long,
    val openingAmount: Long,
    val editsUsed: Int = 0,
    val openedBy: String = "",
    val closedAt: Long? = null,
    val countedAmount: Long? = null,
    val expectedAtClose: Long? = null,
    val closedBy: String? = null,
    val closeNote: String = "",
    /** Día de la caja ("2026-10-23"). La caja es diaria: una por día. */
    val operationDate: String = "",
    /** Correcciones del cierre ya usadas (máximo 2). */
    val closeEditsUsed: Int = 0,
    /** Id en Firestore (tiendas/{t}/cajas/{remoteId}). */
    val remoteId: String? = null,
)

/**
 * Registro de cada cambio de la apertura (K1, máximo 3) o del cierre (c4, máximo 2).
 * Nada se sobrescribe sin dejar rastro: quién, cuándo, valor anterior y nuevo.
 */
@Entity(tableName = "cash_changes")
data class CashChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    /** "APERTURA" o "CIERRE". */
    val kind: String,
    val number: Int,
    val before: Long,
    val after: Long,
    val note: String = "",
    val by: String,
    val at: Long,
)

@Entity(tableName = "arqueos")
data class ArqueoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val at: Long,
    val expected: Long,
    val counted: Long,
    val note: String,
    val by: String,
)
