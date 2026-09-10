package com.persondic.domain

import com.persondic.data.local.entity.Person
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MeetingDraftTest {

    private val minjun = UUID.randomUUID()
    private val hayoung = UUID.randomUUID()

    private fun fact(personId: UUID, body: String = "테니스를 시작했다") =
        DraftFact(personId = personId, body = body)

    // --- moving between questions ---

    @Test
    fun theFlowWillNotLeaveWhoWithNobodyChosen() {
        assertFalse(MeetingDraft().canLeave(MeetingStep.WHO))
        assertTrue(MeetingDraft().withAttendee(minjun).canLeave(MeetingStep.WHO))
    }

    @Test
    fun everyOtherQuestionCanBeSkipped() {
        // A blank place is a true answer to "where", and a meeting nobody writes a summary for is
        // still a meeting. Only the people cannot be worked out afterwards.
        val draft = MeetingDraft().withAttendee(minjun)

        assertTrue(draft.canLeave(MeetingStep.WHEN_WHERE))
        assertTrue(draft.canLeave(MeetingStep.SUMMARY))
        assertTrue(draft.canLeave(MeetingStep.FACTS))
    }

    @Test
    fun theStepsRunInOrderAndStop() {
        assertEquals(MeetingStep.WHEN_WHERE, MeetingStep.WHO.next)
        assertEquals(MeetingStep.FACTS, MeetingStep.SUMMARY.next)
        assertNull(MeetingStep.FACTS.next)
        assertNull(MeetingStep.WHO.previous)
    }

    // --- who was there ---

    @Test
    fun severalPeopleCanBeAtOneMeeting() {
        val draft = MeetingDraft().withAttendee(minjun).withAttendee(hayoung)

        assertEquals(listOf(minjun, hayoung), draft.attendeeIds)
    }

    @Test
    fun tappingTheSamePersonTwiceTakesThemBackOut() {
        val draft = MeetingDraft().toggleAttendee(minjun).toggleAttendee(minjun)

        assertTrue(draft.attendeeIds.isEmpty())
    }

    @Test
    fun addingSomebodyAlreadyThereChangesNothing() {
        val draft = MeetingDraft().withAttendee(minjun).withAttendee(minjun)

        assertEquals(listOf(minjun), draft.attendeeIds)
    }

    @Test
    fun somebodyInventedHereIsSelectedByTheSameAct() {
        val draft = MeetingDraft().withNewPerson("윤수진")

        assertEquals(1, draft.newPeople.size)
        assertEquals("윤수진", draft.newPeople.single().displayName)
        assertEquals(listOf(draft.newPeople.single().id), draft.attendeeIds)
    }

    @Test
    fun aNameOfNothingIsNotAPerson() {
        assertEquals(MeetingDraft(), MeetingDraft().withNewPerson("   "))
    }

    // --- facts hang off the people ---

    @Test
    fun aFactBelongsToOneOfThePeoplePresent() {
        val draft = MeetingDraft().withAttendee(minjun).withFact(fact(minjun))

        assertEquals(1, draft.factsFor(minjun).size)
    }

    @Test
    fun aFactAboutSomebodyNotAtTheMeetingIsRefused() {
        val draft = MeetingDraft().withAttendee(minjun).withFact(fact(hayoung))

        assertTrue(draft.facts.isEmpty())
    }

    @Test
    fun anEmptyFactIsNotAFact() {
        val draft = MeetingDraft().withAttendee(minjun).withFact(fact(minjun, body = "  "))

        assertTrue(draft.facts.isEmpty())
    }

    @Test
    fun takingSomebodyOutTakesTheirFactsWithThem() {
        // Otherwise the meeting saves a fact about a person it no longer says was there.
        val draft = MeetingDraft()
            .withAttendee(minjun)
            .withAttendee(hayoung)
            .withFact(fact(minjun))
            .withFact(fact(hayoung))
            .withoutAttendee(minjun)

        assertEquals(listOf(hayoung), draft.attendeeIds)
        assertTrue(draft.factsFor(minjun).isEmpty())
        assertEquals(1, draft.factsFor(hayoung).size)
    }

    @Test
    fun takingOutSomebodyInventedHereForgetsThemEntirely() {
        val draft = MeetingDraft().withNewPerson("윤수진")
        val invented: Person = draft.newPeople.single()

        val after = draft.withoutAttendee(invented.id)

        assertTrue(after.newPeople.isEmpty())
        assertTrue(after.attendeeIds.isEmpty())
    }

    @Test
    fun aFactCanBeTakenBackOut() {
        val draft = MeetingDraft().withAttendee(minjun).withFact(fact(minjun))
        val written = draft.facts.single()

        assertTrue(draft.withoutFact(written.id).facts.isEmpty())
    }

    @Test
    fun factsAreKeptApartByPerson() {
        val draft = MeetingDraft()
            .withAttendee(minjun)
            .withAttendee(hayoung)
            .withFact(fact(minjun, "테니스를 시작했다"))
            .withFact(fact(hayoung, "이사 준비 중"))

        assertEquals("테니스를 시작했다", draft.factsFor(minjun).single().body)
        assertEquals("이사 준비 중", draft.factsFor(hayoung).single().body)
    }
}
