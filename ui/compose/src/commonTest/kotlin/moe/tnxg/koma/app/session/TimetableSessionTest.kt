package moe.tnxg.koma.app.session

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.data.education.EduService
import moe.tnxg.koma.data.education.LoginPreparation
import moe.tnxg.koma.data.education.LoginResult
import moe.tnxg.koma.domain.model.Semester
import moe.tnxg.koma.domain.model.Term
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.platform.PlatformServices
import moe.tnxg.koma.platform.SavedCredentials

class TimetableSessionTest {
    @Test
    fun reimportPreservesConfirmedDateOnlyForSameSchoolAndTerm() {
        val platform = MemorySettings()
        val session = TimetableSession(platform)
        session.accept(snapshot())
        session.confirm("2026-08-31")
        session.accept(snapshot())
        assertEquals("2026-08-31", session.startDate)
        session.accept(snapshot(school = "other"))
        assertEquals("", session.startDate)
    }

    @Test
    fun persistedSnapshotRestoresAndLogoutRemovesBothValues() {
        val platform = MemorySettings()
        val session = TimetableSession(platform)
        session.accept(snapshot())
        session.confirm("2026-08-31")
        val restored = TimetableSession(platform)
        assertEquals("2026-08-31", restored.startDate)
        assertEquals("xjvut", restored.snapshot?.schoolId)
        restored.clear()
        assertNull(platform.readSetting("koma.snapshot"))
        assertNull(platform.readSetting("koma.startDate"))
        assertNull(platform.readSetting("koma.courseSync"))
    }

    @Test
    fun editsConflictsAndBaselineSurviveSessionRecreation() {
        val platform = MemorySettings()
        val session = TimetableSession(platform)
        val initial = snapshot().copy(courses = listOf(course()))
        session.accept(initial)
        session.updateNote("a", "带课本")
        session.setSuspended("a", 1, true)
        session.accept(initial)
        assertTrue(session.conflicts.isEmpty())
        session.accept(initial.copy(courses = listOf(course().copy(location = "教务新教室"))))
        val restored = TimetableSession(platform)
        assertEquals("带课本", restored.snapshot!!.courses.single().note)
        assertEquals(listOf(1), restored.snapshot!!.courses.single().suspendedWeeks)
        assertEquals(1, restored.conflicts.size)
        restored.resolveConflict(restored.conflicts.single().id, false)
        val again = TimetableSession(platform)
        again.accept(initial.copy(courses = listOf(course().copy(location = "教务新教室"))))
        assertTrue(again.conflicts.isEmpty())
        assertEquals("带课本", again.snapshot!!.courses.single().note)
    }

    @Test
    fun schoolAndTermSwitchesKeepIndependentManualData() {
        val session = TimetableSession(MemorySettings())
        val initial = snapshot().copy(courses = listOf(course()))
        session.accept(initial)
        session.updateNote("a", "本校旧学期")
        session.accept(initial.copy(term = Term("2027", "1")))
        assertEquals("", session.snapshot!!.courses.single().note)
        session.updateNote("a", "本校新学期")
        session.accept(initial.copy(schoolId = "other"))
        session.updateNote("a", "另一学校")
        session.accept(initial)
        assertEquals("本校旧学期", session.snapshot!!.courses.single().note)
        session.accept(initial.copy(term = Term("2027", "1")))
        assertEquals("本校新学期", session.snapshot!!.courses.single().note)
        session.accept(initial.copy(schoolId = "other"))
        assertEquals("另一学校", session.snapshot!!.courses.single().note)
    }

    @Test
    fun rescheduleReturnsSavedCourseAndInvalidRequestDoesNotPersist() {
        val platform = MemorySettings()
        val session = TimetableSession(platform)
        session.accept(snapshot().copy(courses = listOf(course())))
        val moved = session.reschedule("a", 1, 3, 4, 3, 4, "新教室")
        assertEquals(listOf(3), moved.weeks)
        assertEquals("a", moved.rescheduledFromCourseId)
        assertEquals(2, TimetableSession(platform).snapshot!!.courses.size)
        assertFailsWith<IllegalArgumentException> { session.reschedule("a", 2, 0, 4, 3, 4, null) }
        assertEquals(2, TimetableSession(platform).snapshot!!.courses.size)
        session.accept(snapshot().copy(courses = emptyList()))
        session.resolveConflict(session.conflicts.single().id, true)
        assertTrue(TimetableSession(platform).snapshot!!.courses.isEmpty())
    }

    @Test
    fun snapshotWithoutBaselineMigratesOfflineAndProtectsExistingManualFields() {
        val platform = MemorySettings()
        val imported = snapshot().copy(courses = listOf(course()))
        val legacy = imported.copy(courses = listOf(course().copy(note = "离线备注", suspendedWeeks = listOf(1))))
        platform.writeSetting("koma.snapshot", Json.encodeToString(legacy))
        platform.writeSetting("koma.startDate", "2026-08-31")
        val session = TimetableSession(platform)
        assertTrue(session.configured)
        assertEquals("离线备注", session.snapshot!!.courses.single().note)
        session.accept(imported)
        assertTrue(session.conflicts.isEmpty())
        assertEquals("离线备注", session.snapshot!!.courses.single().note)
        assertEquals(listOf(1), session.snapshot!!.courses.single().suspendedWeeks)
        assertEquals("离线备注", TimetableSession(platform).snapshot!!.courses.single().note)
    }

    private fun course() = Course("a", "数学", location = "旧教室", weekday = 1,
        startPeriod = 1, endPeriod = 2, weeks = listOf(1, 2))

    private fun snapshot(school: String = "xjvut") = TimetableSnapshot(
        school, Term("2026", "3"), Semester("2026 学年", "2026-09-07", 20),
        emptyList(), emptyList(), importedAt = "2026-11-13T00:00:00Z",
    )
}

private class MemorySettings : PlatformServices {
    private val values = mutableMapOf<String, String>()
    override val edu: EduService get() = error("No education calls in persistence test")
    override val canRememberCredentials = false
    override val notifications = null
    override fun readSetting(key: String) = values[key]
    override fun writeSetting(key: String, value: String?) { if (value == null) values.remove(key) else values[key] = value }
    override suspend fun loadCredentials(): SavedCredentials? = null
    override suspend fun saveCredentials(value: SavedCredentials?) = error("Not used")
    override suspend fun recognizeCaptcha(dataUrl: String, view: Int): String? = null
}
