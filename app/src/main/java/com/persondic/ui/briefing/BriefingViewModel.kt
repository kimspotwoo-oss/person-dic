package com.persondic.ui.briefing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.local.entity.Person
import com.persondic.data.model.Direction
import com.persondic.data.model.FactCategory
import com.persondic.data.model.Sensitivity
import com.persondic.data.repository.PersonDicRepository
import com.persondic.domain.DerivedValues
import com.persondic.ui.common.FactCategoryGroup
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

private val KNOWLEDGE_CATEGORIES = listOf(FactCategory.CONTEXT, FactCategory.PREFERENCE, FactCategory.LIFE)

class BriefingViewModel(
    private val repository: PersonDicRepository,
    private val personId: UUID,
) : ViewModel() {

    val uiState: StateFlow<BriefingUiState> = combine(
        repository.observePerson(personId),
        repository.observeFacts(personId),
        repository.observeOpenCommitments(personId),
        repository.observeInteractions(personId),
    ) { person, facts, openCommitments, interactions ->
        buildUiState(person, facts, openCommitments, interactions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BriefingUiState())

    fun markStillValid(fact: Fact) {
        viewModelScope.launch { repository.renewFactAsStillValid(fact) }
    }

    fun deleteFact(fact: Fact) {
        viewModelScope.launch { repository.deleteFact(fact) }
    }

    private fun buildUiState(
        person: Person?,
        facts: List<Fact>,
        openCommitments: List<Commitment>,
        interactions: List<Interaction>,
    ): BriefingUiState {
        // Sensitive facts (PRIVATE/RESTRICTED) are gated to the 민감 정보 section only - never
        // shown plainly elsewhere, even if pinned. See design principle 4 in SPEC.md 1.
        val normalFacts = facts.filter { it.sensitivity == Sensitivity.NORMAL }
        val sensitiveFacts = facts.filter { it.sensitivity != Sensitivity.NORMAL }

        val cautionFacts = normalFacts.filter { it.pinned }.take(3)

        val openCommitmentGroups = Direction.entries
            .map { direction -> CommitmentDirectionGroup(direction, openCommitments.filter { it.direction == direction }) }
            .filter { it.commitments.isNotEmpty() }

        val hookFacts = normalFacts
            .filter { it.category == FactCategory.HOOK && !DerivedValues.isStale(it) }
            .take(4)

        val recentInteractions = interactions.take(3)

        val knowledgeGroups = KNOWLEDGE_CATEGORIES
            .map { category ->
                FactCategoryGroup(
                    category = category,
                    facts = normalFacts.filter {
                        it.category == category && !it.pinned && !DerivedValues.isStale(it)
                    },
                )
            }
            .filter { it.facts.isNotEmpty() }

        val staleFacts = normalFacts.filter { DerivedValues.isStale(it) }

        return BriefingUiState(
            personName = person?.displayName.orEmpty(),
            cautionFacts = cautionFacts,
            openCommitmentGroups = openCommitmentGroups,
            hookFacts = hookFacts,
            recentInteractions = recentInteractions,
            knowledgeGroups = knowledgeGroups,
            sensitiveFacts = sensitiveFacts,
            staleFacts = staleFacts,
        )
    }
}
