package com.persondic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.persondic.data.local.entity.Tie

/**
 * Nothing in Phase 0 creates ties yet, but the table is part of the schema and backups have to
 * round-trip the whole database — a backup that silently drops a table becomes data loss the
 * moment the feature lands.
 */
@Dao
interface TieDao {

    @Query("SELECT * FROM tie")
    suspend fun getAll(): List<Tie>

    @Upsert
    suspend fun upsertAll(ties: List<Tie>)
}
