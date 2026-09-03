package com.app.flashlearn.database.dao

import androidx.room.*
import com.app.flashlearn.database.entity.ConceptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConceptDao {
    @Insert
    suspend fun insert(entity: ConceptEntity): Long

    @Update
    suspend fun update(entity: ConceptEntity)

    @Query("UPDATE concept SET active = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun deactivate(id: Long, updatedAt: Long)

    @Query("SELECT * FROM concept WHERE id = :id")
    suspend fun findById(id: Long): ConceptEntity?

    @Query("SELECT * FROM concept WHERE uuid = :uuid")
    suspend fun findByUuid(uuid: String): ConceptEntity?

    @Query("""
        SELECT * FROM concept
        WHERE active = 1
          AND (:query = '' OR uuid LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun search(query: String): Flow<List<ConceptEntity>>

    @Query("SELECT * FROM concept WHERE active = 1 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConceptEntity>>

    @Query("SELECT * FROM concept WHERE active = 1 AND favorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<ConceptEntity>>
}
