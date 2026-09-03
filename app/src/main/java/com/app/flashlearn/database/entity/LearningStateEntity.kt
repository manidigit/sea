package com.app.flashlearn.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "learning_state",
    primaryKeys = ["conceptId"],
    indices = [
        Index(value = ["stage"]),
        Index(value = ["stage", "nextReviewAt"]),
        Index(value = ["difficulty"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConceptEntity::class,
            parentColumns = ["id"],
            childColumns = ["conceptId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LearningStateEntity(
    val conceptId: Long,
    val stage: String,
    val difficulty: String,
    val nextReviewAt: Long?,
    val monthlyWrongCount: Int = 0,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val lastReviewedAt: Long?
)
