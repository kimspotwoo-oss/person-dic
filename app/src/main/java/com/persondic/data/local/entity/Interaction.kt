package com.persondic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.persondic.data.model.InteractionKind
import java.time.Instant
import java.util.UUID

@Entity(tableName = "interaction")
data class Interaction(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val metAt: Instant,
    val place: String? = null,
    val summary: String? = null,
    /**
     * What was actually said, as the user chose to write it down. [summary] stays the one-line
     * version for lists and the briefing; this is the part you come back to re-read.
     */
    val notes: String? = null,
    val kind: InteractionKind,
)
