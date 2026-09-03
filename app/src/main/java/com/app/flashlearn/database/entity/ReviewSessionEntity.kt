package com.app.flashlearn.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_session",
    indices = [
        Index(value = ["startedAt"]),
        Index(value = ["reviewType"])
    ]
)
data class ReviewSessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val reviewType: String
)
