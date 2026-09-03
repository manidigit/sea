package com.app.flashlearn.database.dao

import androidx.room.*
import com.app.flashlearn.database.entity.ConceptTagEntity
import com.app.flashlearn.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConceptTagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ConceptTagEntity)

    @Query("DELETE FROM concept_tag WHERE conceptId = :conceptId AND tagId = :tagId")
    suspend fun delete(conceptId: Long, tagId: Long)

    @Query("""
        SELECT t.* FROM tag t
        INNER JOIN concept_tag ct ON ct.tagId = t.id
        WHERE ct.conceptId = :conceptId
        ORDER BY t.name
    """)
    fun findTagsForConcept(conceptId: Long): Flow<List<TagEntity>>

    @Query("SELECT conceptId FROM concept_tag WHERE tagId = :tagId")
    fun findConceptsForTag(tagId: Long): Flow<List<Long>>
}
