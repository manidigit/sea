package com.app.flashlearn.database.dao

import androidx.room.*
import com.app.flashlearn.database.entity.LearningStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningStateDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: LearningStateEntity)

    @Update
    suspend fun update(entity: LearningStateEntity)

    @Query("SELECT * FROM learning_state WHERE conceptId = :conceptId")
    suspend fun findByConcept(conceptId: Long): LearningStateEntity?

    @Query("SELECT * FROM learning_state WHERE conceptId = :conceptId")
    fun observeByConcept(conceptId: Long): Flow<LearningStateEntity?>

    @Query("""
        SELECT * FROM learning_state
        WHERE stage = :stage
          AND nextReviewAt IS NOT NULL
          AND nextReviewAt <= :now
        ORDER BY nextReviewAt ASC
    """)
    suspend fun findDueByStage(stage: String, now: Long): List<LearningStateEntity>

    @Query("""
        UPDATE learning_state
        SET stage = :newStage,
            difficulty = :newDifficulty,
            nextReviewAt = :nextReviewAt,
            monthlyWrongCount = :monthlyWrongCount,
            totalCorrect = :totalCorrect,
            totalWrong = :totalWrong,
            lastReviewedAt = :lastReviewedAt
        WHERE conceptId = :conceptId
          AND stage = :expectedStage
          AND difficulty = :expectedDifficulty
    """)
    suspend fun updateOptimistic(
        conceptId: Long,
        expectedStage: String,
        expectedDifficulty: String,
        newStage: String,
        newDifficulty: String,
        nextReviewAt: Long?,
        monthlyWrongCount: Int,
        totalCorrect: Int,
        totalWrong: Int,
        lastReviewedAt: Long?
    ): Int
}
