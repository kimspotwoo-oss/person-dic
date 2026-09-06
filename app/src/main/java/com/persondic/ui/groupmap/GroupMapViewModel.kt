package com.persondic.ui.groupmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonGroupTag
import com.persondic.data.repository.PersonDicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class GroupMapViewModel(
    repository: PersonDicRepository,
) : ViewModel() {

    val uiState: StateFlow<GroupMapUiState> = combine(
        repository.observePeople(),
        repository.observeAllGroupTagAssignments(),
    ) { people, assignments ->
        buildUiState(people, assignments)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupMapUiState())

    private fun buildUiState(
        people: List<Person>,
        assignments: List<PersonGroupTag>,
    ): GroupMapUiState {
        val bubbles = assignments
            .groupBy { it.tag }
            .map { (tag, rows) -> GroupBubble(tag = tag, memberIds = rows.map { it.personId }.toSet()) }
            .sortedByDescending { it.memberIds.size }

        return GroupMapUiState(
            bubbles = bubbles,
            peopleById = people.associateBy { it.id },
        )
    }
}
