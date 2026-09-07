package com.persondic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "person")
data class Person(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val displayName: String,
    val alias: String? = null,
    val groupTag: String? = null,
    val metOn: LocalDate? = null,
    val metStory: String? = null,
    val photoUri: String? = null,
    /**
     * Fixed information, kept out of Fact on purpose: a birthday never goes stale, so it must
     * never pick up an expiry date or turn up under "확인 필요".
     *
     * When the year is unknown the date still stores a year ([BIRTHDAY_YEAR_UNKNOWN]) because
     * SQLite has no month-day type; [birthdayHasYear] says whether to show it.
     */
    val birthday: LocalDate? = null,
    val birthdayHasYear: Boolean = true,
    val birthdayIsLunar: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

/**
 * Stand-in year for a birthday whose year nobody knows. A leap year, so 2월 29일 survives
 * being stored and read back.
 */
const val BIRTHDAY_YEAR_UNKNOWN = 2000
