package com.barriogo.app.data

import com.barriogo.app.domain.MAX_OPENING_CHANGES
import kotlinx.coroutines.flow.Flow

class BarrioRepository(private val db: AppDatabase) {
    val profiles: Flow<List<ProfileEntity>> = db.profileDao().observeAll()
    val clients: Flow<List<ClientEntity>> = db.clientDao().observeAll()
    val movements: Flow<List<MovementEntity>> = db.movementDao().observeAll()
    val activeSession: Flow<CashSessionEntity?> = db.cashDao().observeActive()
    val lastClosed: Flow<CashSessionEntity?> = db.cashDao().observeLastClosed()

    suspend fun profile(id: Long): ProfileEntity? = db.profileDao().byId(id)
    suspend fun addProfile(p: ProfileEntity): Long = db.profileDao().insert(p)
    suspend fun updateProfile(p: ProfileEntity) = db.profileDao().update(p)
    suspend fun deleteProfile(id: Long) = db.profileDao().delete(id)

    suspend fun addClient(name: String, phone: String): Long =
        db.clientDao().insert(ClientEntity(name = name.trim(), phone = phone.trim()))

    suspend fun movement(id: Long): MovementEntity? = db.movementDao().byId(id)
    suspend fun insertMovement(m: MovementEntity): Long = db.movementDao().insert(m)
    suspend fun updateMovement(m: MovementEntity) = db.movementDao().update(m)
    suspend fun deleteMovement(id: Long) = db.movementDao().delete(id)
    suspend fun voidMovement(id: Long) {
        val m = db.movementDao().byId(id) ?: return
        db.movementDao().update(m.copy(voided = true, voidedAt = System.currentTimeMillis()))
    }

    /** Corrige valor y concepto. Guarda el valor original la primera vez para no perder el rastro. */
    suspend fun editMovement(id: Long, amount: Long, title: String) {
        val m = db.movementDao().byId(id) ?: return
        if (m.voided) return
        db.movementDao().update(
            m.copy(
                amount = amount,
                title = title,
                editedAt = System.currentTimeMillis(),
                originalAmount = m.originalAmount ?: m.amount.takeIf { it != amount },
            )
        )
    }

    suspend fun openCash(opening: Long, by: String): CashSessionEntity {
        val s = CashSessionEntity(openedAt = System.currentTimeMillis(), openingAmount = opening, openedBy = by)
        val id = db.cashDao().insert(s)
        return s.copy(id = id)
    }

    /** Cambia la apertura del día. Devuelve null si ya se usaron los 3 cambios permitidos. */
    suspend fun changeOpening(s: CashSessionEntity, newAmount: Long): CashSessionEntity? {
        if (s.editsUsed >= MAX_OPENING_CHANGES) return null
        val updated = s.copy(openingAmount = newAmount, editsUsed = s.editsUsed + 1)
        db.cashDao().update(updated)
        return updated
    }

    suspend fun saveArqueo(a: ArqueoEntity) { db.cashDao().insertArqueo(a) }

    suspend fun closeCash(s: CashSessionEntity, counted: Long, expected: Long, by: String, note: String) {
        db.cashDao().update(
            s.copy(closedAt = System.currentTimeMillis(), countedAmount = counted, expectedAtClose = expected, closedBy = by, closeNote = note)
        )
    }
}
