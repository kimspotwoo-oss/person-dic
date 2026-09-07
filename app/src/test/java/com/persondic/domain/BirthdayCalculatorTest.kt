package com.persondic.domain

import com.persondic.data.local.entity.BIRTHDAY_YEAR_UNKNOWN
import com.persondic.data.local.entity.Person
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.MonthDay

class BirthdayCalculatorTest {

    private fun person(
        birthday: LocalDate?,
        hasYear: Boolean = true,
        isLunar: Boolean = false,
    ) = Person(
        displayName = "김민준",
        birthday = birthday,
        birthdayHasYear = hasYear,
        birthdayIsLunar = isLunar,
    )

    private val today = LocalDate.parse("2026-09-07")

    @Test
    fun countsDaysToABirthdayLaterThisYear() {
        val days = BirthdayCalculator.daysUntilBirthday(person(LocalDate.parse("1990-09-21")), today)

        assertEquals(14L, days)
    }

    @Test
    fun aBirthdayAlreadyPassedThisYearRollsToNextYear() {
        val days = BirthdayCalculator.daysUntilBirthday(person(LocalDate.parse("1990-09-01")), today)

        assertEquals(359L, days)
    }

    @Test
    fun todayIsZeroDaysAway() {
        val days = BirthdayCalculator.daysUntilBirthday(person(LocalDate.parse("1990-09-07")), today)

        assertEquals(0L, days)
    }

    @Test
    fun aBirthdayWithoutAYearStillCountsDown() {
        val stored = LocalDate.of(BIRTHDAY_YEAR_UNKNOWN, 9, 21)

        val days = BirthdayCalculator.daysUntilBirthday(person(stored, hasYear = false), today)

        assertEquals(14L, days)
    }

    @Test
    fun aLunarBirthdayIsNotCountedDownRatherThanCountedWrongly() {
        val days = BirthdayCalculator.daysUntilBirthday(
            person(LocalDate.parse("1990-09-21"), isLunar = true),
            today,
        )

        assertNull(days)
    }

    @Test
    fun noBirthdayMeansNoCountdown() {
        assertNull(BirthdayCalculator.daysUntilBirthday(person(null), today))
    }

    @Test
    fun februaryTwentyNinthLandsOnTheTwentyEighthInACommonYear() {
        val next = BirthdayCalculator.nextOccurrence(MonthDay.of(2, 29), LocalDate.parse("2026-01-01"))

        assertEquals(LocalDate.parse("2026-02-28"), next)
    }

    @Test
    fun februaryTwentyNinthKeepsItsDateInALeapYear() {
        val next = BirthdayCalculator.nextOccurrence(MonthDay.of(2, 29), LocalDate.parse("2028-01-01"))

        assertEquals(LocalDate.parse("2028-02-29"), next)
    }

    @Test
    fun aLeapDayBirthdayNeverThrows() {
        val days = BirthdayCalculator.daysUntilBirthday(person(LocalDate.parse("2000-02-29")), today)

        assertEquals(174L, days)
    }

    @Test
    fun reportsTheAgeTheyWillTurn() {
        val age = BirthdayCalculator.ageOnNextBirthday(person(LocalDate.parse("1990-09-21")), today)

        assertEquals(36, age)
    }

    @Test
    fun ageAfterTheBirthdayHasPassedCountsTheNextOne() {
        val age = BirthdayCalculator.ageOnNextBirthday(person(LocalDate.parse("1990-09-01")), today)

        assertEquals(37, age)
    }

    @Test
    fun noAgeWhenTheYearIsUnknown() {
        val stored = LocalDate.of(BIRTHDAY_YEAR_UNKNOWN, 9, 21)

        assertNull(BirthdayCalculator.ageOnNextBirthday(person(stored, hasYear = false), today))
    }

    @Test
    fun noAgeForALunarBirthday() {
        assertNull(
            BirthdayCalculator.ageOnNextBirthday(person(LocalDate.parse("1990-09-21"), isLunar = true), today),
        )
    }
}
