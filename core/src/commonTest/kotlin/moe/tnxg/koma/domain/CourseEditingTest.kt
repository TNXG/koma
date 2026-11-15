package moe.tnxg.koma.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import moe.tnxg.koma.domain.course.*
import moe.tnxg.koma.domain.model.*
import moe.tnxg.koma.domain.timeline.DayBlock
import moe.tnxg.koma.domain.timeline.buildTimeline
import moe.tnxg.koma.domain.week.buildWeekSchedule
import kotlin.test.*

class CourseEditingTest {
    @Test fun legacyJsonDefaultsKeepExistingSchedule() {
        val course = Json.decodeFromString<Course>("""{"id":"a","name":"数学","weekday":1,"startPeriod":1,"endPeriod":2,"weeks":[1,2]}""")
        assertEquals("", course.note)
        assertEquals(emptyList(), course.suspendedWeeks)
        assertTrue(courseHeldOnWeek(course, 1))
    }

    @Test fun suspendedCoursesRemainInSnapshotButLeaveEffectiveWeekAndTimeline() {
        val initial = synchronizeCourses(null, fixture())
        val noted = updateCourseNote(initial, "a", "带课本")
        assertEquals(1, buildTimeline(noted.snapshot, "2026-08-31", LocalDate(2026, 8, 31), 0)
            .filterIsInstance<DayBlock>().first().courses.size)
        val stopped = setCourseSuspended(noted, "a", 1, true)
        assertFalse(courseHeldOnWeek(stopped.snapshot.courses.single(), 1))
        assertEquals("带课本", stopped.snapshot.courses.single().note)
        assertEquals(listOf(1, 2), stopped.snapshot.courses.single().weeks)
        assertTrue(buildWeekSchedule(stopped.snapshot, "2026-08-31", 1).days.first().groups.isEmpty())
        assertTrue(buildTimeline(stopped.snapshot, "2026-08-31", LocalDate(2026, 8, 31), 0)
            .filterIsInstance<DayBlock>().firstOrNull()?.courses.orEmpty().isEmpty())
        assertTrue(courseHeldOnWeek(setCourseSuspended(stopped, "a", 1, false).snapshot.courses.single(), 1))
    }

    @Test fun reschedulePersistsSourceAndRejectsOverlapWithoutMutatingOriginal() {
        val state = synchronizeCourses(null, fixture())
        val moved = rescheduleCourse(state, "a", 1, 3, 4, 3, 4, "新教室")
        val original = moved.snapshot.courses.first { it.id == "a" }
        val replacement = moved.snapshot.courses.single { it.id != "a" }
        assertEquals(listOf(1), original.suspendedWeeks)
        assertEquals(listOf(3), replacement.weeks)
        assertEquals("a", replacement.rescheduledFromCourseId)
        assertEquals(1, replacement.rescheduledFromWeek)
        assertEquals(4, replacement.weekday)
        assertEquals("新教室", replacement.location)
        assertEquals(moved, Json.decodeFromString<CourseSyncState>(Json.encodeToString(moved)))
        assertFailsWith<IllegalArgumentException> { rescheduleCourse(moved, "a", 2, 3, 4, 4, 5, null) }
        assertFailsWith<IllegalArgumentException> { rescheduleCourse(state, "a", 1, 0, 1, 1, 2, null) }
        assertEquals(emptyList(), state.snapshot.courses.single().suspendedWeeks)
        val restored = setCourseSuspended(moved, "a", 1, false)
        assertEquals(listOf("a"), restored.snapshot.courses.map { it.id })
    }

    @Test fun rescheduleRejectsOwnOtherOccurrenceAndCanMoveAnExistingReplacement() {
        val state = synchronizeCourses(null, fixture())
        assertFailsWith<IllegalArgumentException> { rescheduleCourse(state, "a", 1, 2, 1, 1, 2, null) }
        val first = rescheduleCourse(state, "a", 1, 3, 2, 1, 2, null)
        val id = first.snapshot.courses.single { it.id != "a" }.id
        val second = rescheduleCourse(first, id, 3, 4, 3, 3, 4, "再调整")
        val replacement = second.snapshot.courses.single { it.id != "a" }
        assertEquals(id, replacement.id)
        assertEquals(listOf(4), replacement.weeks)
        assertEquals("a", replacement.rescheduledFromCourseId)
        assertEquals(1, replacement.rescheduledFromWeek)
        assertEquals(listOf(1), second.snapshot.courses.first { it.id == "a" }.suspendedWeeks)
    }

    @Test fun allWeeksSuspensionAndRestoreUseOriginalWeekSet() {
        val state = synchronizeCourses(null, fixture())
        val stopped = setCourseSuspended(state, "a", 1, true, allWeeks = true)
        assertEquals(listOf(1, 2), stopped.snapshot.courses.single().suspendedWeeks)
        assertEquals(emptyList(), setCourseSuspended(stopped, "a", 1, false, true).snapshot.courses.single().suspendedWeeks)
        assertFailsWith<IllegalArgumentException> { setCourseSuspended(state, "a", 3, true) }
        assertFailsWith<IllegalArgumentException> { updateCourseNote(state, "missing", "note") }
    }

    @Test fun restoringSourceRejectsAnOccupiedTimeInsteadOfCreatingSilentOverlap() {
        val original = fixture().courses.single()
        val other = original.copy(id = "b", weekday = 2, weeks = listOf(1))
        val initial = synchronizeCourses(null, fixture(listOf(original, other)))
        val stopped = setCourseSuspended(initial, "a", 1, true)
        val occupied = rescheduleCourse(stopped, "b", 1, 1, 1, 1, 2, null)
        assertFailsWith<IllegalArgumentException> { setCourseSuspended(occupied, "a", 1, false) }
        assertFalse(courseHeldOnWeek(occupied.snapshot.courses.first { it.id == "a" }, 1))
        assertFailsWith<IllegalArgumentException> { rescheduleCourse(initial, "a", 1, 3, 1, 1, Int.MAX_VALUE, null) }
    }
}

internal fun fixture(courses: List<Course> = listOf(Course("a", "数学", location = "旧教室",
    weekday = 1, startPeriod = 1, endPeriod = 2, weeks = listOf(1, 2)))) = TimetableSnapshot(
    "school", Term("2026", "3"), Semester("秋", "2026-08-31", 20),
    listOf(TimeSlot(1, 480, 525), TimeSlot(2, 535, 580), TimeSlot(3, 600, 645),
        TimeSlot(4, 655, 700), TimeSlot(5, 720, 765)), courses, importedAt = "first")
