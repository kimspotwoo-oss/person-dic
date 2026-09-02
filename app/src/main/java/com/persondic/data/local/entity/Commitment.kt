package com.persondic.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.persondic.data.model.CommitmentStatus
import com.persondic.data.model.Direction
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "commitment",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("personId")],
)
data class Commitment(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val personId: UUID,
    val direction: Direction,
    val body: String,
    val dueOn: LocalDate? = null,
    val status: CommitmentStatus = CommitmentStatus.OPEN,
    val createdAt: Instant = Instant.now(),
)
