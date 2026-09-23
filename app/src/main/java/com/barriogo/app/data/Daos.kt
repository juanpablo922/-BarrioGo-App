package com.barriogo.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY id") fun observeAll(): Flow<List<ProfileEntity>>
    @Query("SELECT * FROM profiles WHERE id = :id") suspend fun byId(id: Long): ProfileEntity?
    @Insert suspend fun insert(p: ProfileEntity): Long
    @Update suspend fun update(p: ProfileEntity)
    @Query("DELETE FROM profiles WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<ClientEntity>>
    @Query("SELECT * FROM clients WHERE id = :id") suspend fun byId(id: Long): ClientEntity?
    @Insert suspend fun insert(c: ClientEntity): Long
}

@Dao
interface MovementDao {
    @Query("SELECT * FROM movements ORDER BY createdAt DESC, id DESC") fun observeAll(): Flow<List<MovementEntity>>
    @Query("SELECT * FROM movements WHERE id = :id") suspend fun byId(id: Long): MovementEntity?
    @Insert suspend fun insert(m: MovementEntity): Long
    @Update suspend fun update(m: MovementEntity)
    @Query("DELETE FROM movements WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface CashDao {
    @Query("SELECT * FROM cash_sessions WHERE closedAt IS NULL ORDER BY id DESC LIMIT 1")
    fun observeActive(): Flow<CashSessionEntity?>

    @Query("SELECT * FROM cash_sessions WHERE closedAt IS NOT NULL ORDER BY id DESC LIMIT 1")
    fun observeLastClosed(): Flow<CashSessionEntity?>

    @Insert suspend fun insert(s: CashSessionEntity): Long
    @Update suspend fun update(s: CashSessionEntity)
    @Insert suspend fun insertArqueo(a: ArqueoEntity): Long
}
