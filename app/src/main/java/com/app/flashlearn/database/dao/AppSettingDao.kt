package com.app.flashlearn.database.dao

import androidx.room.*
import com.app.flashlearn.database.entity.AppSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_setting WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): AppSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppSettingEntity)

    @Query("DELETE FROM app_setting WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("SELECT * FROM app_setting ORDER BY `key`")
    fun observe(): Flow<List<AppSettingEntity>>
}
