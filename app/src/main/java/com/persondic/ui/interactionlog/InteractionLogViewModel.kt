package com.persondic.ui.interactionlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.model.FactCategory
import com.persondic.data.model.InteractionKind
import com.persondic.data.model.Sensitivity
import com.persondic.data.model.Volatility
import com.persondic.data.repository.PersonDicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class InteractionLogViewModel(
    private val repository: PersonDicRepository,
    private val personId: UUID,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InteractionLogUiState(dateTime = LocalDateTime.now()))
    val uiState: StateFlow<InteractionLogUiState> = _uiState.asStateFlow()

    fun onDateTimeChange(dateTime: LocalDateTime) {
        _uiState.update { it.copy(dateTime = dateTime) }
    }

    fun onPlaceChange(place: String) {
        _uiState.update { it.copy(place = place) }
    }

    fun onSummaryChange(summary: String) {
        _uiState.update { it.copy(summary = summary) }
    }

    fun onNewFactBodyChange(body: String) {
        _uiState.update { it.copy(newFactBody = body) }
    }

    fun addNewFactDraft() {
        val body = _uiState.value.newFactBody.trim()
        if (body.isEmpty()) return
        _uiState.update { it.copy(newFactDrafts = it.newFactDrafts + body, newFactBody = "") }
    }

    fun removeNewFactDraft(index: Int) {
        _uiState.update { it.copy(newFactDrafts = it.newFactDrafts.filterIndexed { i, _ -> i != index }) }
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val factBodies = (state.newFactDrafts + state.newFactBody.trim()).filter { it.isNotBlank() }
        viewModelScope.launch {
            val interaction = Interaction(
                metAt = state.dateTime.atZone(ZoneId.systemDefault()).toInstant(),
                place = state.place.trim().takeIf { it.isNotEmpty() },
                summary = state.summary.trim().takeIf { it.isNotEmpty() },
                kind = InteractionKind.MEET,
            )
            repository.recordInteraction(interaction, personId)

            val today = LocalDate.now()
            factBodies.forEach { body ->
                repository.addFact(
                    Fact(
                        personId = personId,
                        category = FactCategory.LIFE,
                        body = body,
                        volatility = Volatility.SEASONAL,
                        assertedOn = today,
                        sensitivity = Sensitivity.NORMAL,
                    ),
                )
            }
            onSaved()
        }
    }
}
