package com.hsissa.zentra.di

import android.content.Context
import com.google.gson.Gson
import com.hsissa.zentra.data.local.AppDatabase
import com.hsissa.zentra.data.local.UsageDao
import com.hsissa.zentra.data.repository.UsageRepository
import com.hsissa.zentra.domain.repository.UsageRepositoryContract
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun provideUsageDao(database: AppDatabase): UsageDao =
        database.usageDao()

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideUsageRepository(repository: UsageRepository): UsageRepositoryContract =
        repository
}
