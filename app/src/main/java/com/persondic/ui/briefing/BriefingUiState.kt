package com.persondic.ui.briefing

import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonAttribute
import com.persondic.data.model.Direction
import com.persondic.ui.common.FactCategoryGroup

data class CommitmentDirectionGroup(
    val direction: Direction,
    val commitments: List<Commitment>,
)

data class BriefingUiState(
    val personName: String = "",
    /** Fixed information sits above the seven sections: it is identity, not news. */
    val person: Person? = null,
    val attributes: List<PersonAttribute> = emptyList(),
    val cautionFacts: List<Fact> = emptyList(),
    val openCommitmentGroups: List<CommitmentDirectionGroup> = emptyList(),
    val hookFacts: List<Fact> = emptyList(),
    val recentInteractions: List<Interaction> = emptyList(),
    val knowledgeGroups: List<FactCategoryGroup> = emptyList(),
    val sensitiveFacts: List<Fact> = emptyList(),
    val staleFacts: List<Fact> = emptyList(),
) {
    /**
     * Every one of the seven sections is empty, so the screen has nothing to brief on.
     *
     * Sections hide themselves when empty, which is right, but for a person who has just been
     * added that leaves a page with a button on it and nothing else — no way to tell an app that
     * has nothing to say from one that has broken. Fixed information does not count here: a
     * birthday is who someone is, not news about them.
     */
    val hasNothingToShow: Boolean
        get() = cautionFacts.isEmpty() &&
            openCommitmentGroups.isEmpty() &&
            hookFacts.isEmpty() &&
            recentInteractions.isEmpty() &&
            knowledgeGroups.isEmpty() &&
            sensitiveFacts.isEmpty() &&
            staleFacts.isEmpty()
}
