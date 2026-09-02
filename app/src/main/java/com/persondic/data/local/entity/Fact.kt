package com.persondic.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.persondic.data.model.FactCategory
import com.persondic.data.model.Sensitivity
import com.persondic.data.model.Volatility
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "fact",
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
data class Fact(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val personId: UUID,
    val category: FactCategory,
    val body: String,
    val volatility: Volatility,
    val assertedOn: LocalDate,
    val expiresOn: LocalDate? = null,
    val confidence: Float = 1.0f,
    val sensitivity: Sensitivity = Sensitivity.NORMAL,
    val pinned: Boolean = false,
    val sourceId: UUID? = null,
    val supersededBy: UUID? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
