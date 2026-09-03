package com.app.flashlearn.database.dao

import androidx.room.*
import com.app.flashlearn.database.entity.ContentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ContentEntity): Long

    @Update
    suspend fun update(entity: ContentEntity)

    @Delete
    suspend fun delete(entity: ContentEntity)

    @Query("SELECT * FROM content WHERE conceptId = :conceptId")
    fun findByConcept(conceptId: Long): Flow<List<ContentEntity>>

    @Query("SELECT * FROM content WHERE conceptId = :conceptId AND languageCode = :languageCode LIMIT 1")
    suspend fun findByConceptAndLanguage(conceptId: Long, languageCode: String): ContentEntity?

    @Query("""
        SELECT * FROM content
        WHERE languageCode = :languageCode
          AND text LIKE '%' || :query || '%'
        ORDER BY text
    """)
    fun searchByText(languageCode: String, query: String): Flow<List<ContentEntity>>
}
