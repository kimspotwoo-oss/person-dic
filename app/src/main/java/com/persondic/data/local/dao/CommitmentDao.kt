package com.persondic.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.persondic.data.local.entity.Commitment
import com.persondic.data.model.CommitmentStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CommitmentDao {

    @Insert
    suspend fun insert(commitment: Commitment)

    @Update
    suspend fun update(commitment: Commitment)

    @Delete
    suspend fun delete(commitment: Commitment)

    @Query("SELECT * FROM commitment WHERE personId = :personId ORDER BY createdAt DESC")
    fun observeForPerson(personId: UUID): Flow<List<Commitment>>

    @Query("SELECT * FROM commitment WHERE personId = :personId AND status = :status ORDER BY createdAt DESC")
    fun observeForPersonByStatus(personId: UUID, status: CommitmentStatus): Flow<List<Commitment>>
}
