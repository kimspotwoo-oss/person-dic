package com.persondic.ui.relationmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.persondic.data.local.entity.Person
import com.persondic.data.repository.PersonDicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RelationMapUiState(
    val graph: RelationGraph = RelationGraph(),
    val allLabels: List<String> = emptyList(),
    /** null means every label is showing — the 전체 button. */
    val selectedLabel: String? = null,
    val hasSelf: Boolean = false,
)

class RelationMapViewModel(
    private val repository: PersonDicRepository,
) : ViewModel() {

    private val selectedLabel = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RelationMapUiState> = combine(
        repository.observePeopleIncludingSelf(),
        repository.observeTies(),
        repository.observeAllTieLabels(),
        selectedLabel,
    ) { people, ties, labels, label ->
        RelationMapUiState(
            graph = buildRelationGraph(people, ties, label?.let { setOf(it) }),
            allLabels = labels,
            selectedLabel = label,
            hasSelf = people.any { it.isSelf },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RelationMapUiState())

    fun onSelectLabel(label: String?) {
        selectedLabel.value = label
    }

    fun createSelf(displayName: String, onCreated: (Person) -> Unit = {}) {
        viewModelScope.launch { onCreated(repository.ensureSelf(displayName)) }
    }
}
