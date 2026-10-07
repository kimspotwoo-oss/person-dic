package com.persondic.domain

import com.persondic.data.local.entity.BIRTHDAY_YEAR_UNKNOWN
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Person
import com.persondic.data.model.CommitmentStatus
import com.persondic.data.model.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

class RemindersTest {

    private val today = LocalDate.parse("2026-09-07")

    private val minjun = Person(id = UUID.randomUUID(), displayName = "김민준")
    private val seoyeon = Person(id = UUID.randomUUID(), displayName = "이서연")

    private fun commitment(person: Person, body: String, dueOn: LocalDate?) = Commitment(
        personId = person.id,
        direction = Direction.I_OWE,
        body = body,
        dueOn = dueOn,
        status = CommitmentStatus.OPEN,
    )

    private fun withBirthday(person: Person, month: Int, day: Int) =
        person.copy(birthday = LocalDate.of(BIRTHDAY_YEAR_UNKNOWN, month, day))

    @Test
    fun anOverdueCommitmentComesFirstAndIsMarkedOverdue() {
        val reminders = buildReminders(
            people = listOf(minjun),
            openCommitments = listOf(
                commitment(minjun, "책 돌려주기", today.minusDays(3)),
                commitment(minjun, "메일 보내기", today.plusDays(5)),
            ),
            today = today,
        )

        assertEquals(2, reminders.size)
        assertEquals("책 돌려주기", reminders[0].body)
        assertEquals(ReminderKind.COMMITMENT_OVERDUE, reminders[0].kind)
        assertEquals(-3L, reminders[0].daysFromToday)
        assertEquals(ReminderKind.COMMITMENT_DUE_SOON, reminders[1].kind)
    }

    @Test
    fun somethingDueTodayCountsAsDueSoonNotOverdue() {
        val reminders = buildReminders(listOf(minjun), listOf(commitment(minjun, "전화", today)), today)

        assertEquals(0L, reminders.single().daysFromToday)
        assertEquals(ReminderKind.COMMITMENT_DUE_SOON, reminders.single().kind)
    }

    @Test
    fun aCommitmentWithNoDueDateIsNeverAReminder() {
        val reminders = buildReminders(listOf(minjun), listOf(commitment(minjun, "언젠가", null)), today)

        assertTrue(reminders.isEmpty())
    }

    @Test
    fun anythingBeyondTheHorizonIsLeftOut() {
        val reminders = buildReminders(
            people = listOf(minjun),
            openCommitments = listOf(
                commitment(minjun, "가까움", today.plusDays(14)),
                commitment(minjun, "멀다", today.plusDays(15)),
            ),
            today = today,
        )

        assertEquals(listOf("가까움"), reminders.map { it.body })
    }

    @Test
    fun aBirthdayInsideTheHorizonShowsUp() {
        val reminders = buildReminders(listOf(withBirthday(minjun, 9, 21)), emptyList(), today)

        val birthday = reminders.single()
        assertEquals(ReminderKind.BIRTHDAY, birthday.kind)
        assertEquals(14L, birthday.daysFromToday)
        assertEquals(LocalDate.parse("2026-09-21"), birthday.date)
    }

    @Test
    fun aDistantBirthdayIsLeftOut() {
        val reminders = buildReminders(listOf(withBirthday(minjun, 12, 25)), emptyList(), today)

        assertTrue(reminders.isEmpty())
    }

    @Test
    fun aLunarBirthdayIsNotCountedDownSoItNeverAppears() {
        val lunar = withBirthday(minjun, 9, 21).copy(birthdayIsLunar = true)

        assertTrue(buildReminders(listOf(lunar), emptyList(), today).isEmpty())
    }

    @Test
    fun commitmentsAndBirthdaysAreInterleavedByDate() {
        val reminders = buildReminders(
            people = listOf(withBirthday(minjun, 9, 10), seoyeon),
            openCommitments = listOf(
                commitment(seoyeon, "지난 약속", today.minusDays(1)),
                commitment(seoyeon, "나중 약속", today.plusDays(9)),
            ),
            today = today,
        )

        assertEquals(listOf(-1L, 3L, 9L), reminders.map { it.daysFromToday })
        assertEquals(ReminderKind.BIRTHDAY, reminders[1].kind)
    }

    @Test
    fun aCommitmentWhosePersonIsGoneIsDropped() {
        val reminders = buildReminders(
            people = listOf(minjun),
            openCommitments = listOf(commitment(seoyeon, "고아 약속", today)),
            today = today,
        )

        assertTrue(reminders.isEmpty())
    }

    @Test
    fun nothingToDoGivesAnEmptyList() {
        assertTrue(buildReminders(listOf(minjun, seoyeon), emptyList(), today).isEmpty())
    }

