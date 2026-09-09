package com.persondic.domain

import com.persondic.data.local.entity.Tie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class TieDescriptionTest {

    private val minjun = UUID.randomUUID()
    private val jiho = UUID.randomUUID()
    private val stranger = UUID.randomUUID()

    private val names = mapOf(minjun to "김민준", jiho to "박지호")
    private val nameOf: (UUID) -> String? = { names[it] }

    private fun tie(label: String, symmetric: Boolean = false) =
        Tie(fromPersonId = minjun, toPersonId = jiho, label = label, symmetric = symmetric)

    private fun reverse(label: String, symmetric: Boolean = false) =
        Tie(fromPersonId = jiho, toPersonId = minjun, label = label, symmetric = symmetric)

    @Test
    fun readsForwardOnTheSideItWasRecordedFrom() {
        val view = describeTie(tie("소개해준 사람"), minjun, nameOf)

        assertEquals("소개해준 사람: 박지호", requireNotNull(view).text)
        assertEquals(jiho, view.otherPersonId)
    }

    @Test
    fun readsBackwardOnTheOtherSideSoItStaysTrue() {
        val view = describeTie(tie("소개해준 사람"), jiho, nameOf)

        assertEquals("김민준의 소개해준 사람", requireNotNull(view).text)
        assertEquals(minjun, view.otherPersonId)
    }

    @Test
    fun aSymmetricTieReadsTheSameFromBothEnds() {
        // 배우자 means the same thing either way round, so turning the sentence round on the far
        // side ("김민준의 배우자") made one fact look like two.
        val spouse = tie("배우자", symmetric = true)

        assertEquals("배우자: 박지호", requireNotNull(describeTie(spouse, minjun, nameOf)).text)
        assertEquals("배우자: 김민준", requireNotNull(describeTie(spouse, jiho, nameOf)).text)
    }

    @Test
    fun anAsymmetricTieStillTurnsRoundOnTheFarSide() {
        val introduced = tie("소개해준 사람", symmetric = false)

        assertEquals("소개해준 사람: 박지호", requireNotNull(describeTie(introduced, minjun, nameOf)).text)
        assertEquals("김민준의 소개해준 사람", requireNotNull(describeTie(introduced, jiho, nameOf)).text)
    }

    // --- one relation, one row ---

    @Test
    fun thesameFriendshipEnteredFromBothSidesIsOneRow() {
        // 김경모's screen showed "이원호의 친구" and "친구: 이원호" — one friendship, printed twice.
        val views = describeTies(
            listOf(tie("친구", symmetric = true), reverse("친구", symmetric = true)),
            minjun,
            nameOf,
        )

        assertEquals(1, views.size)
        assertEquals("친구: 박지호", views[0].text)
    }

    @Test
    fun theHiddenDuplicateGoesWhenTheRowIsDeleted() {
        val mine = tie("친구", symmetric = true)
        val theirs = reverse("친구", symmetric = true)

        val row = describeTies(listOf(mine, theirs), minjun, nameOf).single()

        assertEquals(setOf(mine.id, theirs.id), row.allTieIds.toSet())
    }

    @Test
    fun aDirectedTieIsNotTheSameFactInReverse() {
        // 내가 박지호를 소개해준 것과 박지호가 나를 소개해준 것은 서로 다른 사실이다.
        val views = describeTies(
            listOf(tie("소개해준 사람"), reverse("소개해준 사람")),
            minjun,
            nameOf,
        )

        assertEquals(2, views.size)
    }

    @Test
    fun differentLabelsBetweenTheSameTwoPeopleBothStay() {
        val views = describeTies(
            listOf(tie("친구", symmetric = true), tie("직장 동료", symmetric = true)),
            minjun,
            nameOf,
        )

        assertEquals(2, views.size)
    }

    @Test
    fun whichRowSurvivesDoesNotDependOnTheOrderTheyArrivedIn() {
        val a = tie("친구", symmetric = true)
        val b = reverse("친구", symmetric = true)

        val oneWay = describeTies(listOf(a, b), minjun, nameOf).single()
        val theOther = describeTies(listOf(b, a), minjun, nameOf).single()

        assertEquals(oneWay.tieId, theOther.tieId)
        assertEquals(oneWay.text, theOther.text)
    }

    @Test
    fun tiesTouchingNobodyOnScreenAreLeftOut() {
        assertEquals(emptyList<TieView>(), describeTies(listOf(tie("친구", symmetric = true)), stranger, nameOf))
    }

    @Test
    fun aTieThatDoesNotTouchTheViewerIsNotShown() {
        assertNull(describeTie(tie("배우자"), stranger, nameOf))
    }

    @Test
    fun aTieToSomeoneNoLongerThereIsNotShown() {
        assertNull(describeTie(tie("배우자"), minjun) { null })
    }

    @Test
    fun bothEndsPointAtTheOtherPersonNotTheViewer() {
        val sibling = tie("형제자매", symmetric = true)
        val forward = requireNotNull(describeTie(sibling, minjun, nameOf))
        val backward = requireNotNull(describeTie(sibling, jiho, nameOf))

        assertEquals(jiho, forward.otherPersonId)
        assertEquals(minjun, backward.otherPersonId)
        assertEquals(forward.tieId, backward.tieId)
        assertEquals("형제자매: 박지호", forward.text)
        assertEquals("형제자매: 김민준", backward.text)
    }
}
