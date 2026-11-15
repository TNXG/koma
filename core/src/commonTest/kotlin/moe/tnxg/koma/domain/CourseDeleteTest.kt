package moe.tnxg.koma.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import moe.tnxg.koma.domain.course.CourseSyncState
import moe.tnxg.koma.domain.course.deleteCourse
import moe.tnxg.koma.domain.course.rescheduleCourse
import moe.tnxg.koma.domain.course.synchronizeCourses
import moe.tnxg.koma.domain.course.updateCourseNotes
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.Semester
import moe.tnxg.koma.domain.model.Term
import moe.tnxg.koma.domain.model.TimetableSnapshot

private fun course() = Course("math", "高等数学", "张哲", "513", weekday = 1,
    startPeriod = 3, endPeriod = 4, weeks = listOf(5, 6, 7))

private fun state() = TimetableSnapshot("xjvut", Term("2026", "3"),
    Semester("秋季", "2026-08-31", 20), emptyList(), listOf(course()), importedAt = "2026-11-15T00:00:00Z")
    .let { CourseSyncState(it, it) }

class CourseDeleteTest {
    @Test fun deletingOneWeekKeepsTheRestAndOtherWeeksNotes() {
        val edited = updateCourseNotes(updateCourseNotes(state(), "math", "简介", 5, "本周作业"), "math", "简介", 6, "考试")
        val deleted = deleteCourse(edited, "math", 6)
        val math = deleted.snapshot.courses.single()
        assertEquals(listOf(5, 7), math.weeks)
        assertEquals("简介", math.note)
        assertEquals("本周作业", math.sessionNotes[5])
        assertTrue(6 !in math.sessionNotes)
    }

    @Test fun deletingTheWholeCourseAlsoRemovesItsMovedOccurrence() {
        val moved = rescheduleCourse(state(), "math", 5, 9, weekday = 1, startPeriod = 3, endPeriod = 4, location = null)
        assertEquals(2, moved.snapshot.courses.size)
        val deleted = deleteCourse(moved, "math")
        assertTrue(deleted.snapshot.courses.isEmpty())
    }

    @Test fun deletingTheSourceWeekRemovesTheOccurrenceMovedFromIt() {
        val moved = rescheduleCourse(state(), "math", 5, 9, weekday = 1, startPeriod = 3, endPeriod = 4, location = null)
        val deleted = deleteCourse(moved, "math", 5)
        val math = deleted.snapshot.courses.single()
        assertEquals(listOf(6, 7), math.weeks)
        assertTrue(deleted.snapshot.courses.none { it.rescheduledFromCourseId == "math" })
    }

    @Test fun aDeletedCourseIsIgnoredByEveryLaterImport() {
        val deleted = deleteCourse(state(), "math")
        val synced = synchronizeCourses(deleted, state().baseline)
        assertTrue(synced.snapshot.courses.isEmpty())
        assertTrue(synced.conflicts.isEmpty())
        assertTrue(synchronizeCourses(synced, state().baseline).snapshot.courses.isEmpty())
    }

    @Test fun aDeletedOccurrenceIsIgnoredWhileOtherWeeksStillSync() {
        val deleted = deleteCourse(state(), "math", 6)
        val synced = synchronizeCourses(deleted, state().baseline)
        assertEquals(listOf(5, 7), synced.snapshot.courses.single().weeks)
    }

    @Test fun deletingTheLastWeekRemovesTheCourseItself() {
        val single = CourseSyncState(state().snapshot.copy(courses = listOf(course().copy(weeks = listOf(5)))),
            state().snapshot.copy(courses = listOf(course().copy(weeks = listOf(5)))))
        assertTrue(deleteCourse(single, "math", 5).snapshot.courses.isEmpty())
        assertFailsWith<IllegalArgumentException> { deleteCourse(single, "math", 6) }
    }
}
