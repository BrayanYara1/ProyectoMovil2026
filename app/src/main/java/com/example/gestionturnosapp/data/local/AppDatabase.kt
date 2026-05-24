package com.example.gestionturnosapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.sqlcipher.database.SupportFactory
import com.example.gestionturnosapp.data.model.*

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    buildDatabase(context)
                } catch (t: Throwable) {
                    android.util.Log.e("AppDatabase", "Error building database, attempting to recover", t)
                    try {
                        context.deleteDatabase("gestion_turnos_db")
                        buildDatabase(context)
                    } catch (t2: Throwable) {
                        // Último recurso: crear sin cifrado si SQLCipher falla catastróficamente
                        Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "gestion_turnos_db_unencrypted"
                        ).fallbackToDestructiveMigration().build()
                    }
                }
            }.also { INSTANCE = it }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            // TEMPORAL: Desactivar cifrado SQLCipher para diagnosticar el crash
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "gestion_turnos_db_diag",
            )
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
