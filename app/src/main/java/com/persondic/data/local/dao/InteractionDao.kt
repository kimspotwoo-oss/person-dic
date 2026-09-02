package com.persondic.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.persondic.data.local.entity.Attendance
import com.persondic.data.local.entity.Interaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

@Dao
interface InteractionDao {

    @Insert
    suspend fun insert(interaction: Interaction)

    @Update
    suspend fun update(interaction: Interaction)

    @Delete
    suspend fun delete(interaction: Interaction)

    @Insert
    suspend fun insertAttendance(attendance: Attendance)

    @Query(
        """
        SELECT interaction.* FROM interaction
        INNER JOIN attendance ON attendance.interactionId = interaction.id
        WHERE attendance.personId = :personId
        ORDER BY metAt DESC
        """,
    )
    fun observeForPerson(personId: UUID): Flow<List<Interaction>>

    @Query(
        """
        SELECT MAX(interaction.metAt) FROM interaction
        INNER JOIN attendance ON attendance.interactionId = interaction.id
        WHERE attendance.personId = :personId
        """,
    )
    suspend fun lastInteractionAt(personId: UUID): Instant?
}
