package com.persondic.domain

import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Person
import java.time.LocalDate
import java.time.MonthDay
import java.util.UUID

enum class ReminderKind {
    /** An open commitment whose due date has passed. */
    COMMITMENT_OVERDUE,

    /** An open commitment due within the horizon. */
    COMMITMENT_DUE_SOON,

    /** A birthday coming up within the horizon. */
    BIRTHDAY,
}

/**
 * One dated reason to get in touch.
 *
 * [daysFromToday] is negative once the date has passed, so ordering by it puts the most overdue
 * item first and the furthest away last.
 */
data class Reminder(
    val personId: UUID,
    val personName: String,
    val kind: ReminderKind,
    val body: String,
    val date: LocalDate,
    val daysFromToday: Long,
)

/**
 * Collects the things with a date attached to them: commitments that are due, and birthdays that
 * are close.
 *
 * Deliberately only dated facts. Design principle 1 rules out scoring or ranking people, and
 * "who have you neglected" is a score wearing a date's clothes — a missed deadline is not. Sorting
 * people by how long it has been is offered on the list instead, where the reader asks for it.
 *
 * Commitments without a due date never appear: there is nothing to be late for.
 */
fun buildReminders(
    people: List<Person>,
    openCommitments: List<Commitment>,
    today: LocalDate = LocalDate.now(),
    horizonDays: Long = DEFAULT_HORIZON_DAYS,
): List<Reminder> {
    val byId = people.associateBy { it.id }

    val commitmentReminders = openCommitments.mapNotNull { commitment ->
        val dueOn = commitment.dueOn ?: return@mapNotNull null
        val person = byId[commitment.personId] ?: return@mapNotNull null
        val days = dueOn.toEpochDay() - today.toEpochDay()
        if (days > horizonDays) return@mapNotNull null
        Reminder(
            personId = person.id,
            personName = person.displayName,
            kind = if (days < 0) ReminderKind.COMMITMENT_OVERDUE else ReminderKind.COMMITMENT_DUE_SOON,
            body = commitment.body,
            date = dueOn,
            daysFromToday = days,
        )
    }

    val birthdayReminders = people.mapNotNull { person ->
        val days = BirthdayCalculator.daysUntilBirthday(person, today) ?: return@mapNotNull null
        if (days > horizonDays) return@mapNotNull null
        val birthday = person.birthday ?: return@mapNotNull null
        Reminder(
            personId = person.id,
            personName = person.displayName,
            kind = ReminderKind.BIRTHDAY,
            body = "",
            date = BirthdayCalculator.nextOccurrence(MonthDay.from(birthday), today),
            daysFromToday = days,
        )
    }

    return (commitmentReminders + birthdayReminders)
        .sortedWith(compareBy({ it.daysFromToday }, { it.personName }, { it.body }))
}

/** Two weeks out. Far enough to act on, close enough not to become a wall of text. */
const val DEFAULT_HORIZON_DAYS = 14L
