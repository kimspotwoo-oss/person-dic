package com.persondic.data.backup

import com.persondic.data.local.entity.Attendance
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.PersonGroupTag
import com.persondic.data.local.entity.Tie
import com.persondic.data.model.Direction
import com.persondic.data.model.FactCategory
import com.persondic.data.model.Volatility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class ImportMergeTest {

    private val known = UUID.randomUUID()
    private val alsoKnown = UUID.randomUUID()
    private val missing = UUID.randomUUID()
    private val knownInteraction = UUID.randomUUID()
    private val missingInteraction = UUID.randomUUID()

    private fun fact(personId: UUID) = Fact(
        personId = personId,
        category = FactCategory.LIFE,
        body = "사실",
        volatility = Volatility.SEASONAL,
        assertedOn = LocalDate.parse("2026-01-01"),
    )

    private fun plan(snapshot: BackupSnapshot) = planImportMerge(
        snapshot = snapshot,
        knownPeople = setOf(known, alsoKnown),
        knownInteractions = setOf(knownInteraction),
    )

    @Test
    fun keepsRowsWhoseOwnerExists() {
        val result = plan(BackupSnapshot(facts = listOf(fact(known), fact(alsoKnown))))

        assertEquals(2, result.facts.size)
        assertEquals(0, result.dropped)
    }

    @Test
    fun dropsFactsPointingAtAPersonThatIsNotThere() {
        val kept = fact(known)
        val result = plan(BackupSnapshot(facts = listOf(kept, fact(missing))))

        assertEquals(listOf(kept), result.facts)
        assertEquals(1, result.dropped)
    }

    @Test
    fun dropsCommitmentsAndTagsForMissingPeople() {
        val result = plan(
            BackupSnapshot(
                commitments = listOf(
                    Commitment(personId = known, direction = Direction.I_OWE, body = "약속"),
                    Commitment(personId = missing, direction = Direction.I_OWE, body = "고아 약속"),
                ),
                groupTags = listOf(
                    PersonGroupTag(personId = known, tag = "회사"),
                    PersonGroupTag(personId = missing, tag = "고아 태그"),
                ),
            ),
        )

        assertEquals(1, result.commitments.size)
        assertEquals(1, result.groupTags.size)
        assertEquals(2, result.dropped)
    }

    @Test
    fun aTieNeedsBothEndsToExist() {
        val result = plan(
            BackupSnapshot(
                ties = listOf(
                    Tie(fromPersonId = known, toPersonId = alsoKnown, label = "동료"),
                    Tie(fromPersonId = known, toPersonId = missing, label = "한쪽만"),
                    Tie(fromPersonId = missing, toPersonId = known, label = "반대쪽만"),
                ),
            ),
        )

        assertEquals(1, result.ties.size)
        assertEquals("동료", result.ties.single().label)
        assertEquals(2, result.dropped)
    }

    @Test
    fun attendanceNeedsBothThePersonAndTheInteraction() {
        val result = plan(
            BackupSnapshot(
                attendances = listOf(
                    Attendance(interactionId = knownInteraction, personId = known),
                    Attendance(interactionId = missingInteraction, personId = known),
                    Attendance(interactionId = knownInteraction, personId = missing),
                ),
            ),
        )

        assertEquals(1, result.attendances.size)
        assertEquals(2, result.dropped)
    }

    @Test
    fun anEmptySnapshotPlansNothing() {
        val result = plan(BackupSnapshot())

        assertEquals(0, result.dropped)
        assertTrue(result.facts.isEmpty())
        assertTrue(result.attendances.isEmpty())
    }

    @Test
    fun countsEveryDroppedRowAcrossTables() {
        val result = plan(
            BackupSnapshot(
                facts = listOf(fact(missing)),
                commitments = listOf(Commitment(personId = missing, direction = Direction.I_OWE, body = "x")),
                groupTags = listOf(PersonGroupTag(personId = missing, tag = "x")),
                ties = listOf(Tie(fromPersonId = missing, toPersonId = missing, label = "x")),
                attendances = listOf(Attendance(interactionId = missingInteraction, personId = missing)),
            ),
        )

        assertEquals(5, result.dropped)
    }
}
