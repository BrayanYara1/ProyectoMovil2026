package com.example.gestionturnosapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.sqlcipher.database.SupportFactory
import com.example.gestionturnosapp.data.EstudioMedico
import com.example.gestionturnosapp.data.Medicamento
import com.example.gestionturnosapp.data.Turno
import com.example.gestionturnosapp.data.Mensaje

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
                // Passphrase para el cifrado (Idealmente obtenida de Keystore)
                val passphrase = net.sqlcipher.database.SQLiteDatabase.getBytes("gestion_turnos_secure_key".toCharArray())
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestion_turnos_db",
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
