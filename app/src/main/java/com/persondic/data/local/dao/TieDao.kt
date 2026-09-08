package com.persondic.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.persondic.data.local.entity.Tie
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Ties are undirected on screen but stored one way round; see domain/TieDescription.kt. */
@Dao
interface TieDao {

    @Query("SELECT * FROM tie WHERE fromPersonId = :personId OR toPersonId = :personId")
    fun observeForPerson(personId: UUID): Flow<List<Tie>>

    @Query("SELECT * FROM tie")
    fun observeAll(): Flow<List<Tie>>

    @Query("SELECT DISTINCT label FROM tie ORDER BY label ASC")
    fun observeAllLabels(): Flow<List<String>>

    @Insert
    suspend fun insert(tie: Tie)

    @Query("DELETE FROM tie WHERE id = :id")
    suspend fun delete(id: UUID)

    @Query("SELECT * FROM tie")
    suspend fun getAll(): List<Tie>

    @Upsert
    suspend fun upsertAll(ties: List<Tie>)
}
