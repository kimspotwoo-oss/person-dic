package com.persondic.domain

import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import java.time.ZoneId
import java.util.UUID

/**
 * One month of meetings, newest month first and newest meeting first inside it.
 *
 * [factCount] is how many facts name that meeting as where they came from. A list of meetings on
 * its own answers "when", which the date already said; what it could not say is whether anything
 * came of the meeting. Reading it off the facts already loaded for the person costs no query.
 */
data class TimelineEntry(
    val interaction: Interaction,
    val factCount: Int,
)

data class TimelineMonth(
    val year: Int,
    val month: Int,
    val entries: List<TimelineEntry>,
)

/**
 * Groups meetings by the month they happened in.
 *
 * A flat list is fine at seven meetings and unreadable at seventy, and the thing a person scans a
 * meeting history by is when — so the months become the structure rather than a date printed at
 * the end of every row.
 *
 * Grouping is by the reader's own calendar, not UTC: a meeting late on the last evening of a month
 * belongs to that month as they lived it.
 */
fun buildTimeline(
    interactions: List<Interaction>,
    facts: List<Fact> = emptyList(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<TimelineMonth> {
    if (interactions.isEmpty()) return emptyList()

    val factsBySource: Map<UUID, Int> = facts
        .mapNotNull { it.sourceId }
        .groupingBy { it }
        .eachCount()

    return interactions
        .sortedByDescending { it.metAt }
        .groupBy { interaction ->
            val date = interaction.metAt.atZone(zoneId).toLocalDate()
            date.year to date.monthValue
        }
        .map { (yearMonth, sameMonth) ->
            TimelineMonth(
                year = yearMonth.first,
                month = yearMonth.second,
                entries = sameMonth.map { TimelineEntry(it, factsBySource[it.id] ?: 0) },
            )
        }
}
