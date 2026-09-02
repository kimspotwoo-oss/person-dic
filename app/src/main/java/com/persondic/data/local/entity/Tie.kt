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
)
