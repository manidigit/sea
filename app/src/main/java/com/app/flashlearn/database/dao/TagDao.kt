package com.app.flashlearn.database.dao

import androidx.room.*
import com.app.flashlearn.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert
    suspend fun insert(entity: TagEntity): Long

    @Delete
    suspend fun delete(entity: TagEntity)

    @Query("SELECT * FROM tag WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Query("SELECT * FROM tag ORDER BY name")
    fun observeAll(): Flow<List<TagEntity>>
}
