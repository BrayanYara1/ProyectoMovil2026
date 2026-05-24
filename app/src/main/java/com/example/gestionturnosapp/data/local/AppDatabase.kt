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

        private const val DB_NAME = "gestion_turnos_db_secure"

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = try {
                    buildDatabase(context)
                } catch (t: Throwable) {
                    android.util.Log.e("AppDatabase", "Error building secure database, attempting recover", t)
                    try {
                        // Si falla (ej: contraseña incorrecta o cambio de esquema radical), borramos y recreamos
                        context.deleteDatabase(DB_NAME)
                        buildDatabase(context)
                    } catch (t2: Throwable) {
                        // Último recurso: base de datos sin cifrado para evitar crash persistente
                        Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "${DB_NAME}_unencrypted"
                        ).fallbackToDestructiveMigration().build()
                    }
                }
                INSTANCE = instance
                instance
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val passphrase = net.sqlcipher.database.SQLiteDatabase.getBytes("SaludActiva_Secret_Key_2024".toCharArray())
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
