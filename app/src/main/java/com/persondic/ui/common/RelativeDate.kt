package com.persondic.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun relativeDateLabel(
    instant: Instant,
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    val date = instant.atZone(zoneId).toLocalDate()
    val today = now.atZone(zoneId).toLocalDate()
    val days = ChronoUnit.DAYS.between(date, today)
    return when {
        days <= 0 -> "오늘"
        days < 7 -> "${days}일 전"
        days < 30 -> "${days / 7}주 전"
        days < 365 -> "${days / 30}개월 전"
        else -> "${days / 365}년 전"
    }
}
