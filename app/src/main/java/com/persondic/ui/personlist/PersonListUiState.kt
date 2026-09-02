package com.persondic.ui.personlist

import com.persondic.data.local.entity.Person

data class PersonListItem(
    val person: Person,
    val daysSinceLastInteraction: Long?,
)

data class PersonGroup(
    val label: String?,
    val people: List<PersonListItem>,
)

data class PersonListUiState(
    val isGroupedByTag: Boolean = true,
    val searchQuery: String = "",
    val groups: List<PersonGroup> = emptyList(),
) {
    val isEmpty: Boolean get() = groups.all { it.people.isEmpty() }
}
