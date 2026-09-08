package com.persondic.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tie",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["fromPersonId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["toPersonId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fromPersonId"), Index("toPersonId")],
)
data class Tie(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val fromPersonId: UUID,
    val toPersonId: UUID,
    val label: String,
    /**
     * True when the label reads the same from either end — 친구, 배우자, 형제자매.
     *
     * Stored rather than guessed from the label, because a label the user invents cannot be
     * looked up in any list the app ships with. The graph draws a plain line for these and an
     * arrow for the rest, and the arrow points at [toPersonId].
     */
    val symmetric: Boolean = false,
)
