package com.app.flashlearn.database.dao

import androidx.room.*
import com.app.flashlearn.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(entity: CategoryEntity): Long

    @Update
    suspend fun update(entity: CategoryEntity)

    @Delete
    suspend fun delete(entity: CategoryEntity)

    @Query("SELECT * FROM category ORDER BY name")
    fun observeAll(): Flow<List<CategoryEntity>>
}
