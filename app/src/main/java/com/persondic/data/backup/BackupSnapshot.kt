package com.persondic.data.backup

import com.persondic.data.local.entity.Attendance
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonGroupTag
import com.persondic.data.local.entity.Tie

/** Every row of the database, in entity form. What export writes and what import replays. */
data class BackupSnapshot(
    val people: List<Person> = emptyList(),
    val facts: List<Fact> = emptyList(),
    val interactions: List<Interaction> = emptyList(),
    val attendances: List<Attendance> = emptyList(),
    val commitments: List<Commitment> = emptyList(),
    val groupTags: List<PersonGroupTag> = emptyList(),
    val ties: List<Tie> = emptyList(),
) {
    val isEmpty: Boolean
        get() = people.isEmpty() && facts.isEmpty() && interactions.isEmpty() &&
            attendances.isEmpty() && commitments.isEmpty() && groupTags.isEmpty() && ties.isEmpty()
}
