package com.app.flashlearn.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "language_pair",
    indices = [
        Index(value = ["sourceLanguage", "targetLanguage"], unique = true),
        Index(value = ["isActive"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = LanguageEntity::class,
            parentColumns = ["code"],
            childColumns = ["sourceLanguage"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = LanguageEntity::class,
            parentColumns = ["code"],
            childColumns = ["targetLanguage"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class LanguagePairEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceLanguage: String,
    val targetLanguage: String,
    val isActive: Boolean = false
)
