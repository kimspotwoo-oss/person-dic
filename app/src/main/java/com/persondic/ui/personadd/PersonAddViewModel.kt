package com.persondic.ui.personadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.persondic.data.local.entity.Person
import com.persondic.data.repository.PersonDicRepository
import com.persondic.ui.common.AttributeDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Everything typed before the person exists.
 *
 * Adding a person writes several tables, so nothing is saved until the whole form is: a half
 * written person left behind by a mistyped name would be worse than losing the typing.
 */
data class PersonAddUiState(
    val displayName: String = "",
    val alias: String = "",
    val metStory: String = "",
    val photoUri: String? = null,
    val tags: List<String> = emptyList(),
    val monthDay: LocalDate? = null,
    val birthYear: Int? = null,
    val isLunar: Boolean = false,
    val attributes: List<AttributeDraft> = emptyList(),
    val isSaving: Boolean = false,
) {
    val canSave: Boolean
        get() = displayName.isNotBlank() && !isSaving
}

class PersonAddViewModel(
    private val repository: PersonDicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonAddUiState())
    val uiState: StateFlow<PersonAddUiState> = _uiState.asStateFlow()

    val allTags: StateFlow<List<String>> = repository.observeAllGroupTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allAttributeLabels: StateFlow<List<String>> = repository.observeAllAttributeLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onDisplayNameChange(value: String) = _uiState.update { it.copy(displayName = value) }

    fun onAliasChange(value: String) = _uiState.update { it.copy(alias = value) }

    fun onMetStoryChange(value: String) = _uiState.update { it.copy(metStory = value) }

    fun onPhotoChange(photoUri: String?) = _uiState.update { it.copy(photoUri = photoUri) }

    fun addTag(tag: String) = _uiState.update {
        if (tag in it.tags) it else it.copy(tags = it.tags + tag)
    }

    fun removeTag(tag: String) = _uiState.update { it.copy(tags = it.tags - tag) }

    fun onBirthdayChange(monthDay: LocalDate?, birthYear: Int?, isLunar: Boolean) = _uiState.update {
        it.copy(monthDay = monthDay, birthYear = birthYear, isLunar = isLunar)
    }

    fun addAttribute(draft: AttributeDraft) = _uiState.update { state ->
        if (draft.label.isBlank() || draft.value.isBlank()) {
            state
        } else {
            // One value per label. Saving twice under the same label would upsert over itself
            // and silently keep only the last one, so the list makes the replacement visible.
            state.copy(attributes = state.attributes.filterNot { it.label == draft.label } + draft)
        }
    }

    fun removeAttribute(label: String) = _uiState.update { state ->
        state.copy(attributes = state.attributes.filterNot { it.label == label })
    }

    fun save(onSaved: (java.util.UUID) -> Unit) {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val person = Person(
                displayName = state.displayName.trim(),
                alias = state.alias.trim().takeIf { it.isNotEmpty() },
                metStory = state.metStory.trim().takeIf { it.isNotEmpty() },
                photoUri = state.photoUri,
            )
            repository.addPerson(person)
            // Goes through setBirthday so the stand-in year and the legacy mirror column are
            // applied in exactly one place.
            repository.setBirthday(person, state.monthDay, state.birthYear, state.isLunar)
            state.tags.forEach { tag -> repository.addGroupTag(person.id, tag) }
            state.attributes.forEachIndexed { index, attribute ->
                repository.setAttribute(
                    personId = person.id,
                    label = attribute.label,
                    value = attribute.value,
                    sensitivity = attribute.sensitivity,
                    sortOrder = index,
                )
            }
            onSaved(person.id)
        }
    }
}
