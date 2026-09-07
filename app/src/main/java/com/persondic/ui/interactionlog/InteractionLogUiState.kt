package com.persondic.ui.interactionlog

import com.persondic.data.local.entity.Fact
import java.time.LocalDateTime

data class InteractionLogUiState(
    val dateTime: LocalDateTime,
    val place: String = "",
    val summary: String = "",
    val notes: String = "",
    val newFactBody: String = "",
    val newFactDrafts: List<String> = emptyList(),
    /** Set when an existing meeting is open, so the screen edits it instead of adding another. */
    val isExisting: Boolean = false,
    /** Facts that were written down at this meeting. Empty for a new one. */
    val factsFromThisMeeting: List<Fact> = emptyList(),
)
