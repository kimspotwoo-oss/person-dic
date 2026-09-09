package com.persondic.domain

import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.model.FactCategory
import com.persondic.data.model.InteractionKind
import com.persondic.data.model.Volatility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class InteractionTimelineTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private val personId = UUID.randomUUID()

    private fun met(year: Int, month: Int, day: Int, hour: Int = 12) = Interaction(
        metAt = LocalDateTime.of(year, month, day, hour, 0).atZone(seoul).toInstant(),
        kind = InteractionKind.MEET,
    )

    private fun factFrom(source: Interaction?) = Fact(
        personId = personId,
        category = FactCategory.HOOK,
        body = "테니스를 시작했다",
        volatility = Volatility.SEASONAL,
        assertedOn = LocalDate.of(2026, 1, 1),
        sourceId = source?.id,
    )

    @Test
    fun meetingsAreGroupedByMonthNewestFirst() {
        val timeline = buildTimeline(
            listOf(met(2026, 7, 3), met(2026, 9, 1), met(2026, 9, 20)),
            zoneId = seoul,
        )

        assertEquals(listOf(2026 to 9, 2026 to 7), timeline.map { it.year to it.month })
        assertEquals(2, timeline[0].entries.size)
    }

    @Test
    fun theNewestMeetingComesFirstInsideAMonth() {
        val early = met(2026, 9, 1)
        val late = met(2026, 9, 20)

        val entries = buildTimeline(listOf(early, late), zoneId = seoul).single().entries

        assertEquals(late.id, entries[0].interaction.id)
        assertEquals(early.id, entries[1].interaction.id)
    }

    @Test
    fun eachMeetingCountsTheFactsThatCameFromIt() {
        val meeting = met(2026, 9, 8)
        val other = met(2026, 9, 1)
        val facts = listOf(factFrom(meeting), factFrom(meeting), factFrom(other), factFrom(null))

        val entries = buildTimeline(listOf(meeting, other), facts, seoul).single().entries

        assertEquals(2, entries.first { it.interaction.id == meeting.id }.factCount)
        assertEquals(1, entries.first { it.interaction.id == other.id }.factCount)
    }

    @Test
    fun aFactWithNoMeetingBehindItIsCountedNowhere() {
        val meeting = met(2026, 9, 8)

        val entries = buildTimeline(listOf(meeting), listOf(factFrom(null)), seoul).single().entries

        assertEquals(0, entries.single().factCount)
    }

    @Test
    fun aMonthBoundaryFollowsTheReadersClockNotUtc() {
        // 22:00 on 31 August in Seoul is 13:00 UTC the same day; an hour later it is September in
        // Seoul and still August in UTC. The reader lived through September.
        val lateAugust = met(2026, 8, 31, hour = 22)
        val justIntoSeptember = met(2026, 9, 1, hour = 1)

        val timeline = buildTimeline(listOf(lateAugust, justIntoSeptember), zoneId = seoul)

        assertEquals(listOf(2026 to 9, 2026 to 8), timeline.map { it.year to it.month })
    }

    @Test
    fun theYearIsPartOfTheGroupingNotJustTheMonth() {
        val timeline = buildTimeline(listOf(met(2025, 9, 5), met(2026, 9, 5)), zoneId = seoul)

        assertEquals(2, timeline.size)
        assertEquals(listOf(2026 to 9, 2025 to 9), timeline.map { it.year to it.month })
    }

    @Test
    fun nothingRecordedIsAnEmptyTimeline() {
        assertTrue(buildTimeline(emptyList(), zoneId = seoul).isEmpty())
    }
}
