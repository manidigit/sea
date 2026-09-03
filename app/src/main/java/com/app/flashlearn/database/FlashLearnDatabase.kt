package com.app.flashlearn.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.flashlearn.database.dao.*
import com.app.flashlearn.database.entity.*

@Database(
    entities = [
        LanguageEntity::class,
        LanguagePairEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        ConceptEntity::class,
        ContentEntity::class,
        LearningStateEntity::class,
        ReviewSessionEntity::class,
        ReviewHistoryEntity::class,
        ConceptTagEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FlashLearnDatabase : RoomDatabase() {
    abstract fun languageDao(): LanguageDao
    abstract fun languagePairDao(): LanguagePairDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun conceptTagDao(): ConceptTagDao
    abstract fun conceptDao(): ConceptDao
    abstract fun contentDao(): ContentDao
    abstract fun learningStateDao(): LearningStateDao
    abstract fun reviewSessionDao(): ReviewSessionDao
    abstract fun reviewHistoryDao(): ReviewHistoryDao
    abstract fun appSettingDao(): AppSettingDao
}
