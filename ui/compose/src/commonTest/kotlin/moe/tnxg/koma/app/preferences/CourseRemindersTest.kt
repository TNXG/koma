package moe.tnxg.koma.app.preferences

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import moe.tnxg.koma.domain.model.*

class CourseRemindersTest {
    private val course = Course("math", "数学", location = "A201", weekday = 1,
        startPeriod = 1, endPeriod = 2, weeks = listOf(1, 2))
    private fun snapshot(grid: List<TimeSlot>) = TimetableSnapshot("xjvut", Term("2026", "3"),
        Semester("秋季", "2026-11-09", 2), grid, listOf(course), importedAt = "2026-11-01T00:00:00Z")

    @Test fun schedulesLeadTimeInLocalTimezoneNotUtc() {
        val next = nextCourseReminder(snapshot(listOf(TimeSlot(1, 600, 650))), "2026-11-09",
            Instant.parse("2026-11-09T01:00:00Z"), 10, TimeZone.of("Asia/Shanghai"))!!
        assertEquals(Instant.parse("2026-11-09T01:50:00Z"), next.at)
        assertEquals("数学", next.course.name)
    }

    @Test fun alreadyPassedReminderAdvancesToNextTeachingWeek() {
        val next = nextCourseReminder(snapshot(listOf(TimeSlot(1, 600, 650))), "2026-11-09",
            Instant.parse("2026-11-09T02:01:00Z"), 10, TimeZone.of("Asia/Shanghai"))!!
        assertEquals(Instant.parse("2026-11-16T01:50:00Z"), next.at)
    }

    @Test fun missingPeriodTimeNeverInventsReminderClockTime() {
        assertNull(nextCourseReminder(snapshot(emptyList()), "2026-11-09",
            Instant.parse("2026-11-09T01:00:00Z"), 10, TimeZone.of("Asia/Shanghai")))
    }
}
