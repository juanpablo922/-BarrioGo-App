package com.barriogo.app.data

import com.barriogo.app.domain.MovementType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Perfiles y el libro de movimientos.
 * - [cuenta] = tienda activa: los movimientos de Room se leen y guardan solo para ella.
 * - [nube] = gastos, fiados y abonos de Firestore: el libro los junta con las ventas de Room.
 *   Los clientes viven en Firestore (FiadosRepository); la tabla clients de Room ya no se usa.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BarrioRepository(
    private val db: AppDatabase,
    private val cuenta: StateFlow<String>,
    nube: Flow<List<MovementEntity>>,
) {
    val profiles: Flow<List<ProfileEntity>> = db.profileDao().observeAll()

    /** Libro completo de la tienda activa, del más nuevo al más viejo. */
    val movements: Flow<List<MovementEntity>> = combine(cuenta.flatMapLatest { db.movementDao().observeAll(it) }, nube) { local, remotos ->
        (local.filter { it.type !in EN_FIRESTORE } + remotos).sortedWith(compareByDescending<MovementEntity> { it.createdAt }.thenByDescending { it.id })
    }

    suspend fun profile(id: Long): ProfileEntity? = db.profileDao().byId(id)
    suspend fun addProfile(p: ProfileEntity): Long = db.profileDao().insert(p)
    suspend fun updateProfile(p: ProfileEntity) = db.profileDao().update(p)
    suspend fun deleteProfile(id: Long) = db.profileDao().delete(id)

    /** Movimiento de Room (ventas). Gastos, fiados y abonos están en Firestore. */
    suspend fun movement(id: Long): MovementEntity? = db.movementDao().byId(id)
    suspend fun insertMovement(m: MovementEntity): Long = db.movementDao().insert(m.copy(cuenta = cuenta.value))
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

    companion object {
        /**
         * Tipos que se leen solo de Firestore. Lo que quedó en Room de versiones anteriores con
         * estos tipos (fiados y abonos sin nube) queda oculto: no se borra.
         */
        val EN_FIRESTORE = setOf(MovementType.GASTO.name, MovementType.FIADO.name, MovementType.ABONO.name)
    }
}
