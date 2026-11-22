package moe.tnxg.koma.domain

import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.Semester
import moe.tnxg.koma.domain.model.Term
import moe.tnxg.koma.domain.model.TimeSlot
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.domain.timeline.AgendaState
import moe.tnxg.koma.domain.timeline.ClassState
import moe.tnxg.koma.domain.timeline.todayAgenda
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TodayAgendaTest {
    private val day = LocalDate(2026, 9, 7)
    private val start = "2026-08-31"
    private val first = Course("first", "数学", weekday = 1, startPeriod = 1, endPeriod = 1, weeks = listOf(2))
    private val moved = first.copy(id = "moved", name = "编程", startPeriod = 3, endPeriod = 3,
        rescheduledFromCourseId = "original", rescheduledFromWeek = 2)
    private val snapshot = TimetableSnapshot(
        "local-json", Term("2026", "1"), Semester("秋季", start, 2),
        listOf(TimeSlot(1, 480, 525), TimeSlot(2, 540, 585), TimeSlot(3, 600, 645)),
        listOf(moved, first.copy(id = "tomorrow", weekday = 2),
            first.copy(id = "original", startPeriod = 2, endPeriod = 2, suspendedWeeks = listOf(2)), first),
        importedAt = "",
    )

    @Test fun compactWindowFollowsTheCurrentThenNextEffectiveCourse() {
        val inClass = todayAgenda(snapshot, start, day, 480)
        assertEquals(listOf("first", "moved"), inClass.courses.map { it.time.course.id })
        assertEquals(ClassState.Current, inClass.visibleCourses(1).single().state)
        assertEquals(525, inClass.nextChangeMinute)

        val between = todayAgenda(snapshot, start, day, 525)
        assertEquals("moved", between.visibleCourses(1).single().time.course.id)
        assertEquals(ClassState.Next, between.visibleCourses(1).single().state)
        assertEquals(600, between.nextChangeMinute)
        assertEquals(AgendaState.Finished, todayAgenda(snapshot, start, day, 645).state)
    }

    @Test fun missingTimesKeepTheCourseWithoutClaimingItIsInProgressOrFinished() {
        val agenda = todayAgenda(snapshot.copy(timeGrid = snapshot.timeGrid.take(1)), start, day, 660)
        val visible = agenda.visibleCourses(1).single()
        assertEquals("moved", visible.time.course.id)
        assertEquals(ClassState.Unknown, visible.state)
        assertEquals(AgendaState.Classes, agenda.state)
        assertNull(agenda.nextChangeMinute)
    }

    @Test fun aFreeDayShowsTheNearestEffectiveClassDateEvenAcrossWeeks() {
        val tuesday = LocalDate(2026, 9, 8)
        val nextWeek = first.copy(id = "next-week", weeks = listOf(3))
        val future = snapshot.copy(semester = snapshot.semester.copy(totalWeeks = 4),
            courses = snapshot.courses + nextWeek +
                first.copy(id = "suspended", weekday = 3, suspendedWeeks = listOf(2)))
        val agenda = todayAgenda(future, start, tuesday, 700)
        assertEquals(tuesday, agenda.date)
        assertEquals("tomorrow", agenda.courses.single().time.course.id)
        val preview = todayAgenda(future, start, LocalDate(2026, 9, 9), 700)
        assertEquals(LocalDate(2026, 9, 14), preview.date)
        assertEquals(3, preview.week)
        assertEquals("next-week", preview.visibleCourses(1).single().time.course.id)
        assertNull(preview.nextChangeMinute)
    }

    @Test fun datesWithNoRemainingClassDayRetainAnEmptyState() {
        assertEquals(AgendaState.NoTimetable, todayAgenda(null, null, day, 0).state)
        assertEquals(AgendaState.NeedsStart, todayAgenda(snapshot, null, day, 0).state)
        val opening = todayAgenda(snapshot, start, LocalDate(2026, 8, 30), 0)
        assertEquals(day, opening.date)
        assertEquals(2, opening.week)
        val freeDay = todayAgenda(snapshot, start, LocalDate(2026, 9, 9), 0)
        assertEquals(2, freeDay.week)
        assertEquals(AgendaState.NoClasses, freeDay.state)
        assertEquals(emptyList(), freeDay.visibleCourses(1))
        assertEquals(AgendaState.AfterTerm, todayAgenda(snapshot, start, LocalDate(2026, 9, 14), 0).state)
    }
}
