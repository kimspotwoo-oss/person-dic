package com.persondic.ui.personlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonGroupTag
import com.persondic.data.repository.PersonDicRepository
import com.persondic.domain.buildReminders
import com.persondic.domain.distinguishingTags
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
    val openCommitments: List<Commitment>,
    val query: String,
    val options: ViewOptions,
)

/** Grouping and ordering travel together so the combine stays inside its typed overloads. */
private data class ViewOptions(
    val groupByTag: Boolean = true,
    val sort: PersonSort = PersonSort.NAME,
)

class PersonListViewModel(
    private val repository: PersonDicRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val viewOptions = MutableStateFlow(ViewOptions())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PersonListUiState> = combine(
        repository.observePeople(),
        repository.observeAllGroupTagAssignments(),
        repository.observeAllOpenCommitments(),
        searchQuery,
        viewOptions,
    ) { people, assignments, commitments, query, options ->
        ListInputs(people, assignments, commitments, query, options)
    }
        .mapLatest { inputs ->
            val tagsByPerson: Map<UUID, List<String>> = inputs.assignments
                .groupBy { it.personId }
                .mapValues { (_, rows) -> rows.map { it.tag }.sorted() }

            // Over everyone, not over the search results: what a row says about a person should
            // not change because someone else stopped matching the query.
            val pickedTags = distinguishingTags(tagsByPerson)

            val items = filterPeople(inputs.people, tagsByPerson, inputs.query).map { person ->
                PersonListItem(
                    person = person,
                    tags = tagsByPerson[person.id].orEmpty(),
                    distinguishingTags = pickedTags[person.id].orEmpty(),
                    daysSinceLastInteraction = repository.daysSinceLastInteraction(person.id),
                )
            }
            PersonListUiState(
                isGroupedByTag = inputs.options.groupByTag,
                sort = inputs.options.sort,
                searchQuery = inputs.query,
                groups = buildGroups(sortItems(items, inputs.options.sort), inputs.options.groupByTag),
                reminders = buildReminders(inputs.people, inputs.openCommitments),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonListUiState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onToggleGroupByTag() {
        viewOptions.value = viewOptions.value.let { it.copy(groupByTag = !it.groupByTag) }
    }

    fun onToggleSort() {
        viewOptions.value = viewOptions.value.let {
            it.copy(sort = if (it.sort == PersonSort.NAME) PersonSort.LEAST_RECENT else PersonSort.NAME)
        }
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

    /**
     * Someone with no logged meeting sorts last rather than first. Never having recorded a meeting
     * usually means the person was only just added, not that they have been out of touch longest.
     */
    private fun sortItems(items: List<PersonListItem>, sort: PersonSort): List<PersonListItem> = when (sort) {
        PersonSort.NAME -> items
        PersonSort.LEAST_RECENT -> items.sortedWith(
            compareByDescending<PersonListItem> { it.daysSinceLastInteraction ?: Long.MIN_VALUE }
                .thenBy { it.person.displayName },
        )
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
