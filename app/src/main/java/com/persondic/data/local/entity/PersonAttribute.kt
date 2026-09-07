package com.persondic.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.persondic.data.model.Sensitivity
import java.util.UUID

/**
 * A fixed, labelled piece of information about a person — 혈액형, MBTI, 고향 and the like.
 *
 * Separate from Fact because these never expire and must never appear under "확인 필요". Free-form
 * labels rather than fixed columns, so a new kind of fixed information costs nothing: no schema
 * change, no migration.
 */
@Entity(
    tableName = "person_attribute",
    primaryKeys = ["personId", "label"],
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
data class PersonAttribute(
    val personId: UUID,
    val label: String,
    val value: String,
    /**
     * Set per entry, because the most sensitive fixed information is exactly the kind that fits
     * here — 종교, 건강, 정치 성향 are all "one value everyone has". Without this they would sit
     * in plain view at the top of the briefing while an equivalent Fact stays gated.
     */
    val sensitivity: Sensitivity = Sensitivity.NORMAL,
    val sortOrder: Int = 0,
)
