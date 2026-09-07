package com.persondic.ui.persondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonAttribute
import com.persondic.data.model.CommitmentStatus
import com.persondic.data.model.Direction
import com.persondic.data.model.FactCategory
import com.persondic.data.model.Sensitivity
import com.persondic.data.repository.PersonDicRepository
import com.persondic.domain.TieView
import com.persondic.domain.describeTie
import com.persondic.ui.common.FactCategoryGroup
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
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

    /**
     * When each fact's source meeting happened, for the facts that came out of one.
     *
     * The meeting screen already shows what was learned there; this is the same link read the
     * other way, so a fact can say where it came from.
     */
    val factSourceDates: StateFlow<Map<UUID, LocalDate>> = repository.observeInteractions(personId)
        .map { interactions ->
            interactions.associate { it.id to it.metAt.atZone(ZoneId.systemDefault()).toLocalDate() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val interactions: StateFlow<List<Interaction>> = repository.observeInteractions(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val commitments: StateFlow<List<Commitment>> = repository.observeCommitments(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tags: StateFlow<List<String>> = repository.observeGroupTags(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTags: StateFlow<List<String>> = repository.observeAllGroupTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val attributes: StateFlow<List<PersonAttribute>> = repository.observeAttributes(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allAttributeLabels: StateFlow<List<String>> = repository.observeAllAttributeLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Ties phrased from this person's side, so an asymmetric label still reads correctly. */
    val ties: StateFlow<List<TieView>> = combine(
        repository.observeTies(personId),
        repository.observePeople(),
    ) { ties, people ->
        val names = people.associate { it.id to it.displayName }
        ties.mapNotNull { tie -> describeTie(tie, personId) { names[it] } }
            .sortedBy { it.text }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Everyone else, for picking the other end of a tie. */
    val otherPeople: StateFlow<List<Person>> = repository.observePeople()
        .map { people -> people.filterNot { it.id == personId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTieLabels: StateFlow<List<String>> = repository.observeAllTieLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addTag(tag: String) {
        viewModelScope.launch { repository.addGroupTag(personId, tag) }
    }

    fun removeTag(tag: String) {
        viewModelScope.launch { repository.removeGroupTag(personId, tag) }
    }

    fun setPhoto(photoUri: String?) {
        val current = person.value ?: return
        viewModelScope.launch { repository.setPersonPhoto(current, photoUri) }
    }

    fun addTie(otherPersonId: UUID, label: String) {
        viewModelScope.launch { repository.addTie(personId, otherPersonId, label) }
    }

    fun removeTie(tieId: UUID) {
        viewModelScope.launch { repository.removeTie(tieId) }
    }

    fun updateProfile(displayName: String, alias: String?, metStory: String?) {
        val current = person.value ?: return
        val name = displayName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            repository.updatePerson(current.copy(displayName = name, alias = alias, metStory = metStory))
        }
    }

    fun setBirthday(monthDay: LocalDate?, birthYear: Int?, isLunar: Boolean) {
        val current = person.value ?: return
        viewModelScope.launch { repository.setBirthday(current, monthDay, birthYear, isLunar) }
    }

    fun setAttribute(label: String, value: String, sensitivity: Sensitivity) {
        viewModelScope.launch {
            repository.setAttribute(
                personId = personId,
                label = label,
                value = value,
                sensitivity = sensitivity,
                sortOrder = attributes.value.size,
            )
        }
    }

    fun removeAttribute(label: String) {
        viewModelScope.launch { repository.removeAttribute(personId, label) }
    }

    fun deleteFact(fact: Fact) {
        viewModelScope.launch { repository.deleteFact(fact) }
    }

    fun togglePinned(fact: Fact) {
        viewModelScope.launch { repository.updateFact(fact.copy(pinned = !fact.pinned)) }
    }

    fun addCommitment(direction: Direction, body: String, dueOn: LocalDate?) {
        val trimmedBody = body.trim()
        if (trimmedBody.isEmpty()) return
        viewModelScope.launch {
            repository.addCommitment(
                Commitment(personId = personId, direction = direction, body = trimmedBody, dueOn = dueOn),
            )
        }
    }

    fun setCommitmentStatus(commitment: Commitment, status: CommitmentStatus) {
        viewModelScope.launch { repository.updateCommitment(commitment.copy(status = status)) }
    }

    fun deleteCommitment(commitment: Commitment) {
        viewModelScope.launch { repository.deleteCommitment(commitment) }
    }

    private fun groupByCategory(facts: List<Fact>): List<FactCategoryGroup> =
        FactCategory.entries
            .map { category -> FactCategoryGroup(category, facts.filter { it.category == category }) }
            .filter { it.facts.isNotEmpty() }
}
