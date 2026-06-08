package com.hsissa.zentra.di

import android.content.Context
import com.hsissa.zentra.data.local.AppDatabase
import com.hsissa.zentra.data.local.UsageDao
import com.hsissa.zentra.data.repository.UsageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideUsageDao(database: AppDatabase): UsageDao {
        return database.usageDao()
    }

    @Provides
    @Singleton
    fun provideUsageRepository(@ApplicationContext context: Context, usageDao: UsageDao): UsageRepository {
        return UsageRepository(context, usageDao)
    }
}
