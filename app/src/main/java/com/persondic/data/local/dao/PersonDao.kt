package com.persondic.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
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

    /** The people I know. The owner's own row is excluded so this stays safe by default. */
    @Query("SELECT * FROM person WHERE isSelf = 0 ORDER BY displayName ASC")
    fun observeAll(): Flow<List<Person>>

    /** Everyone including me — for the relationship graph and for picking the other end of a tie. */
    @Query("SELECT * FROM person ORDER BY isSelf DESC, displayName ASC")
    fun observeAllIncludingSelf(): Flow<List<Person>>

    @Query("SELECT * FROM person WHERE isSelf = 1 LIMIT 1")
    fun observeSelf(): Flow<Person?>

    @Query("SELECT * FROM person WHERE isSelf = 1 LIMIT 1")
    suspend fun getSelf(): Person?

    @Query("SELECT * FROM person")
    suspend fun getAll(): List<Person>

    @Upsert
    suspend fun upsertAll(people: List<Person>)
}
