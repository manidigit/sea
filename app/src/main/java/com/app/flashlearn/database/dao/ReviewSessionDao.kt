package com.app.flashlearn.database.dao

import androidx.room.*
import com.app.flashlearn.database.entity.ReviewSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewSessionDao {
    @Insert
    suspend fun insert(entity: ReviewSessionEntity)

    @Query("UPDATE review_session SET endedAt = :endedAt WHERE id = :id")
    suspend fun closeSession(id: String, endedAt: Long)

    @Query("SELECT * FROM review_session WHERE id = :id")
    suspend fun findById(id: String): ReviewSessionEntity?

    @Query("SELECT * FROM review_session WHERE id = :id")
    fun observeById(id: String): Flow<ReviewSessionEntity?>
}
