package moe.tnxg.koma.app.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import moe.tnxg.koma.data.education.zhengfang.DEFAULT_TIME_GRID
import moe.tnxg.koma.domain.course.CourseSyncState
import moe.tnxg.koma.domain.course.rescheduleCourse
import moe.tnxg.koma.domain.course.resolveCourseConflict
import moe.tnxg.koma.domain.course.setCourseSuspended
import moe.tnxg.koma.domain.course.deleteCourse
import moe.tnxg.koma.domain.course.synchronizeCourses
import moe.tnxg.koma.domain.course.updateCourseNote
import moe.tnxg.koma.domain.course.updateCourseNotes
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.platform.PlatformServices

class TimetableSession(private val platform: PlatformServices) {
    private val json = Json { ignoreUnknownKeys = true }
    var snapshot by mutableStateOf(platform.readSetting("koma.snapshot")?.let { raw ->
        runCatching { json.decodeFromString<TimetableSnapshot>(raw) }.getOrNull()
    })
        private set
    private var savedStates = platform.readSetting("koma.courseSync")?.let { raw ->
        runCatching { json.decodeFromString<List<CourseSyncState>>(raw) }.getOrNull()
    }.orEmpty()
    private var state = snapshot?.let { current ->
        savedStates.firstOrNull { it.snapshot.schoolId == current.schoolId && it.snapshot.term == current.term }
            ?: CourseSyncState(current.copy(courses = current.courses.filter { it.rescheduledFromCourseId == null }
                .map { it.copy(note = "", sessionNotes = emptyMap(), suspendedWeeks = emptyList()) }), current)
    }
    var conflicts by mutableStateOf(state?.conflicts.orEmpty())
        private set
    var startDate by mutableStateOf(platform.readSetting("koma.startDate")?.takeIf {
        runCatching { LocalDate.parse(it) }.isSuccess
    }.orEmpty())
        private set

    init {
        state?.let { snapshot = it.snapshot }
    }

    val configured: Boolean get() = snapshot != null && startDate.isNotEmpty()

    fun accept(next: TimetableSnapshot) {
        val previous = snapshot
        val sameSchool = previous?.schoolId == next.schoolId
        val sameTerm = sameSchool && previous.term == next.term
        val target = state?.takeIf { it.snapshot.schoolId == next.schoolId && it.snapshot.term == next.term }
            ?: savedStates.firstOrNull { it.snapshot.schoolId == next.schoolId && it.snapshot.term == next.term }
        val grid = next.timeGrid.ifEmpty {
            target?.snapshot?.timeGrid.orEmpty().ifEmpty {
                if (sameSchool) previous.timeGrid.orEmpty().ifEmpty { DEFAULT_TIME_GRID } else DEFAULT_TIME_GRID
            }
        }
        save(synchronizeCourses(target, next.copy(timeGrid = grid)))
        if (!sameTerm) {
            platform.writeSetting("koma.startDate", null)
            startDate = ""
        }
    }

    fun updateNote(courseId: String, note: String) {
        save(updateCourseNote(requireState(), courseId, note))
    }

    /** Shared note plus the current week's session note, persisted in one write. */
    fun updateNotes(courseId: String, note: String, week: Int, sessionNote: String) {
        save(updateCourseNotes(requireState(), courseId, note, week, sessionNote))
    }

    fun setSuspended(courseId: String, week: Int, suspended: Boolean, allWeeks: Boolean = false) {
        save(setCourseSuspended(requireState(), courseId, week, suspended, allWeeks))
    }

    /** Throws IllegalArgumentException for invalid input or overlapping effective courses. */
    fun reschedule(
        courseId: String, fromWeek: Int, toWeek: Int, weekday: Int,
        startPeriod: Int, endPeriod: Int, location: String?,
    ): Course {
        val next = rescheduleCourse(requireState(), courseId, fromWeek, toWeek, weekday, startPeriod, endPeriod, location)
        val original = next.snapshot.courses.first { it.id == courseId }
        val moved = if (original.rescheduledFromCourseId != null) original else next.snapshot.courses.first {
            it.rescheduledFromCourseId == courseId && it.rescheduledFromWeek == fromWeek
        }
        save(next)
        return moved
    }

    /** [week] null removes the whole course; otherwise only that week's occurrence. */
    fun delete(courseId: String, week: Int? = null) {
        save(deleteCourse(requireState(), courseId, week))
    }

    fun resolveConflict(id: String, useRemote: Boolean) {
        save(resolveCourseConflict(requireState(), id, useRemote))
    }

    private fun requireState(): CourseSyncState = requireNotNull(state) { "请先导入课表" }

    private fun save(next: CourseSyncState) {
        val updated = savedStates.filterNot {
            it.snapshot.schoolId == next.snapshot.schoolId && it.snapshot.term == next.snapshot.term
        } + next
        platform.writeSetting("koma.courseSync", json.encodeToString(updated))
        platform.writeSetting("koma.snapshot", json.encodeToString(next.snapshot))
        savedStates = updated
        state = next
        snapshot = next.snapshot
        conflicts = next.conflicts
    }

    fun confirm(iso: String) {
        LocalDate.parse(iso)
        platform.writeSetting("koma.startDate", iso)
        startDate = iso
    }

    fun clear() {
        platform.writeSetting("koma.snapshot", null)
        platform.writeSetting("koma.startDate", null)
        platform.writeSetting("koma.courseSync", null)
        savedStates = emptyList()
        state = null
        conflicts = emptyList()
        snapshot = null
        startDate = ""
    }
}
