package com.barriogo.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProfileEntity::class, ClientEntity::class, MovementEntity::class, CashSessionEntity::class, ArqueoEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun clientDao(): ClientDao
    abstract fun movementDao(): MovementDao
    abstract fun cashDao(): CashDao

    companion object {
        /** v2 · Los perfiles se unen a su documento de Firestore (Fase 3). */
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN remoteId TEXT")
            }
        }

        /** v3 · Editar y eliminar movimientos sin perder el historial (cuándo y valor original). */
        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movements ADD COLUMN voidedAt INTEGER")
                db.execSQL("ALTER TABLE movements ADD COLUMN editedAt INTEGER")
                db.execSQL("ALTER TABLE movements ADD COLUMN originalAmount INTEGER")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "barriogo.db")
                .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                .build()
    }
}
