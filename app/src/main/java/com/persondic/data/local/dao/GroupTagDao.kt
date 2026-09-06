package com.persondic.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.persondic.data.local.entity.PersonGroupTag
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface GroupTagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: PersonGroupTag)

    @Query("DELETE FROM person_group_tag WHERE personId = :personId AND tag = :tag")
    suspend fun delete(personId: UUID, tag: String)

    @Query("SELECT tag FROM person_group_tag WHERE personId = :personId ORDER BY tag ASC")
    fun observeTagsForPerson(personId: UUID): Flow<List<String>>

    @Query("SELECT DISTINCT tag FROM person_group_tag ORDER BY tag ASC")
    fun observeAllTags(): Flow<List<String>>

    @Query("SELECT * FROM person_group_tag")
    fun observeAll(): Flow<List<PersonGroupTag>>

    @Query("SELECT DISTINCT personId FROM person_group_tag WHERE tag LIKE '%' || :query || '%'")
    suspend fun findPersonIdsMatchingTag(query: String): List<UUID>
}
