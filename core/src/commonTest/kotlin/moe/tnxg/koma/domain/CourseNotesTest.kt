package moe.tnxg.koma.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import moe.tnxg.koma.domain.course.CourseSyncState
import moe.tnxg.koma.domain.course.resolveCourseConflict
import moe.tnxg.koma.domain.course.synchronizeCourses
import moe.tnxg.koma.domain.course.updateCourseNotes
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.Semester
import moe.tnxg.koma.domain.model.Term
import moe.tnxg.koma.domain.model.TimetableSnapshot

private fun course() = Course("math", "高等数学", "张哲", "513", weekday = 1,
    startPeriod = 3, endPeriod = 4, weeks = listOf(5, 6, 7))

private fun snapshot(list: List<Course> = listOf(course())) = TimetableSnapshot(
    "xjvut", Term("2026", "3"), Semester("秋季", "2026-08-31", 20), emptyList(), list,
    importedAt = "2026-11-15T00:00:00Z",
)

private fun state() = CourseSyncState(snapshot(), snapshot())

class CourseNotesTest {
    @Test fun sessionNoteIsScopedToItsWeekWhileTheWholeNoteStaysShared() {
        val edited = updateCourseNotes(state(), "math", "期末占比 40%", 6, "交作业")
        val math = edited.snapshot.courses.single()
        assertEquals("期末占比 40%", math.note)
        assertEquals("交作业", math.sessionNotes[6])
        assertEquals(null, math.sessionNotes[5])
    }

    @Test fun clearingOneWeekKeepsTheOtherLessonsNote() {
        val both = updateCourseNotes(state(), "math", "", 5, "带计算器")
            .let { updateCourseNotes(it, "math", "", 6, "交作业") }
        val cleared = updateCourseNotes(both, "math", "", 6, "")
        val math = cleared.snapshot.courses.single()
        assertEquals("带计算器", math.sessionNotes[5])
        assertTrue(6 !in math.sessionNotes)
        assertEquals("", math.note)
    }

    @Test fun unchangedReimportKeepsBothNoteLayersWithoutConflicts() {
        val edited = updateCourseNotes(state(), "math", "课程简介", 5, "本节课备注")
        val synced = synchronizeCourses(edited, snapshot())
        val math = synced.snapshot.courses.single()
        assertEquals("课程简介", math.note)
        assertEquals("本节课备注", math.sessionNotes[5])
        assertTrue(synced.conflicts.isEmpty())
    }

    @Test fun adoptingTheSchoolScheduleStillKeepsLocalNotes() {
        val remote = snapshot(listOf(course().copy(location = "综合教学楼429")))
        val edited = updateCourseNotes(state(), "math", "课程简介", 5, "本节课备注")
        val conflict = synchronizeCourses(edited, remote).conflicts.single()
        val resolved = resolveCourseConflict(synchronizeCourses(edited, remote), conflict.id, useRemote = true)
        val math = resolved.snapshot.courses.single()
        assertEquals("综合教学楼429", math.location)
        assertEquals("课程简介", math.note)
        assertEquals("本节课备注", math.sessionNotes[5])
    }
}
