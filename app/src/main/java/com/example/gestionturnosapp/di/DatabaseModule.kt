package com.example.gestionturnosapp.di

import android.content.Context
import com.example.gestionturnosapp.data.local.AppDatabase
import com.example.gestionturnosapp.data.local.EstudioDao
import com.example.gestionturnosapp.data.local.MedicamentoDao
import com.example.gestionturnosapp.data.local.TurnoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideTurnoDao(database: AppDatabase): TurnoDao {
        return database.turnoDao()
    }

    @Provides
    fun provideMedicamentoDao(database: AppDatabase): MedicamentoDao {
        return database.medicamentoDao()
    }

    @Provides
    fun provideEstudioDao(database: AppDatabase): EstudioDao {
        return database.estudioDao()
    }
}