    @Test
    fun theOrderIsStableWhenTwoThingsFallOnTheSameDay() {
        val first = buildReminders(
            listOf(minjun, seoyeon),
            listOf(commitment(seoyeon, "가", today), commitment(minjun, "나", today)),
            today,
        )
        val second = buildReminders(
            listOf(seoyeon, minjun),
            listOf(commitment(minjun, "나", today), commitment(seoyeon, "가", today)),
            today,
        )

        assertEquals(first.map { it.personName to it.body }, second.map { it.personName to it.body })
    }

    private fun dueOn(date: LocalDate, body: String, from: LocalDate) = Reminder(
        personId = minjun.id,
        personName = minjun.displayName,
        kind = if (date < from) ReminderKind.COMMITMENT_OVERDUE else ReminderKind.COMMITMENT_DUE_SOON,
        body = body,
        date = date,
        daysFromToday = date.toEpochDay() - from.toEpochDay(),
    )

    private fun windowsOf(groups: List<ReminderGroup>) =
        groups.flatMap { group -> group.reminders.map { it.body to group.window } }.toMap()

    @Test
    fun somethingPastDueGoesInTheOverdueGroup() {
        val groups = groupReminders(listOf(dueOn(today.minusDays(1), "어제", today)), today)

        assertEquals(ReminderWindow.OVERDUE, groups.single().window)
    }

    @Test
    fun fromAMondayTodayAndSundayAreThisWeekAndTheNextMondayIsLater() {
        assertEquals(DayOfWeek.MONDAY, today.dayOfWeek)

        val windows = windowsOf(
            groupReminders(
                listOf(
                    dueOn(today, "오늘", today),
                    dueOn(today.plusDays(6), "일요일", today),
                    dueOn(today.plusDays(7), "다음 월요일", today),
                ),
                today,
            ),
        )

        assertEquals(ReminderWindow.THIS_WEEK, windows["오늘"])
        assertEquals(ReminderWindow.THIS_WEEK, windows["일요일"])
        assertEquals(ReminderWindow.LATER, windows["다음 월요일"])
    }

    @Test
    fun onAThursdayThisSaturdayAndNextTuesdayAreDifferentWeeksThoughBothUnderSevenDays() {
        val thursday = today.plusDays(3)
        assertEquals(DayOfWeek.THURSDAY, thursday.dayOfWeek)

        val windows = windowsOf(
            groupReminders(
                listOf(
                    dueOn(thursday.plusDays(2), "토요일", thursday),
                    dueOn(thursday.plusDays(5), "화요일", thursday),
                ),
                thursday,
            ),
        )

        assertEquals(ReminderWindow.THIS_WEEK, windows["토요일"])
        assertEquals(ReminderWindow.LATER, windows["화요일"])
    }

    @Test
    fun onASundayOnlyTodayIsLeftInThisWeek() {
        val sunday = today.plusDays(6)
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)

        val windows = windowsOf(
            groupReminders(
                listOf(dueOn(sunday, "오늘", sunday), dueOn(sunday.plusDays(1), "내일", sunday)),
                sunday,
            ),
        )

        assertEquals(ReminderWindow.THIS_WEEK, windows["오늘"])
        assertEquals(ReminderWindow.LATER, windows["내일"])
    }

    @Test
    fun groupsComeOutOverdueFirstAndKeepTheOrderTheyWereGivenWithin() {
        val groups = groupReminders(
            listOf(
                dueOn(today.plusDays(9), "나중 1", today),
                dueOn(today.minusDays(2), "지남", today),
                dueOn(today.plusDays(2), "이번 주", today),
                dueOn(today.plusDays(8), "나중 2", today),
            ),
            today,
        )

        assertEquals(
            listOf(ReminderWindow.OVERDUE, ReminderWindow.THIS_WEEK, ReminderWindow.LATER),
            groups.map { it.window },
        )
        assertEquals(listOf("나중 1", "나중 2"), groups.last().reminders.map { it.body })
    }

    @Test
    fun aWindowWithNothingInItIsLeftOut() {
        val groups = groupReminders(listOf(dueOn(today.plusDays(10), "다음 주", today)), today)

        assertEquals(listOf(ReminderWindow.LATER), groups.map { it.window })
    }

    @Test
    fun noRemindersGiveNoGroups() {
        assertTrue(groupReminders(emptyList(), today).isEmpty())
    }

    @Test
    fun aBirthdayTodayIsThisWeekNotOverdue() {
        val reminders = buildReminders(
            people = listOf(withBirthday(minjun, 9, 7), seoyeon),
            openCommitments = listOf(commitment(seoyeon, "지난 약속", today.minusDays(1))),
            today = today,
        )

        val groups = groupReminders(reminders, today)

        assertEquals(listOf(ReminderWindow.OVERDUE, ReminderWindow.THIS_WEEK), groups.map { it.window })
        assertEquals(ReminderKind.BIRTHDAY, groups[1].reminders.single().kind)
    }
}
