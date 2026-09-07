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

    private fun tie(label: String) = Tie(fromPersonId = minjun, toPersonId = jiho, label = label)

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
    fun aSymmetricLabelStillReadsCorrectlyBothWays() {
        assertEquals("배우자: 박지호", requireNotNull(describeTie(tie("배우자"), minjun, nameOf)).text)
        assertEquals("김민준의 배우자", requireNotNull(describeTie(tie("배우자"), jiho, nameOf)).text)
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
        val sibling = tie("형제자매")
        val forward = requireNotNull(describeTie(sibling, minjun, nameOf))
        val backward = requireNotNull(describeTie(sibling, jiho, nameOf))

        assertEquals(jiho, forward.otherPersonId)
        assertEquals(minjun, backward.otherPersonId)
        assertEquals(forward.tieId, backward.tieId)
    }
}
