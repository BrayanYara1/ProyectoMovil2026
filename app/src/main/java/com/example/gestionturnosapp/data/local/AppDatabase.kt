package com.example.gestionturnosapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory
import com.example.gestionturnosapp.data.EstudioMedico
import com.example.gestionturnosapp.data.Medicamento
import com.example.gestionturnosapp.data.Turno

@Database(entities = [Turno::class, Medicamento::class, EstudioMedico::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun turnoDao(): TurnoDao
    abstract fun medicamentoDao(): MedicamentoDao
    abstract fun estudioDao(): EstudioDao

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
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
