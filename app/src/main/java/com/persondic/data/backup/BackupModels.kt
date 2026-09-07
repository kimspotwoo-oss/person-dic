package com.persondic.data.backup

import kotlinx.serialization.Serializable

/**
 * Bumped whenever the on-disk shape changes in a way older builds cannot read. Import refuses
 * anything newer than it understands rather than quietly dropping the parts it cannot map.
 */
const val BACKUP_FORMAT_VERSION = 1

const val BACKUP_JSON_NAME = "backup.json"
const val BACKUP_PHOTO_DIR = "photos"

/**
 * The backup DTOs deliberately mirror the entities with primitives only: UUIDs, dates and enums
 * all travel as strings. A backup written today has to stay readable after the entities change,
 * so the file format is kept independent of the Room classes instead of reusing them.
 */
@Serializable
data class BackupFile(
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val exportedAt: String = "",
    val people: List<BackupPerson> = emptyList(),
    val facts: List<BackupFact> = emptyList(),
    val interactions: List<BackupInteraction> = emptyList(),
    val attendances: List<BackupAttendance> = emptyList(),
    val commitments: List<BackupCommitment> = emptyList(),
    val groupTags: List<BackupGroupTag> = emptyList(),
    val ties: List<BackupTie> = emptyList(),
)

@Serializable
data class BackupPerson(
    val id: String,
    val displayName: String,
    val alias: String? = null,
    val groupTag: String? = null,
    val metOn: String? = null,
    val metStory: String? = null,
    /** File name inside the archive's photos/ folder, not a device path. */
    val photo: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class BackupFact(
    val id: String,
    val personId: String,
    val category: String,
    val body: String,
    val volatility: String,
    val assertedOn: String,
    val expiresOn: String? = null,
    val confidence: Float = 1.0f,
    val sensitivity: String = "NORMAL",
    val pinned: Boolean = false,
    val sourceId: String? = null,
    val supersededBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class BackupInteraction(
    val id: String,
    val metAt: String,
    val place: String? = null,
    val summary: String? = null,
    val kind: String,
)

@Serializable
data class BackupAttendance(
    val interactionId: String,
    val personId: String,
)

@Serializable
data class BackupCommitment(
    val id: String,
    val personId: String,
    val direction: String,
    val body: String,
    val dueOn: String? = null,
    val status: String = "OPEN",
    val createdAt: String? = null,
)

@Serializable
data class BackupGroupTag(
    val personId: String,
    val tag: String,
)

@Serializable
data class BackupTie(
    val id: String,
    val fromPersonId: String,
    val toPersonId: String,
    val label: String,
)
