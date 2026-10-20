package com.persondic.domain

import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.local.entity.Person
import com.persondic.data.model.FactCategory
import com.persondic.data.model.InteractionKind
import com.persondic.data.model.Sensitivity
import com.persondic.data.model.Volatility
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * The questions a meeting is recorded through, one to a screen.
 *
 * Everything on one page is what the app had, and it reads as a form to be filled in — which is
 * exactly the feeling that stops a meeting being written down on the walk home. One question at a
 * time is how a sign-up flow gets long forms answered, and the same trick works here.
 *
 * Only [WHO] can refuse to move on. Somewhere to put the meeting is the one thing the app cannot
 * work out for itself; the date defaults to now, and a blank place or summary is a true answer to
 * "where" and "what happened" when the person does not want to type one.
 */
enum class MeetingStep {
    WHO,
    WHEN_WHERE,
    SUMMARY,
    FACTS,
    ;

    val next: MeetingStep? get() = entries.getOrNull(ordinal + 1)
    val previous: MeetingStep? get() = entries.getOrNull(ordinal - 1)
}

/** A fact typed during the meeting, before it belongs to anybody in the database. */
data class DraftFact(
    val id: UUID = UUID.randomUUID(),
    val personId: UUID,
    val body: String,
    val category: FactCategory = FactCategory.CONTEXT,
    val volatility: Volatility = Volatility.SLOW,
    val sensitivity: Sensitivity = Sensitivity.NORMAL,
)

/**
 * A meeting being written down.
 *
 * [newPeople] are the ones added from inside the flow. They carry their real ids from the moment
 * they are typed, so the facts step can hang facts on them, but they are not written to the
 * database until the whole thing is saved — abandoning the flow half way should not leave a person
 * behind with nothing but a name.
 */
data class MeetingDraft(
    val attendeeIds: List<UUID> = emptyList(),
    val newPeople: List<Person> = emptyList(),
    val metAt: Instant = Instant.EPOCH,
    val place: String = "",
    val summary: String = "",
    val notes: String = "",
    val kind: InteractionKind = InteractionKind.MEET,
    val facts: List<DraftFact> = emptyList(),
) {
    /** Whether the flow can move past [step]. */
    fun canLeave(step: MeetingStep): Boolean = when (step) {
        MeetingStep.WHO -> attendeeIds.isNotEmpty()
        else -> true
    }

    fun withAttendee(personId: UUID): MeetingDraft =
        if (personId in attendeeIds) this else copy(attendeeIds = attendeeIds + personId)

    /**
     * Dropping someone takes their facts with them. Leaving those behind would save a fact about
     * somebody the meeting no longer says was there, which is worse than losing what was typed.
     */
    fun withoutAttendee(personId: UUID): MeetingDraft = copy(
        attendeeIds = attendeeIds - personId,
        newPeople = newPeople.filterNot { it.id == personId },
        facts = facts.filterNot { it.personId == personId },
    )

    fun toggleAttendee(personId: UUID): MeetingDraft =
        if (personId in attendeeIds) withoutAttendee(personId) else withAttendee(personId)

    /** A person invented here is selected by the same act that creates them. */
    fun withNewPerson(name: String): MeetingDraft {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return this
        val person = Person(displayName = trimmed)
        return copy(newPeople = newPeople + person, attendeeIds = attendeeIds + person.id)
    }

    fun withFact(fact: DraftFact): MeetingDraft =
        if (fact.body.isBlank() || fact.personId !in attendeeIds) this else copy(facts = facts + fact)

    fun withoutFact(factId: UUID): MeetingDraft = copy(facts = facts.filterNot { it.id == factId })

    fun factsFor(personId: UUID): List<DraftFact> = facts.filter { it.personId == personId }

    /**
     * The rows this draft becomes. Both the full flow and the one-line entry on S2 save through
     * here, so there is one idea of what a recorded meeting is, not two that drift apart.
     *
     * Null while nobody is chosen: a meeting with no attendees is a row nobody can find again.
     */
    fun toRecord(today: LocalDate): MeetingRecord? {
        if (!canLeave(MeetingStep.WHO)) return null
        val interaction = Interaction(
            metAt = metAt,
            place = place.trim().takeIf { it.isNotBlank() },
            summary = summary.trim().takeIf { it.isNotBlank() },
            notes = notes.trim().takeIf { it.isNotBlank() },
            kind = kind,
        )
        return MeetingRecord(
            interaction = interaction,
            attendeeIds = attendeeIds,
            newPeople = newPeople,
            facts = facts.map { fact ->
                Fact(
                    personId = fact.personId,
                    category = fact.category,
                    body = fact.body.trim(),
                    volatility = fact.volatility,
                    sensitivity = fact.sensitivity,
                    assertedOn = today,
                    // What ties the fact back to the evening it was learned on.
                    sourceId = interaction.id,
                )
            },
        )
    }

    companion object {
        /**
         * A meeting written down as one line from someone's own page.
         *
         * It is the same draft the full flow builds, answered where it can be: the person is the
         * page being looked at, which is the one question the flow refuses to skip; the date is
         * now, as it would default to; the line is the summary. Place, notes and facts are left
         * blank, which the flow already accepts as true answers.
         *
         * Null for a blank line — that is not a meeting anyone meant to save.
         */
        fun oneLine(personId: UUID, line: String, now: Instant): MeetingDraft? {
            val summary = line.trim()
            if (summary.isEmpty()) return null
            return MeetingDraft(attendeeIds = listOf(personId), metAt = now, summary = summary)
        }
    }
}

/** What saving a [MeetingDraft] writes, in one transaction. */
data class MeetingRecord(
    val interaction: Interaction,
    val attendeeIds: List<UUID>,
    val newPeople: List<Person>,
    val facts: List<Fact>,
)
