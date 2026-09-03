package com.app.flashlearn.database.dao

import androidx.room.*
import com.app.flashlearn.database.entity.LanguageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: LanguageEntity)

    @Query("SELECT * FROM language WHERE code = :code")
    suspend fun findByCode(code: String): LanguageEntity?

    @Query("SELECT * FROM language ORDER BY displayName")
    fun observeAll(): Flow<List<LanguageEntity>>
}
