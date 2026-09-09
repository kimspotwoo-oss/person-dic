package com.persondic.ui.personlist

import com.persondic.data.local.entity.Person
import com.persondic.domain.Reminder

data class PersonListItem(
    val person: Person,
    val tags: List<String>,
    /** The few tags worth printing in a list row — see [com.persondic.domain.distinguishingTags]. */
    val distinguishingTags: List<String>,
    val daysSinceLastInteraction: Long?,
)

data class PersonGroup(
    val label: String?,
    val people: List<PersonListItem>,
)

/** How the list is ordered. Not a ranking of people — the reader picks it. */
enum class PersonSort {
    NAME,
    LEAST_RECENT,
}

data class PersonListUiState(
    val isGroupedByTag: Boolean = true,
    val sort: PersonSort = PersonSort.NAME,
    val searchQuery: String = "",
    val groups: List<PersonGroup> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
) {
    val isEmpty: Boolean get() = groups.all { it.people.isEmpty() }
}
