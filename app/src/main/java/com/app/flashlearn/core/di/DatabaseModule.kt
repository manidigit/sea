package com.app.flashlearn.core.di

import android.content.Context
import androidx.room.Room
import com.app.flashlearn.database.FlashLearnDatabase
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
    fun provideDatabase(
        @ApplicationContext context: Context
    ): FlashLearnDatabase =
        Room.databaseBuilder(
            context,
            FlashLearnDatabase::class.java,
            "flashlearn.db"
        ).build()
}
