package com.persondic.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.persondic.data.local.entity.Fact
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface FactDao {

    @Insert
    suspend fun insert(fact: Fact)

    @Update
    suspend fun update(fact: Fact)

    @Delete
    suspend fun delete(fact: Fact)

    @Query("SELECT * FROM fact WHERE id = :id")
    fun observeById(id: UUID): Flow<Fact?>

    @Query("SELECT * FROM fact WHERE personId = :personId ORDER BY createdAt DESC")
    fun observeForPerson(personId: UUID): Flow<List<Fact>>

    @Query("SELECT DISTINCT personId FROM fact WHERE body LIKE '%' || :query || '%'")
    suspend fun findPersonIdsMatchingBody(query: String): List<UUID>
}
