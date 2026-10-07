package com.persondic.domain

import com.persondic.data.local.entity.Commitment
import com.persondic.data.model.CommitmentStatus
import com.persondic.data.model.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class CommitmentSectionsTest {

    private val personId = UUID.randomUUID()
    private val today = LocalDate.parse("2026-09-07")

    private fun commitment(
        body: String,
        status: CommitmentStatus = CommitmentStatus.OPEN,
        dueOn: LocalDate? = null,
        createdDaysAgo: Long = 0,
        direction: Direction = Direction.I_OWE,
    ) = Commitment(
        personId = personId,
        direction = direction,
        body = body,
        dueOn = dueOn,
        status = status,
        createdAt = Instant.parse("2026-09-07T12:00:00Z").minusSeconds(createdDaysAgo * 86_400),
    )

    @Test
    fun doneAndDroppedBothCountAsFinished() {
        val sections = splitCommitments(
            listOf(
                commitment("열림"),
                commitment("완료", CommitmentStatus.DONE),
                commitment("취소", CommitmentStatus.DROPPED),
            ),
        )

        assertEquals(listOf("열림"), sections.open.map { it.body })
        assertEquals(setOf("완료", "취소"), sections.finished.map { it.body }.toSet())
    }

    @Test
    fun anOpenOneDueSoonComesBeforeOneAddedMoreRecently() {
        val sections = splitCommitments(
            listOf(
                commitment("나중 마감, 방금 추가", dueOn = today.plusDays(20), createdDaysAgo = 0),
                commitment("곧 마감, 오래전 추가", dueOn = today.plusDays(2), createdDaysAgo = 30),
                commitment("이미 지남", dueOn = today.minusDays(3), createdDaysAgo = 10),
            ),
        )

        assertEquals(
            listOf("이미 지남", "곧 마감, 오래전 추가", "나중 마감, 방금 추가"),
            sections.open.map { it.body },
        )
    }

    @Test
    fun anOpenOneWithNoDueDateGoesAfterEveryDatedOne() {
        val sections = splitCommitments(
            listOf(
                commitment("마감 없음", createdDaysAgo = 0),
                commitment("먼 마감", dueOn = today.plusDays(300), createdDaysAgo = 40),
            ),
        )

        assertEquals(listOf("먼 마감", "마감 없음"), sections.open.map { it.body })
    }

    @Test
    fun undatedOpenOnesAreNewestFirst() {
        val sections = splitCommitments(
            listOf(
                commitment("오래됨", createdDaysAgo = 9),
                commitment("최근", createdDaysAgo = 1),
            ),
        )

        assertEquals(listOf("최근", "오래됨"), sections.open.map { it.body })
    }

    @Test
    fun finishedOnesAreNewestFirstWhateverTheirDueDate() {
        val sections = splitCommitments(
            listOf(
                commitment("오래된 완료", CommitmentStatus.DONE, dueOn = today.plusDays(1), createdDaysAgo = 20),
                commitment("최근 취소", CommitmentStatus.DROPPED, dueOn = today.minusDays(5), createdDaysAgo = 2),
            ),
        )

        assertEquals(listOf("최근 취소", "오래된 완료"), sections.finished.map { it.body })
    }

    @Test
    fun directionDoesNotAffectWhereAnythingGoes() {
        val sections = splitCommitments(
            listOf(
                commitment("상대", direction = Direction.THEY_OWE, dueOn = today.plusDays(1)),
                commitment("나", direction = Direction.I_OWE, dueOn = today.plusDays(2)),
                commitment("서로", direction = Direction.MUTUAL, dueOn = today.plusDays(3)),
            ),
        )

        assertEquals(listOf("상대", "나", "서로"), sections.open.map { it.body })
    }

    @Test
    fun nothingGivesTwoEmptySections() {
        val sections = splitCommitments(emptyList())

        assertTrue(sections.open.isEmpty())
        assertTrue(sections.finished.isEmpty())
    }
}
