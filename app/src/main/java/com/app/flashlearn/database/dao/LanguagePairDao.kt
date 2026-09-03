package com.app.flashlearn.database.dao

import androidx.room.*
import com.app.flashlearn.database.entity.LanguagePairEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguagePairDao {
    @Insert
    suspend fun insert(entity: LanguagePairEntity): Long

    @Update
    suspend fun update(entity: LanguagePairEntity)

    @Query("SELECT * FROM language_pair WHERE isActive = 1 LIMIT 1")
    suspend fun findActive(): LanguagePairEntity?

    @Query("SELECT * FROM language_pair WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<LanguagePairEntity?>
}
