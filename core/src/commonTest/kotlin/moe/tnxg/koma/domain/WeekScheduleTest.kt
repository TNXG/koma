package moe.tnxg.koma.domain

import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.model.*
import moe.tnxg.koma.domain.week.*
import kotlin.test.*

class WeekScheduleTest {
    private fun course(id: String, day: Int, start: Int, end: Int, weeks: List<Int> = listOf(1)) =
        Course(id, id, weekday = day, startPeriod = start, endPeriod = end, weeks = weeks)
    private fun snapshot(courses: List<Course>) = TimetableSnapshot(
        "school", Term("2026", "3"), Semester("秋", "2026-08-31", 20),
        listOf(TimeSlot(1, 480, 525), TimeSlot(2, 535, 580)), courses, importedAt = "",
    )

    @Test fun weekProjectionFiltersInactiveCoursesAndCrossesMonthBoundary() {
        val result = buildWeekSchedule(snapshot(listOf(
            course("inactive", 1, 1, 2, listOf(2)), course("late", 1, 3, 4),
            course("early", 1, 1, 2), course("sunday", 7, 1, 2),
        )), "2026-08-31", 1)
        assertEquals(LocalDate.parse("2026-08-31"), result.days.first().date)
        assertEquals(LocalDate.parse("2026-09-06"), result.days.last().date)
        assertEquals(listOf("early", "late"), result.days.first().groups.flatMap { it.courses }.map { it.id })
        assertEquals(listOf("sunday"), result.days.last().groups.single().courses.map { it.id })
        assertTrue(result.days[1].groups.isEmpty())
    }

    @Test fun transitiveOverlapsRemainAccessibleInOneConflictGroup() {
        val result = buildWeekSchedule(snapshot(listOf(
            course("c", 1, 4, 5), course("a", 1, 1, 2), course("b", 1, 2, 4),
            course("separate", 1, 6, 7),
        )), "2026-08-31", 1)
        val groups = result.days.first().groups
        assertEquals(listOf("a", "b", "c"), groups.first().courses.map { it.id })
        assertEquals(1, groups.first().startPeriod)
        assertEquals(5, groups.first().endPeriod)
        assertEquals(listOf("separate"), groups.last().courses.map { it.id })
    }

    @Test fun semesterBoundsAndLatePeriodsDoNotHideCourses() {
        val data = snapshot(listOf(course("evening", 7, 13, 14, listOf(20))))
        val result = buildWeekSchedule(data, "2026-08-31", 99)
        assertEquals(20, result.week)
        assertEquals(14, result.periodCount)
        assertEquals("evening", result.days.last().groups.single().courses.single().id)
        assertEquals(1, buildWeekSchedule(data, "2026-08-31", -1).week)
    }

    @Test fun clockPositionUsesRealPeriodTimesAndDoesNotLeakOutsideTheAxis() {
        val slots = listOf(TimeSlot(1, 480, 525), TimeSlot(2, 535, 580), TimeSlot(4, 700, 745))
        assertNull(weekTimePosition(slots, 479))
        assertEquals(0f, weekTimePosition(slots, 480))
        assertEquals(1f, weekTimePosition(slots, 530))
        assertEquals(1.5f, weekTimePosition(slots, 557)!!, .02f)
        assertEquals(3f, weekTimePosition(slots, 700))
        assertEquals(4f, weekTimePosition(slots, 745))
        assertNull(weekTimePosition(slots, 746))
        assertNull(weekTimePosition(emptyList(), 500))
    }
}
