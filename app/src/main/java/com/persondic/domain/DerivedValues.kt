package com.persondic.domain

import com.persondic.data.local.entity.Fact
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object DerivedValues {

    fun daysSinceLastInteraction(
        lastInteractionAt: Instant?,
        zoneId: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zoneId),
    ): Long? {
        if (lastInteractionAt == null) return null
        val lastDate = lastInteractionAt.atZone(zoneId).toLocalDate()
        return ChronoUnit.DAYS.between(lastDate, today)
    }

    fun isStale(fact: Fact, today: LocalDate = LocalDate.now()): Boolean {
        val expiresOn = fact.expiresOn ?: return false
        return expiresOn.isBefore(today)
    }

    fun staleFactCount(facts: List<Fact>, today: LocalDate = LocalDate.now()): Int =
        facts.count { isStale(it, today) }
}
