package com.barriogo.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProfileEntity::class, ClientEntity::class, MovementEntity::class, CashSessionEntity::class, ArqueoEntity::class,
        CashChangeEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun clientDao(): ClientDao
    abstract fun movementDao(): MovementDao

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

        /**
         * v4 · Caja diaria (día de operación, correcciones del cierre y su registro) y sincronización
         * de cajas y gastos con Firestore (remoteId). Las cajas viejas toman el día en que se abrieron.
         */
        private val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cash_sessions ADD COLUMN operationDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cash_sessions ADD COLUMN closeEditsUsed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cash_sessions ADD COLUMN remoteId TEXT")
                db.execSQL(
                    "UPDATE cash_sessions SET operationDate = strftime('%Y-%m-%d', openedAt / 1000, 'unixepoch', 'localtime')"
                )
                db.execSQL("ALTER TABLE movements ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE movements ADD COLUMN category TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS cash_changes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, kind TEXT NOT NULL, " +
                        "number INTEGER NOT NULL, `before` INTEGER NOT NULL, `after` INTEGER NOT NULL, note TEXT NOT NULL, " +
                        "`by` TEXT NOT NULL, `at` INTEGER NOT NULL)"
                )
            }
        }

        /**
         * v5 · La caja y los gastos se leen de Firestore. Room guarda solo ventas, fiados y abonos,
         * ahora por tienda (cuenta) y unidos a la caja de Firestore (cajaId). Lo que ya estaba guardado
         * no se sabe de qué tienda es: queda con cuenta '' y ninguna cuenta lo ve (no se borra).
         */
        private val MIGRACION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movements ADD COLUMN cuenta TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE movements ADD COLUMN cajaId TEXT")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "barriogo.db")
                .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4, MIGRACION_4_5)
                .build()
    }
}
