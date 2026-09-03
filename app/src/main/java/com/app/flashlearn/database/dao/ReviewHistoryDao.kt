package com.app.flashlearn.database.dao

import androidx.room.*
import com.app.flashlearn.database.entity.ReviewHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewHistoryDao {
    @Insert
    suspend fun insert(entity: ReviewHistoryEntity): Long

    @Query("SELECT * FROM review_history WHERE conceptId = :conceptId ORDER BY reviewDate DESC")
    fun findByConcept(conceptId: Long): Flow<List<ReviewHistoryEntity>>

    @Query("SELECT * FROM review_history WHERE sessionId = :sessionId ORDER BY reviewDate ASC")
    fun findBySession(sessionId: String): Flow<List<ReviewHistoryEntity>>

    @Query("""
        SELECT * FROM review_history
        WHERE sessionId = :sessionId AND reviewAttemptId = :reviewAttemptId
        LIMIT 1
    """)
    suspend fun findByAttempt(sessionId: String, reviewAttemptId: String): ReviewHistoryEntity?
}
