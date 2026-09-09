package com.persondic.domain

import com.persondic.data.local.entity.Tie
import java.util.UUID

/** One tie as it should read on one person's screen, plus who to open when it is tapped. */
data class TieView(
    val tieId: UUID,
    val otherPersonId: UUID,
    val text: String,
    /**
     * Other stored ties saying the same thing as this row. Deleting the row has to remove these
     * too, or the duplicate simply reappears once the one on screen is gone.
     */
    val duplicateIds: List<UUID> = emptyList(),
) {
    val allTieIds: List<UUID> get() = listOf(tieId) + duplicateIds
}

/**
 * Writes a tie as a phrase that is true from the viewer's side.
 *
 * A directed tie's label describes the far end, so the same row has to be read two ways:
 *
 *   stored: 김민준 →[소개해준 사람]→ 박지호
 *   on 김민준's screen:  "소개해준 사람: 박지호"      (박지호가 소개해줬다)
 *   on 박지호's screen:  "김민준의 소개해준 사람"     (박지호는 김민준을 소개해준 사람이다)
 *
 * A symmetric tie is the opposite case and needs the opposite treatment: 친구 means the same thing
 * from both ends, so both ends get the same sentence. Turning it round there produced "이원호의
 * 친구" on one screen and "친구: 이원호" on the other — two phrasings of one fact, which read like
 * two different facts. Whether a tie is symmetric is stored on it ([Tie.symmetric]) rather than
 * guessed from the label, because a label the user invents is in no list the app ships with.
 *
 * Returns null when the tie does not touch [viewerId], or when the other person is unknown.
 */
fun describeTie(tie: Tie, viewerId: UUID, nameOf: (UUID) -> String?): TieView? {
    val otherId = when (viewerId) {
        tie.fromPersonId -> tie.toPersonId
        tie.toPersonId -> tie.fromPersonId
        else -> return null
    }
    val other = nameOf(otherId) ?: return null
    val text = if (tie.symmetric || viewerId == tie.fromPersonId) {
        "${tie.label}: $other"
    } else {
        "${other}의 ${tie.label}"
    }
    return TieView(tie.id, otherId, text)
}

/**
 * Every tie touching [viewerId], each written once.
 *
 * Recording 친구 from both people's screens stores two rows, and both used to be listed — 김경모's
 * screen showed "이원호의 친구" above "친구: 이원호", one friendship printed twice. A symmetric tie
 * is the same relation whichever way round it was entered, so the pair is matched unordered; a
 * directed one is not, and A→[소개해준 사람]→B stays a different fact from B→[소개해준 사람]→A.
 *
 * The row keeps every id it stands for so that deleting it deletes the lot.
 */
fun describeTies(ties: List<Tie>, viewerId: UUID, nameOf: (UUID) -> String?): List<TieView> {
    val bySameThing = LinkedHashMap<String, MutableList<TieView>>()
    ties.forEach { tie ->
        val view = describeTie(tie, viewerId, nameOf) ?: return@forEach
        bySameThing.getOrPut(sameRelationKey(tie)) { mutableListOf() } += view
    }
    return bySameThing.values.map { sameThing ->
        // Lowest id wins so the surviving row does not depend on the order rows came back in.
        val kept = sameThing.minByOrNull { it.tieId.toString() } ?: sameThing.first()
        kept.copy(duplicateIds = sameThing.map { it.tieId }.filterNot { it == kept.tieId })
    }
}

/**
 * One tie per relation, for callers that want the relations rather than the rows — the graph drew
 * a friendship entered from both sides as two lines laid exactly on top of each other.
 */
fun dedupeTies(ties: List<Tie>): List<Tie> =
    ties.groupBy(::sameRelationKey).values.map { sameThing ->
        sameThing.minByOrNull { it.id.toString() } ?: sameThing.first()
    }

/**
 * What two ties have to share to be the same fact. A symmetric tie is matched on the unordered
 * pair, since which screen it was entered from carries no meaning; a directed one keeps its
 * direction, because A introduced B is not B introduced A.
 */
private fun sameRelationKey(tie: Tie): String {
    val ends = if (tie.symmetric) {
        listOf(tie.fromPersonId, tie.toPersonId).map { it.toString() }.sorted().joinToString("-")
    } else {
        "${tie.fromPersonId}>${tie.toPersonId}"
    }
    return "$ends|${tie.label}|${tie.symmetric}"
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
