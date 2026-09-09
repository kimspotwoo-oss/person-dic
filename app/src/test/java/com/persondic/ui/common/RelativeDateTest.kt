package com.persondic.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeDateTest {

    @Test
    fun aGapReadsInTheLargestUnitThatFits() {
        assertEquals("오늘", relativeDaysLabel(0))
        assertEquals("1일 전", relativeDaysLabel(1))
        assertEquals("6일 전", relativeDaysLabel(6))
        assertEquals("1주 전", relativeDaysLabel(7))
        assertEquals("4주 전", relativeDaysLabel(29))
        assertEquals("1개월 전", relativeDaysLabel(30))
        assertEquals("1년 전", relativeDaysLabel(365))
    }

    @Test
    fun aFutureDateStillReadsAsToday() {
        // Clock skew, or a meeting logged for tomorrow, should not print "-3일 전".
        assertEquals("오늘", relativeDaysLabel(-3))
    }
}
