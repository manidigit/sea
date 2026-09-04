import os
import zipfile

project_files = {
    "sea/app/build.gradle.kts": '''
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.app.sea"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.app.sea"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
}
''',
    "sea/app/src/main/java/com/app/sea/SeaApplication.kt": '''
package com.app.sea

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SeaApplication : Application()
''',
    "sea/app/src/main/java/com/app/sea/database/entity/Entities.kt": '''
package com.app.sea.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "language")
data class LanguageEntity(
    @PrimaryKey val code: String,
    val displayName: String
)

@Entity(tableName = "category", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCustom: Boolean = false
)

@Entity(
    tableName = "concept",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["categoryId"]),
        Index(value = ["active"]),
        Index(value = ["favorite"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ConceptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val contentType: String,
    val categoryId: Long?,
    val favorite: Boolean,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "content",
    indices = [
        Index(value = ["conceptId"]),
        Index(value = ["languageCode"]),
        Index(value = ["conceptId", "languageCode"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConceptEntity::class,
            parentColumns = ["id"],
            childColumns = ["conceptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LanguageEntity::class,
            parentColumns = ["code"],
            childColumns = ["languageCode"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class ContentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conceptId: Long,
    val languageCode: String,
    val text: String,
    val pronunciation: String?,
    val definition: String?,
    val example: String?,
    val grammarNote: String?,
    val usageNote: String?
)

@Entity(
    tableName = "learning_state",
    indices = [
        Index(value = ["stage"]),
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
    @PrimaryKey val conceptId: Long,
    val stage: String,
    val difficulty: String,
    val nextReviewAt: Long?,
    val monthlyWrongCount: Int,
    val totalCorrect: Int,
    val totalWrong: Int,
    val lastReviewedAt: Long?
)

@Entity(tableName = "review_session")
data class ReviewSessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val reviewType: String
)

@Entity(
    tableName = "review_history",
    indices = [
        Index(value = ["conceptId"]),
        Index(value = ["sessionId"]),
        Index(value = ["reviewDate"])
    ]
)
data class ReviewHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conceptId: Long,
    val sessionId: String,
    val reviewAttemptId: String,
    val reviewStage: String,
    val reviewDate: Long,
    val isCorrect: Boolean,
    val previousStatus: String,
    val newStatus: String,
    val previousDifficulty: String,
    val newDifficulty: String,
    val responseTimeMs: Long?
)
''',
    "sea/app/src/main/java/com/app/sea/domain/engine/ReviewTransitionEngine.kt": '''
package com.app.sea.domain.engine

import java.time.Instant
import java.time.temporal.ChronoUnit

enum class ReviewStage { DAILY, WEEKLY, MONTHLY, LEARNED }
enum class Difficulty { EASY, MEDIUM, HARD, VERY_HARD }
enum class ReviewAnswer { CORRECT, INCORRECT }

data class LearningStateDomain(
    val conceptId: Long,
    val stage: ReviewStage,
    val difficulty: Difficulty,
    val nextReviewAt: Long?,
    val monthlyWrongCount: Int,
    val totalCorrect: Int,
    val totalWrong: Int,
    val lastReviewedAt: Long?
)

data class TransitionResult(
    val updatedState: LearningStateDomain,
    val previousStage: ReviewStage,
    val newStage: ReviewStage,
    val previousDifficulty: Difficulty,
    val newDifficulty: Difficulty
)

object ReviewTransitionEngine {

    fun calculateTransition(
        currentState: LearningStateDomain,
        answer: ReviewAnswer,
        now: Instant,
        isFirstTimeFullPass: Boolean = false
    ): TransitionResult {
        val nowMillis = now.toEpochMilli()

        var newStage = currentState.stage
        var newDifficulty = currentState.difficulty
        var newNextReviewAt: Long? = currentState.nextReviewAt
        var newMonthlyWrongCount = currentState.monthlyWrongCount
        var newTotalCorrect = currentState.totalCorrect
        var newTotalWrong = currentState.totalWrong

        when (answer) {
            ReviewAnswer.CORRECT -> {
                newTotalCorrect += 1
                when (currentState.stage) {
                    ReviewStage.DAILY -> {
                        newStage = ReviewStage.WEEKLY
                        newNextReviewAt = now.plus(7, ChronoUnit.DAYS).toEpochMilli()
                    }
                    ReviewStage.WEEKLY -> {
                        newStage = ReviewStage.MONTHLY
                        newNextReviewAt = now.plus(30, ChronoUnit.DAYS).toEpochMilli()
                    }
                    ReviewStage.MONTHLY -> {
                        newStage = ReviewStage.LEARNED
                        newNextReviewAt = null
                        if (isFirstTimeFullPass) {
                            newDifficulty = Difficulty.EASY
                        }
                    }
                    ReviewStage.LEARNED -> {
                        newNextReviewAt = null
                    }
                }
            }
            ReviewAnswer.INCORRECT -> {
                newTotalWrong += 1
                when (currentState.stage) {
                    ReviewStage.DAILY -> {
                        newStage = ReviewStage.DAILY
                        newNextReviewAt = nowMillis
                    }
                    ReviewStage.WEEKLY -> {
                        newStage = ReviewStage.DAILY
                        newNextReviewAt = nowMillis
                        if (currentState.difficulty == Difficulty.EASY) {
                            newDifficulty = Difficulty.MEDIUM
                        }
                    }
                    ReviewStage.MONTHLY -> {
                        newStage = ReviewStage.DAILY
                        newNextReviewAt = nowMillis
                        newMonthlyWrongCount += 1
                        newDifficulty = if (newMonthlyWrongCount > 1) {
                            Difficulty.VERY_HARD
                        } else {
                            Difficulty.HARD
                        }
                    }
                    ReviewStage.LEARNED -> {
                        newNextReviewAt = null
                    }
                }
            }
        }

        val updatedState = currentState.copy(
            stage = newStage,
            difficulty = newDifficulty,
            nextReviewAt = newNextReviewAt,
            monthlyWrongCount = newMonthlyWrongCount,
            totalCorrect = newTotalCorrect,
            totalWrong = newTotalWrong,
            lastReviewedAt = nowMillis
        )

        return TransitionResult(
            updatedState = updatedState,
            previousStage = currentState.stage,
            newStage = newStage,
            previousDifficulty = currentState.difficulty,
            newDifficulty = newDifficulty
        )
    }
}
''',
    "sea/app/src/main/java/com/app/sea/util/BackupRestoreManager.kt": '''
package com.app.sea.util

import android.content.Context
import com.app.sea.database.SeaDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRestoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: SeaDatabase
) {
    private val dbName = "sea_database.db"

    fun exportDatabase(destFile: File): Result<Unit> = runCatching {
        database.openHelper.writableDatabase.query("PRAGMA checkpoint_full;").close()
        val currentDbFile = context.getDatabasePath(dbName)
        if (!currentDbFile.exists()) throw IllegalStateException("Database file not found.")

        FileInputStream(currentDbFile).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    fun restoreDatabase(sourceFile: File): Result<Unit> = runCatching {
        database.close()
        val currentDbFile = context.getDatabasePath(dbName)
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(currentDbFile).use { output ->
                input.copyTo(output)
            }
        }
    }
}
'''
}

with zipfile.ZipFile('sea_project.zip', 'w', zipfile.ZIP_DEFLATED) as zipf:
    for file_path, content in project_files.items():
        zipf.writestr(file_path, content.strip())

print("--- فایل sea_project.zip با موفقیت در همین مسیر ساخته شد ---")
