package com.app.flashlearn.domain.model

data class LearningState(
    val conceptId: Long,
    val stage: LearningStage,
    val difficulty: Difficulty,
    val nextReviewAt: Long?,
    val monthlyWrongCount: Int,
    val totalCorrect: Int,
    val totalWrong: Int,
    val lastReviewedAt: Long?
)
