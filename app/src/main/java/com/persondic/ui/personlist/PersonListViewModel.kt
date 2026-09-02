package com.persondic.ui.personlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.persondic.data.local.entity.Person
import com.persondic.data.repository.PersonDicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PersonListViewModel(
    private val repository: PersonDicRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val isGroupedByTag = MutableStateFlow(true)

    val uiState: StateFlow<PersonListUiState> = combine(
        repository.observePeople(),
        searchQuery,
        isGroupedByTag,
    ) { people, query, grouped -> Triple(people, query, grouped) }
        .mapLatest { (people, query, grouped) ->
            val items = filterPeople(people, query).map { person ->
                PersonListItem(
                    person = person,
                    daysSinceLastInteraction = repository.daysSinceLastInteraction(person.id),
                )
            }
            PersonListUiState(
                isGroupedByTag = grouped,
                searchQuery = query,
                groups = buildGroups(items, grouped),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonListUiState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onToggleGroupByTag() {
        isGroupedByTag.value = !isGroupedByTag.value
    }

    fun addPerson(displayName: String, alias: String?, groupTag: String?) {
        val name = displayName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            repository.addPerson(
                Person(
                    displayName = name,
                    alias = alias?.trim()?.takeIf { it.isNotEmpty() },
                    groupTag = groupTag?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
        }
    }

    private fun filterPeople(people: List<Person>, query: String): List<Person> {
        val needle = query.trim()
        if (needle.isEmpty()) return people
        return people.filter { person ->
            person.displayName.contains(needle, ignoreCase = true) ||
                person.alias?.contains(needle, ignoreCase = true) == true ||
                person.groupTag?.contains(needle, ignoreCase = true) == true
        }
    }

    private fun buildGroups(people: List<PersonListItem>, groupByTag: Boolean): List<PersonGroup> {
        if (!groupByTag) return listOf(PersonGroup(label = null, people = people))
        return people
            .groupBy { it.person.groupTag ?: UNTAGGED_LABEL }
            .toSortedMap()
            .map { (tag, members) -> PersonGroup(label = tag, people = members) }
    }

    private companion object {
        const val UNTAGGED_LABEL = "미분류"
    }
}
