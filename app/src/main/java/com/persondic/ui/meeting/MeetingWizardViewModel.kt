package com.persondic.ui.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.local.entity.Person
import com.persondic.data.repository.PersonDicRepository
import com.persondic.domain.DraftFact
import com.persondic.domain.MeetingDraft
import com.persondic.domain.MeetingStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class MeetingWizardViewModel(
    private val repository: PersonDicRepository,
) : ViewModel() {

    private val _step = MutableStateFlow(MeetingStep.WHO)
    val step: StateFlow<MeetingStep> = _step.asStateFlow()

    private val _draft = MutableStateFlow(MeetingDraft(metAt = Instant.now()))
    val draft: StateFlow<MeetingDraft> = _draft.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    /**
     * Everyone who can be picked: the people already recorded, plus the ones invented in this flow
     * — those are not in the database yet, so nothing else would show them.
     */
    val selectableStatePeople: StateFlow<List<Person>> = combine(
        repository.observePeople(),
        _draft.map { it.newPeople },
    ) { existing, invented -> existing + invented }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onNext() {
        val current = _step.value
        if (!_draft.value.canLeave(current)) return
        current.next?.let { _step.value = it }
    }

    /** True when the caller should leave the flow instead: there is no question before the first. */
    fun onBack(): Boolean {
        val previous = _step.value.previous ?: return true
        _step.value = previous
        return false
    }

    fun onToggleAttendee(personId: UUID) = _draft.update { it.toggleAttendee(personId) }

    fun onAddNewPerson(name: String) = _draft.update { it.withNewPerson(name) }

    fun onMetAtChange(dateTime: LocalDateTime) = _draft.update {
        it.copy(metAt = dateTime.atZone(ZoneId.systemDefault()).toInstant())
    }

    fun onPlaceChange(place: String) = _draft.update { it.copy(place = place) }

    fun onSummaryChange(summary: String) = _draft.update { it.copy(summary = summary) }

    fun onNotesChange(notes: String) = _draft.update { it.copy(notes = notes) }

    fun onAddFact(fact: DraftFact) = _draft.update { it.withFact(fact) }

    fun onRemoveFact(factId: UUID) = _draft.update { it.withoutFact(factId) }

    /**
     * Writes the meeting down and everything hanging off it.
     *
     * Guarded against a second tap: the flow closes on the callback, and until it does the button
     * is still on screen. Saving twice would file the same meeting twice under a different id,
     * which nothing later can tell apart from having met them twice.
     */
    fun save(onSaved: () -> Unit) {
        if (_saving.value) return
        val draft = _draft.value
        if (!draft.canLeave(MeetingStep.WHO)) return
        _saving.value = true

        viewModelScope.launch {
            val interaction = Interaction(
                metAt = draft.metAt,
                place = draft.place.trim().takeIf { it.isNotBlank() },
                summary = draft.summary.trim().takeIf { it.isNotBlank() },
                notes = draft.notes.trim().takeIf { it.isNotBlank() },
                kind = draft.kind,
            )
            val today = LocalDate.now()
            repository.recordMeeting(
                interaction = interaction,
                attendeeIds = draft.attendeeIds,
                newPeople = draft.newPeople,
                facts = draft.facts.map { fact ->
                    Fact(
                        personId = fact.personId,
                        category = fact.category,
                        body = fact.body.trim(),
                        volatility = fact.volatility,
                        sensitivity = fact.sensitivity,
                        assertedOn = today,
                        // What ties the fact back to the evening it was learned on.
                        sourceId = interaction.id,
                    )
                },
            )
            _saving.value = false
            onSaved()
        }
    }
}
