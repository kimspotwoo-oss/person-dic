package com.persondic.domain

import com.persondic.data.local.entity.Tie
import java.util.UUID

/** One tie as it should read on one person's screen, plus who to open when it is tapped. */
data class TieView(
    val tieId: UUID,
    val otherPersonId: UUID,
    val text: String,
)

/**
 * Writes a tie as a phrase that is true from the viewer's side.
 *
 * A tie is directed and its label describes the far end, so the same row has to be read two ways.
 * "배우자" happens to be symmetric, but "소개해준 사람" is not: showing the stored label unchanged
 * on the other person's screen would state the opposite of what was recorded.
 *
 *   stored: 김민준 →[소개해준 사람]→ 박지호
 *   on 김민준's screen:  "소개해준 사람: 박지호"      (박지호가 소개해줬다)
 *   on 박지호's screen:  "김민준의 소개해준 사람"     (박지호는 김민준을 소개해준 사람이다)
 *
 * Returns null when the tie does not touch [viewerId], or when the other person is unknown.
 */
fun describeTie(tie: Tie, viewerId: UUID, nameOf: (UUID) -> String?): TieView? = when (viewerId) {
    tie.fromPersonId -> nameOf(tie.toPersonId)?.let { other ->
        TieView(tie.id, tie.toPersonId, "${tie.label}: $other")
    }

    tie.toPersonId -> nameOf(tie.fromPersonId)?.let { other ->
        TieView(tie.id, tie.fromPersonId, "${other}의 ${tie.label}")
    }

    else -> null
}

/** Offered in the editor so the same relation does not get typed three different ways. */
val SUGGESTED_TIE_LABELS = listOf(
    "친구",
    "배우자",
    "형제자매",
    "직장 동료",
    "부모",
    "자녀",
    "소개해준 사람",
)

/**
 * The suggested labels that read the same from either end, used to pre-tick the checkbox.
 * Only a starting guess — what is stored is whatever the user leaves the box on.
 */
val SYMMETRIC_TIE_LABELS = setOf("친구", "배우자", "형제자매", "직장 동료")
