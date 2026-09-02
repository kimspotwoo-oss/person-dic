package com.persondic.ui.interactionlog

import java.time.LocalDateTime

data class InteractionLogUiState(
    val dateTime: LocalDateTime,
    val place: String = "",
    val summary: String = "",
    val newFactBody: String = "",
    val newFactDrafts: List<String> = emptyList(),
)
