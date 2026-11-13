package moe.tnxg.koma.feature.settings

import kotlinx.datetime.LocalDate
import moe.tnxg.koma.app.preferences.AccentPreference
import moe.tnxg.koma.app.preferences.HomeDestination
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.app.preferences.ScheduleDensity
import moe.tnxg.koma.app.preferences.ThemePreference
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.Semester
import moe.tnxg.koma.domain.model.Term
import moe.tnxg.koma.domain.model.TimeSlot
import moe.tnxg.koma.domain.model.TimetableSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsLogicTest {
    private val grid = listOf(
        TimeSlot(1, 480, 525),
        TimeSlot(2, 535, 580),
        TimeSlot(3, 600, 645),
        TimeSlot(4, 655, 700),
    )

    private fun sampleSnapshot(courses: List<Course>): TimetableSnapshot = TimetableSnapshot(
        schoolId = "xjvut",
        term = Term("2026", "1"),
        semester = Semester("2026 秋", "2026-08-31", 16),
        timeGrid = grid,
        courses = courses,
        studentName = "测试同学",
        importedAt = "2026-08-30",
    )

    @Test
    fun displayWeekPreviewRespectsWeekendAndOtherWeekCourseToggles() {
        val week1Course = Course("c1", "高等数学", weekday = 1, startPeriod = 1, endPeriod = 2, weeks = listOf(1))
        val week2NonClashing = Course("c2", "线性代数", weekday = 1, startPeriod = 3, endPeriod = 4, weeks = listOf(2))
        val week2Clashing = Course("c3", "冲突课程", weekday = 1, startPeriod = 1, endPeriod = 2, weeks = listOf(2))
        val snapshot = sampleSnapshot(listOf(week1Course, week2NonClashing, week2Clashing))

        val weekdaysOnly = buildDisplayWeekPreview(
            snapshot = snapshot,
            startDate = "2026-08-31",
            todayDate = LocalDate.parse("2026-08-31"),
            showWeekend = false,
            showOtherWeekCourses = false,
        )
        assertEquals(5, weekdaysOnly.visibleDays)
        assertTrue(weekdaysOnly.offWeekByWeekday.isEmpty())

        val withWeekendAndOther = buildDisplayWeekPreview(
            snapshot = snapshot,
            startDate = "2026-08-31",
            todayDate = LocalDate.parse("2026-08-31"),
            showWeekend = true,
            showOtherWeekCourses = true,
        )
        assertEquals(7, withWeekendAndOther.visibleDays)
        val mondayOffWeek = withWeekendAndOther.offWeekByWeekday[1].orEmpty()
        assertEquals(listOf("线性代数"), mondayOffWeek.flatMap { it.courses }.map { it.name })
    }

    @Test
    fun cardPreviewCoursePrefersCompleteMetadata() {
        val partial = Course("c1", "体育", weekday = 2, startPeriod = 1, endPeriod = 2, weeks = listOf(1))
        val complete = Course("c2", "大学物理", "张老师", "C302", weekday = 3, startPeriod = 3, endPeriod = 4, weeks = listOf(1))
        assertEquals("大学物理", pickCardPreviewCourse(sampleSnapshot(listOf(partial, complete)))?.name)
        assertNull(pickCardPreviewCourse(sampleSnapshot(emptyList())))
    }

    @Test
    fun failedStoreUpdatesKeepOldValuesAndExposeError() {
        val store = PreferenceStore({ null }) { _, _ -> error("write failed") }
        val before = store.value
        assertFalse(store.update(before.copy(theme = ThemePreference.Dark, showRoom = false)))
        assertEquals(before, store.value)
        assertNotNull(store.error)

        assertFalse(store.reset())
        assertEquals(before, store.value)
        assertNotNull(store.error)

        store.clearError()
        assertNull(store.error)
    }

    @Test
    fun allPreferenceEnumLabelsAreDefined() {
        assertEquals(listOf("跟随系统", "浅色", "深色"), ThemePreference.entries.map { it.label() })
        assertEquals(listOf("鸢尾蓝", "晴空蓝", "松石绿", "蔷薇粉", "琥珀金"), AccentPreference.entries.map { it.label() })
        assertEquals(listOf("紧凑", "标准", "宽松"), ScheduleDensity.entries.map { it.label() })
        assertEquals(listOf("今天", "课表", "我的"), HomeDestination.entries.map { it.label() })
        assertEquals("准时", reminderLeadLabel(0))
        assertEquals("提前 10 分钟", reminderLeadLabel(10))
    }
}
