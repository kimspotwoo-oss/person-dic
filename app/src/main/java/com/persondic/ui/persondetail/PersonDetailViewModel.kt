package com.persondic.ui.persondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.local.entity.Person
import com.persondic.data.model.FactCategory
import com.persondic.data.repository.PersonDicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class PersonDetailViewModel(
    private val repository: PersonDicRepository,
    private val personId: UUID,
) : ViewModel() {

    val person: StateFlow<Person?> = repository.observePerson(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val factGroups: StateFlow<List<FactCategoryGroup>> = repository.observeFacts(personId)
        .map(::groupByCategory)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val interactions: StateFlow<List<Interaction>> = repository.observeInteractions(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val commitments: StateFlow<List<Commitment>> = repository.observeCommitments(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteFact(fact: Fact) {
        viewModelScope.launch { repository.deleteFact(fact) }
    }

    fun togglePinned(fact: Fact) {
        viewModelScope.launch { repository.updateFact(fact.copy(pinned = !fact.pinned)) }
    }

    private fun groupByCategory(facts: List<Fact>): List<FactCategoryGroup> =
        FactCategory.entries
            .map { category -> FactCategoryGroup(category, facts.filter { it.category == category }) }
            .filter { it.facts.isNotEmpty() }
}
