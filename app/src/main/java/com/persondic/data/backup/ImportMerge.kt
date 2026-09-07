package com.persondic.data.backup

import com.persondic.data.local.entity.Attendance
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.PersonAttribute
import com.persondic.data.local.entity.PersonGroupTag
import com.persondic.data.local.entity.Tie
import java.util.UUID

/** The child rows that are safe to write, plus how many had to be left out. */
data class ImportMergePlan(
    val facts: List<Fact> = emptyList(),
    val commitments: List<Commitment> = emptyList(),
    val groupTags: List<PersonGroupTag> = emptyList(),
    val ties: List<Tie> = emptyList(),
    val attributes: List<PersonAttribute> = emptyList(),
    val attendances: List<Attendance> = emptyList(),
    val dropped: Int = 0,
)

/**
 * Decides which child rows a restore can write.
 *
 * A row pointing at a person or interaction that exists neither in the backup nor in the database
 * would be rejected by the foreign keys, and because the whole restore is one transaction that
 * single row would take everything else down with it. Dropping those rows and reporting the count
 * keeps the rest of the backup usable.
 *
 * [knownPeople] and [knownInteractions] are read back after the parent rows are written, so they
 * already include both what the backup brought and what was in the database beforehand.
 */
fun planImportMerge(
    snapshot: BackupSnapshot,
    knownPeople: Set<UUID>,
    knownInteractions: Set<UUID>,
): ImportMergePlan {
    var dropped = 0

    fun <T> List<T>.keepingKnown(predicate: (T) -> Boolean): List<T> =
        filter(predicate).also { kept -> dropped += size - kept.size }

    val facts = snapshot.facts.keepingKnown { it.personId in knownPeople }
    val commitments = snapshot.commitments.keepingKnown { it.personId in knownPeople }
    val groupTags = snapshot.groupTags.keepingKnown { it.personId in knownPeople }
    val ties = snapshot.ties.keepingKnown { it.fromPersonId in knownPeople && it.toPersonId in knownPeople }
    val attributes = snapshot.attributes.keepingKnown { it.personId in knownPeople }
    val attendances = snapshot.attendances.keepingKnown {
        it.personId in knownPeople && it.interactionId in knownInteractions
    }

    return ImportMergePlan(
        facts = facts,
        commitments = commitments,
        groupTags = groupTags,
        ties = ties,
        attributes = attributes,
        attendances = attendances,
        dropped = dropped,
    )
}
