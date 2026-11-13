package moe.tnxg.koma.feature.home.week

import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.TimeSlot
import moe.tnxg.koma.domain.week.WeekCourseGroup
import moe.tnxg.koma.domain.week.WeekDay
import kotlin.test.*

class NowLineTest {
    private val slots = mapOf(
        1 to TimeSlot(1, 480, 525), 2 to TimeSlot(2, 535, 580), 3 to TimeSlot(3, 600, 645),
    )

    @Test fun multiPeriodCourseKeepsLineThroughItsBreakAndHidesAfterwards() {
        val course = Course("a", "数学", weekday = 1, startPeriod = 1, endPeriod = 2, weeks = listOf(1))
        val day = WeekDay(LocalDate.parse("2026-09-07"), listOf(WeekCourseGroup(1, 2, listOf(course))))
        assertEquals(1, activeGroupFor(day, slots, 480)?.startPeriod)
        assertEquals(1, activeGroupFor(day, slots, 525)?.startPeriod)
        assertEquals(1, activeGroupFor(day, slots, 579)?.startPeriod)
        assertNull(activeGroupFor(day, slots, 580))
    }

    @Test fun lineHidesInTheGapBetweenSeparateCourses() {
        val first = Course("a", "数学", weekday = 1, startPeriod = 1, endPeriod = 1, weeks = listOf(1))
        val next = Course("b", "英语", weekday = 1, startPeriod = 3, endPeriod = 3, weeks = listOf(1))
        val day = WeekDay(LocalDate.parse("2026-09-07"), listOf(
            WeekCourseGroup(1, 1, listOf(first)), WeekCourseGroup(3, 3, listOf(next)),
        ))
        assertNull(activeGroupFor(day, slots, 530))
        assertEquals(3, activeGroupFor(day, slots, 610)?.startPeriod)
        assertNull(activeGroupFor(day, slots, 645))
    }

    @Test fun daysWithoutCoursesNeverShowLine() {
        val empty = WeekDay(LocalDate.parse("2026-09-07"), emptyList())
        assertNull(activeGroupFor(empty, slots, 500))
    }
}
