package com.persondic.domain

import com.persondic.data.local.entity.Person
import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit

/**
 * Birthday arithmetic for the fixed-information block.
 *
 * Lunar birthdays are stored and shown but never counted down: converting a lunar date to a solar
 * one needs a lunar calendar table this app does not carry, and a countdown that is quietly wrong
 * by weeks is worse than no countdown at all.
 */
object BirthdayCalculator {

    /** Days until the next time this birthday comes round, or null if it cannot be counted. */
    fun daysUntilBirthday(person: Person, today: LocalDate = LocalDate.now()): Long? {
        val birthday = person.birthday ?: return null
        if (person.birthdayIsLunar) return null
        return ChronoUnit.DAYS.between(today, nextOccurrence(MonthDay.from(birthday), today))
    }

    /**
     * Age on the next birthday's date, or null when the birth year is unknown or the date is
     * lunar. Reported in the international count, not 한국식 세는 나이.
     */
    fun ageOnNextBirthday(person: Person, today: LocalDate = LocalDate.now()): Int? {
        val birthday = person.birthday ?: return null
        if (!person.birthdayHasYear || person.birthdayIsLunar) return null
        val next = nextOccurrence(MonthDay.from(birthday), today)
        return next.year - birthday.year
    }

    /**
     * The next date this month-day falls on, counting today as "today".
     *
     * 2월 29일 in a common year lands on 2월 28일: MonthDay.atYear clamps to the last valid day of
     * the month, which is the same rule java.time uses everywhere else.
     */
    fun nextOccurrence(monthDay: MonthDay, today: LocalDate): LocalDate {
        val thisYear = monthDay.atYear(today.year)
        return if (thisYear < today) monthDay.atYear(today.year + 1) else thisYear
    }
}
