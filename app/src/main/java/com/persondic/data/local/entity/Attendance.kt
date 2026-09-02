package com.persondic.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "attendance",
    primaryKeys = ["interactionId", "personId"],
    foreignKeys = [
        ForeignKey(
            entity = Interaction::class,
            parentColumns = ["id"],
            childColumns = ["interactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("personId")],
)
data class Attendance(
    val interactionId: UUID,
    val personId: UUID,
)
