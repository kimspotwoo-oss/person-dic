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
    val kind: InteractionKind,
)
