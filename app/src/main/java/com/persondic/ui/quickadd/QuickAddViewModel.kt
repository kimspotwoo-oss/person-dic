package com.persondic.ui.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Person
import com.persondic.data.repository.PersonDicRepository
import com.persondic.domain.QuickAddResult
import com.persondic.domain.parseQuickAdd
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class QuickAddUiState(
    val input: String = "",
    val parsed: QuickAddResult = QuickAddResult(),
    val isSaving: Boolean = false,
) {
    val canSave: Boolean
        get() = parsed.people.isNotEmpty() && !isSaving
}

class QuickAddViewModel(
    private val repository: PersonDicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickAddUiState())
    val uiState: StateFlow<QuickAddUiState> = _uiState.asStateFlow()

    /**
     * Parsing runs on every keystroke rather than on a "preview" button: the format is only worth
     * using if a typo shows up as you type instead of after saving.
     */
    fun onInputChange(input: String) {
        _uiState.update { it.copy(input = input, parsed = parseQuickAdd(input)) }
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val today = LocalDate.now()
            state.parsed.people.forEach { parsed ->
                val person = Person(
                    displayName = parsed.displayName,
                    alias = parsed.alias,
                    metStory = parsed.metStory,
                )
                repository.addPerson(person)
                parsed.tags.forEach { tag -> repository.addGroupTag(person.id, tag) }
                parsed.facts.forEach { fact ->
                    repository.addFact(
                        Fact(
                            personId = person.id,
                            category = fact.category,
                            body = fact.body,
                            volatility = fact.volatility,
                            assertedOn = today,
                            sensitivity = fact.sensitivity,
                            pinned = fact.pinned,
                        ),
                    )
                }
                parsed.commitments.forEach { commitment ->
                    repository.addCommitment(
                        Commitment(
                            personId = person.id,
                            direction = commitment.direction,
                            body = commitment.body,
                        ),
                    )
                }
            }
            _uiState.value = QuickAddUiState()
            onSaved()
        }
    }
}
