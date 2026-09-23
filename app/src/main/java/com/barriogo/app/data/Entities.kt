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
    val biometric: Boolean = false,
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
)

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
