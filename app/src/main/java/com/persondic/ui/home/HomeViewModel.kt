package com.persondic.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Person
import com.persondic.data.model.Direction
import com.persondic.data.repository.PersonDicRepository
import com.persondic.domain.Reminder
import com.persondic.domain.buildReminders
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class HomeViewModel(private val repository: PersonDicRepository) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> = combine(
        repository.observePeople(),
        repository.observeAllOpenCommitments(),
    ) { people, commitments -> buildReminders(people, commitments) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** For choosing who a new reminder is about. */
    val people: StateFlow<List<Person>> = repository.observePeople()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * A reminder the reader sets themselves.
     *
     * Stored as an open commitment they owe, because that is what it is — something to do about
     * somebody by a date — and it means the reminder appears above through the same path as one
     * that came out of a meeting, rather than through a second mechanism doing the same job.
     */
    fun addReminder(personId: UUID, body: String, dueOn: LocalDate) {
        val text = body.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            repository.addCommitment(
                Commitment(
                    personId = personId,
                    body = text,
                    direction = Direction.I_OWE,
                    dueOn = dueOn,
                ),
            )
        }
    }
}
