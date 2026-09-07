package com.persondic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.persondic.data.local.entity.PersonAttribute
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface PersonAttributeDao {

    @Upsert
    suspend fun upsert(attribute: PersonAttribute)

    @Query("DELETE FROM person_attribute WHERE personId = :personId AND label = :label")
    suspend fun delete(personId: UUID, label: String)

    @Query("SELECT * FROM person_attribute WHERE personId = :personId ORDER BY sortOrder ASC, label ASC")
    fun observeForPerson(personId: UUID): Flow<List<PersonAttribute>>

    @Query("SELECT DISTINCT label FROM person_attribute ORDER BY label ASC")
    fun observeAllLabels(): Flow<List<String>>

    @Query("SELECT * FROM person_attribute")
    suspend fun getAll(): List<PersonAttribute>

    @Upsert
    suspend fun upsertAll(attributes: List<PersonAttribute>)
}
