package com.example.gestionturnosapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import com.example.gestionturnosapp.data.model.*
import java.util.concurrent.Executors

@Database(entities = [Turno::class, Medicamento::class, EstudioMedico::class, Mensaje::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun turnoDao(): TurnoDao
    abstract fun medicamentoDao(): MedicamentoDao
    abstract fun estudioDao(): EstudioDao
    abstract fun mensajeDao(): MensajeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DB_NAME = "gestion_turnos_db_secure"

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val currentInstance = INSTANCE
                if (currentInstance != null) return currentInstance

                try {
                    val instance = buildDatabase(context)
                    INSTANCE = instance
                    instance
                } catch (t: Throwable) {
                    android.util.Log.e("AppDatabase", "Error opening database, attempting recovery", t)
                    try {
                        context.deleteDatabase(DB_NAME)
                        val instance = buildDatabase(context)
                        INSTANCE = instance
                        instance
                    } catch (t2: Throwable) {
                        android.util.Log.e("AppDatabase", "Fallback recovery failed", t2)
                        // Fallback extremo
                        val instance = Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "${DB_NAME}_emergency"
                        ).fallbackToDestructiveMigration().build()
                        INSTANCE = instance
                        instance
                    }
                }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            // Usamos un charset explícito para asegurar consistencia entre dispositivos
            val passphrase = "SaludActiva_Secret_Key_2024".toByteArray(Charsets.UTF_8)
            val factory = SupportOpenHelperFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
            .openHelperFactory(factory)
            // Forzamos un solo hilo para evitar 'database is locked' en SQLCipher con múltiples corrutinas
            .setQueryExecutor(Executors.newSingleThreadExecutor())
            .setTransactionExecutor(Executors.newSingleThreadExecutor())
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
