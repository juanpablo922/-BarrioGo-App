package com.barriogo.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProfileEntity::class, ClientEntity::class, MovementEntity::class, CashSessionEntity::class, ArqueoEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun clientDao(): ClientDao
    abstract fun movementDao(): MovementDao
    abstract fun cashDao(): CashDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "barriogo.db").build()
    }
}
