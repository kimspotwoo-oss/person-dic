package com.persondic.ui.personlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonGroupTag
import com.persondic.data.repository.PersonDicRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

private data class ListInputs(
    val people: List<Person>,
    val assignments: List<PersonGroupTag>,
    val query: String,
    val groupByTag: Boolean,
)

class PersonListViewModel(
    private val repository: PersonDicRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val isGroupedByTag = MutableStateFlow(true)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PersonListUiState> = combine(
        repository.observePeople(),
        repository.observeAllGroupTagAssignments(),
        searchQuery,
        isGroupedByTag,
    ) { people, assignments, query, grouped -> ListInputs(people, assignments, query, grouped) }
        .mapLatest { inputs ->
            val tagsByPerson: Map<UUID, List<String>> = inputs.assignments
                .groupBy { it.personId }
                .mapValues { (_, rows) -> rows.map { it.tag }.sorted() }

            val items = filterPeople(inputs.people, tagsByPerson, inputs.query).map { person ->
                PersonListItem(
                    person = person,
                    tags = tagsByPerson[person.id].orEmpty(),
                    daysSinceLastInteraction = repository.daysSinceLastInteraction(person.id),
                )
            }
            PersonListUiState(
                isGroupedByTag = inputs.groupByTag,
                searchQuery = inputs.query,
                groups = buildGroups(items, inputs.groupByTag),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonListUiState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onToggleGroupByTag() {
        isGroupedByTag.value = !isGroupedByTag.value
    }

    private suspend fun filterPeople(
        people: List<Person>,
        tagsByPerson: Map<UUID, List<String>>,
        query: String,
    ): List<Person> {
        val needle = query.trim()
        if (needle.isEmpty()) return people
        val factMatchIds = repository.findPersonIdsByFactBody(needle).toSet()
        return people.filter { person ->
            person.displayName.contains(needle, ignoreCase = true) ||
                person.alias?.contains(needle, ignoreCase = true) == true ||
                tagsByPerson[person.id].orEmpty().any { it.contains(needle, ignoreCase = true) } ||
                person.id in factMatchIds
        }
    }

    private fun buildGroups(items: List<PersonListItem>, groupByTag: Boolean): List<PersonGroup> {
        if (!groupByTag) return listOf(PersonGroup(label = null, people = items))

        val byTag = sortedMapOf<String, MutableList<PersonListItem>>()
        val untagged = mutableListOf<PersonListItem>()
        items.forEach { item ->
            if (item.tags.isEmpty()) {
                untagged += item
            } else {
                item.tags.forEach { tag -> byTag.getOrPut(tag) { mutableListOf() } += item }
            }
        }

        val tagged = byTag.map { (tag, members) -> PersonGroup(label = tag, people = members) }
        return if (untagged.isEmpty()) tagged else tagged + PersonGroup(UNTAGGED_LABEL, untagged)
    }

    private companion object {
        const val UNTAGGED_LABEL = "미분류"
    }
}
