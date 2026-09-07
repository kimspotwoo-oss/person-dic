package com.persondic.data.backup

import com.persondic.data.local.entity.Attendance
import com.persondic.data.local.entity.BIRTHDAY_YEAR_UNKNOWN
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonAttribute
import com.persondic.data.local.entity.PersonGroupTag
import com.persondic.data.local.entity.Tie
import com.persondic.data.model.CommitmentStatus
import com.persondic.data.model.Direction
import com.persondic.data.model.FactCategory
import com.persondic.data.model.InteractionKind
import com.persondic.data.model.Sensitivity
import com.persondic.data.model.Volatility
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Converts between Room entities and the backup DTOs.
 *
 * Reading is deliberately forgiving: a row with an unreadable id is dropped (it has no usable
 * primary key) but a row with an unknown enum name or a malformed date keeps its text and falls
 * back to a default, because losing one attribute is better than losing a whole person.
 */

// Export

fun Person.toBackup(photoName: String?): BackupPerson = BackupPerson(
    id = id.toString(),
    displayName = displayName,
    alias = alias,
    groupTag = groupTag,
    metOn = metOn?.toString(),
    metStory = metStory,
    photo = photoName,
    birthday = birthday?.toString(),
    birthYear = birthYear,
    birthdayIsLunar = birthdayIsLunar,
    birthdayHasYear = birthYear != null,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun Fact.toBackup(): BackupFact = BackupFact(
    id = id.toString(),
    personId = personId.toString(),
    category = category.name,
    body = body,
    volatility = volatility.name,
    assertedOn = assertedOn.toString(),
    expiresOn = expiresOn?.toString(),
    confidence = confidence,
    sensitivity = sensitivity.name,
    pinned = pinned,
    sourceId = sourceId?.toString(),
    supersededBy = supersededBy?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun Interaction.toBackup(): BackupInteraction = BackupInteraction(
    id = id.toString(),
    metAt = metAt.toString(),
    place = place,
    summary = summary,
    notes = notes,
    kind = kind.name,
)

fun Attendance.toBackup(): BackupAttendance = BackupAttendance(
    interactionId = interactionId.toString(),
    personId = personId.toString(),
)

fun Commitment.toBackup(): BackupCommitment = BackupCommitment(
    id = id.toString(),
    personId = personId.toString(),
    direction = direction.name,
    body = body,
    dueOn = dueOn?.toString(),
    status = status.name,
    createdAt = createdAt.toString(),
)

fun PersonGroupTag.toBackup(): BackupGroupTag = BackupGroupTag(
    personId = personId.toString(),
    tag = tag,
)

fun PersonAttribute.toBackup(): BackupAttribute = BackupAttribute(
    personId = personId.toString(),
    label = label,
    value = value,
    sensitivity = sensitivity.name,
    sortOrder = sortOrder,
)

fun Tie.toBackup(): BackupTie = BackupTie(
    id = id.toString(),
    fromPersonId = fromPersonId.toString(),
    toPersonId = toPersonId.toString(),
    label = label,
)

// Import

fun BackupPerson.toEntity(photoPath: String?): Person? {
    val personId = id.toUuidOrNull() ?: return null
    val name = displayName.trim().ifEmpty { return null }
    return Person(
        id = personId,
        displayName = name,
        alias = alias,
        groupTag = groupTag,
        metOn = metOn.toLocalDateOrNull(),
        metStory = metStory,
        photoUri = photoPath,
        birthday = storedBirthMonthDay(),
        birthYear = storedBirthYear(),
        birthdayIsLunar = birthdayIsLunar,
        birthdayHasYear = storedBirthYear() != null,
        createdAt = createdAt.toInstantOrNow(),
        updatedAt = updatedAt.toInstantOrNow(),
    )
}

fun BackupFact.toEntity(): Fact? {
    val factId = id.toUuidOrNull() ?: return null
    val owner = personId.toUuidOrNull() ?: return null
    return Fact(
        id = factId,
        personId = owner,
        category = category.toEnumOrDefault(FactCategory.LIFE),
        body = body,
        volatility = volatility.toEnumOrDefault(Volatility.SEASONAL),
        assertedOn = assertedOn.toLocalDateOrNull() ?: LocalDate.now(),
        expiresOn = expiresOn.toLocalDateOrNull(),
        confidence = confidence,
        sensitivity = sensitivity.toEnumOrDefault(Sensitivity.NORMAL),
        pinned = pinned,
        sourceId = sourceId.toUuidOrNull(),
        supersededBy = supersededBy.toUuidOrNull(),
        createdAt = createdAt.toInstantOrNow(),
        updatedAt = updatedAt.toInstantOrNow(),
    )
}

fun BackupInteraction.toEntity(): Interaction? {
    val interactionId = id.toUuidOrNull() ?: return null
    return Interaction(
        id = interactionId,
        metAt = metAt.toInstantOrNow(),
        place = place,
        summary = summary,
        notes = notes,
        kind = kind.toEnumOrDefault(InteractionKind.OTHER),
    )
}

fun BackupAttendance.toEntity(): Attendance? {
    val interaction = interactionId.toUuidOrNull() ?: return null
    val person = personId.toUuidOrNull() ?: return null
    return Attendance(interactionId = interaction, personId = person)
}

fun BackupCommitment.toEntity(): Commitment? {
    val commitmentId = id.toUuidOrNull() ?: return null
    val owner = personId.toUuidOrNull() ?: return null
    return Commitment(
        id = commitmentId,
        personId = owner,
        direction = direction.toEnumOrDefault(Direction.I_OWE),
        body = body,
        dueOn = dueOn.toLocalDateOrNull(),
        status = status.toEnumOrDefault(CommitmentStatus.OPEN),
        createdAt = createdAt.toInstantOrNow(),
    )
}

fun BackupGroupTag.toEntity(): PersonGroupTag? {
    val owner = personId.toUuidOrNull() ?: return null
    val normalized = tag.trim().ifEmpty { return null }
    return PersonGroupTag(personId = owner, tag = normalized)
}

fun BackupAttribute.toEntity(): PersonAttribute? {
    val owner = personId.toUuidOrNull() ?: return null
    val trimmedLabel = label.trim().ifEmpty { return null }
    return PersonAttribute(
        personId = owner,
        label = trimmedLabel,
        value = value,
        sensitivity = sensitivity.toEnumOrDefault(Sensitivity.NORMAL),
        sortOrder = sortOrder,
    )
}

fun BackupTie.toEntity(): Tie? {
    val tieId = id.toUuidOrNull() ?: return null
    val from = fromPersonId.toUuidOrNull() ?: return null
    val to = toPersonId.toUuidOrNull() ?: return null
    return Tie(id = tieId, fromPersonId = from, toPersonId = to, label = label)
}

/**
 * The year a format-1 file hid inside the birthday. Newer files carry it in its own field, and
 * older ones only counted it as real when birthdayHasYear was set.
 */
private fun BackupPerson.storedBirthYear(): Int? =
    birthYear ?: birthday.toLocalDateOrNull()?.year?.takeIf { birthdayHasYear }

/** Only the month and day survive: the year has its own field now. */
private fun BackupPerson.storedBirthMonthDay(): LocalDate? =
    birthday.toLocalDateOrNull()?.withYear(BIRTHDAY_YEAR_UNKNOWN)

// Lenient primitive decoding

private fun String?.toUuidOrNull(): UUID? =
    this?.let { runCatching { UUID.fromString(it) }.getOrNull() }

private fun String?.toLocalDateOrNull(): LocalDate? =
    this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private fun String?.toInstantOrNow(): Instant =
    this?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: Instant.now()

private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(fallback: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: fallback
