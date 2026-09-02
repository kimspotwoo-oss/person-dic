package com.persondic.ui.briefing

import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.model.Direction
import com.persondic.ui.common.FactCategoryGroup

data class CommitmentDirectionGroup(
    val direction: Direction,
    val commitments: List<Commitment>,
)

data class BriefingUiState(
    val personName: String = "",
    val cautionFacts: List<Fact> = emptyList(),
    val openCommitmentGroups: List<CommitmentDirectionGroup> = emptyList(),
    val hookFacts: List<Fact> = emptyList(),
    val recentInteractions: List<Interaction> = emptyList(),
    val knowledgeGroups: List<FactCategoryGroup> = emptyList(),
    val sensitiveFacts: List<Fact> = emptyList(),
    val staleFacts: List<Fact> = emptyList(),
)
