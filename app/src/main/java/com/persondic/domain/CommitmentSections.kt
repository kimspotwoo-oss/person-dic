package com.persondic.domain

import com.persondic.data.local.entity.Commitment
import com.persondic.data.model.CommitmentStatus

data class CommitmentSections(val open: List<Commitment>, val finished: List<Commitment>)

/** Open ones soonest-due first (undated last); finished ones, done or dropped, newest first. */
fun splitCommitments(commitments: List<Commitment>): CommitmentSections {
    val (open, finished) = commitments.partition { it.status == CommitmentStatus.OPEN }
    return CommitmentSections(
        open = open.sortedWith(
            compareBy<Commitment> { it.dueOn == null }
                .thenBy { it.dueOn }
                .thenByDescending { it.createdAt },
        ),
        finished = finished.sortedByDescending { it.createdAt },
    )
}
