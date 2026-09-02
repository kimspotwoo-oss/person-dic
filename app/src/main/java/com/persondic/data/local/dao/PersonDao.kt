package com.persondic.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.persondic.data.local.entity.Person
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface PersonDao {

    @Insert
    suspend fun insert(person: Person)

    @Update
    suspend fun update(person: Person)

    @Delete
    suspend fun delete(person: Person)

    @Query("SELECT * FROM person WHERE id = :id")
    fun observeById(id: UUID): Flow<Person?>

    @Query("SELECT * FROM person ORDER BY displayName ASC")
    fun observeAll(): Flow<List<Person>>
}
