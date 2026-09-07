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
     * The month and day of the birthday, and nothing more. SQLite has no month-day type, so the
     * year is always [BIRTHDAY_YEAR_UNKNOWN] here and must never be read — the real year lives in
     * [birthYear].
     *
     * Fixed information is kept out of Fact on purpose: a birthday never goes stale, so it must
     * never pick up an expiry date or turn up under "확인 필요".
     */
    val birthday: LocalDate? = null,
    /**
     * 몇년생. Independent of [birthday] because the two are usually learned separately: you often
     * know someone is 94년생 long before you know the date, and you often know the date without
     * ever learning the year.
     */
    val birthYear: Int? = null,
    val birthdayIsLunar: Boolean = false,
    /**
     * Legacy. Superseded by `birthYear != null` and never read.
     *
     * It stays because dropping a column from `person` means recreating the table, and a
     * DROP TABLE inside a Room migration fires ON DELETE CASCADE on every child row — facts,
     * commitments, group tags, attributes, attendances and ties would all be deleted. Verified
     * against SQLite directly; neither `defer_foreign_keys` nor `legacy_alter_table` avoids it,
     * because both pragmas are ignored inside the migration's transaction. A dead column is a
     * far smaller price. The repository keeps it mirroring `birthYear != null`, and the default
     * matches [birthYear]'s so a newly built Person is never self-contradictory.
     */
    val birthdayHasYear: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

/**
 * Stand-in year for a birthday whose year nobody knows. A leap year, so 2월 29일 survives
 * being stored and read back.
 */
const val BIRTHDAY_YEAR_UNKNOWN = 2000
