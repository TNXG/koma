package moe.tnxg.koma.domain.course

import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.courseHeldOnWeek

fun updateCourseNote(state: CourseSyncState, courseId: String, note: String): CourseSyncState {
    val course = state.requireCourse(courseId)
    return state.replaceCourses(state.snapshot.courses.map { if (it.id == courseId) course.copy(note = note) else it })
}

/** Writes the shared note and one week's session note in a single state transition. */
fun updateCourseNotes(
    state: CourseSyncState, courseId: String, note: String, sessionWeek: Int, sessionNote: String,
): CourseSyncState {
    val course = state.requireCourse(courseId)
    val notes = if (sessionNote.isBlank()) course.sessionNotes - sessionWeek
        else course.sessionNotes + (sessionWeek to sessionNote)
    return state.replaceCourses(state.snapshot.courses.map {
        if (it.id == courseId) course.copy(note = note, sessionNotes = notes) else it
    })
}

/** Removes the whole course, or only one week's occurrence; derived reschedules follow. */
fun deleteCourse(state: CourseSyncState, courseId: String, week: Int? = null): CourseSyncState {
    val course = state.requireCourse(courseId)
    val rootId = course.rescheduledFromCourseId ?: course.id
    val courses = if (week == null) {
        state.snapshot.courses.filterNot { it.id == rootId || it.rescheduledFromCourseId == rootId }
    } else {
        require(week in course.weeks) { "该周没有此课程" }
        val remaining = course.weeks - week
        state.snapshot.courses.mapNotNull {
            when {
                it.id == courseId && remaining.isEmpty() -> null
                it.id == courseId -> course.copy(
                    weeks = remaining,
                    suspendedWeeks = course.suspendedWeeks - week,
                    sessionNotes = course.sessionNotes - week,
                )
                it.rescheduledFromCourseId == rootId && it.rescheduledFromWeek == week -> null
                else -> it
            }
        }
    }
    val keptIds = courses.map(Course::id).toSet()
    val ignore = if (state.ignored.any { it.courseId == rootId && it.week == null }) state.ignored
        else state.ignored + CourseIgnore(rootId, week)
    return state.replaceCourses(courses).copy(
        conflicts = state.conflicts.filter { it.courseId in keptIds },
        ignored = ignore,
    )
}

fun setCourseSuspended(
    state: CourseSyncState, courseId: String, week: Int, suspended: Boolean, allWeeks: Boolean = false,
): CourseSyncState {
    val course = state.requireCourse(courseId)
    require(week in course.weeks) { "该周没有此课程" }
    val affected = if (allWeeks) course.weeks.toSet() else setOf(week)
    val weeks = if (suspended) course.suspendedWeeks.toSet() + affected else course.suspendedWeeks.toSet() - affected
    val courses = state.snapshot.courses.mapNotNull {
        when {
            it.id == courseId -> course.copy(suspendedWeeks = weeks.sorted())
            !suspended && it.rescheduledFromCourseId == courseId && it.rescheduledFromWeek in affected -> null
            else -> it
        }
    }
    if (!suspended) require(courses.none { other ->
        other.id != courseId && other.weekday == course.weekday &&
            other.startPeriod <= course.endPeriod && course.startPeriod <= other.endPeriod &&
            affected.any { it in course.suspendedWeeks && courseHeldOnWeek(other, it) }
    }) { "恢复后的时间与其他课程重叠，请先处理冲突" }
    return state.replaceCourses(courses)
}

/** Invalid input or an occupied target throws before changing the state. */
fun rescheduleCourse(
    state: CourseSyncState, courseId: String, fromWeek: Int, toWeek: Int, weekday: Int,
    startPeriod: Int, endPeriod: Int, location: String?,
): CourseSyncState {
    val course = state.requireCourse(courseId)
    require(courseHeldOnWeek(course, fromWeek)) { "只能调整当前有效的课程" }
    require(toWeek in 1..state.snapshot.semester.totalWeeks) { "目标周超出学期范围" }
    require(weekday in 1..7) { "星期必须在 1 到 7 之间" }
    require(startPeriod > 0 && endPeriod >= startPeriod) { "节次范围无效" }
    val availablePeriods = state.snapshot.timeGrid.map { it.index }.toSet()
    require(endPeriod <= (availablePeriods.maxOrNull() ?: 30) &&
        (availablePeriods.isEmpty() || (startPeriod..endPeriod).all { it in availablePeriods })) { "节次超出时间表范围" }
    val rootId = course.rescheduledFromCourseId ?: course.id
    val rootWeek = course.rescheduledFromWeek ?: fromWeek
    val id = if (course.rescheduledFromCourseId != null) course.id else "rescheduled:${course.id.length}:${course.id}:$fromWeek"
    require(id == courseId || state.snapshot.courses.none { it.id == id }) { "该次课程已调整，请先恢复或编辑调课记录" }
    val replacement = course.copy(id = id, weeks = listOf(toWeek), weekday = weekday,
        startPeriod = startPeriod, endPeriod = endPeriod, location = location,
        suspendedWeeks = emptyList(), rescheduledFromCourseId = rootId, rescheduledFromWeek = rootWeek)
    val remainingSource = course.copy(suspendedWeeks = (course.suspendedWeeks + fromWeek).distinct().sorted())
    require(state.snapshot.courses.none { other ->
        when {
            other.id != courseId -> coursesOverlap(replacement, other)
            id != courseId -> coursesOverlap(replacement, remainingSource)
            else -> false
        }
    }) { "目标时间与其他课程重叠，请先处理冲突" }
    val courses = if (id == courseId) {
        state.snapshot.courses.map { if (it.id == courseId) replacement else it }
    } else {
        state.snapshot.courses.map { if (it.id == courseId) remainingSource else it } + replacement
    }
    return state.replaceCourses(courses)
}

internal fun coursesOverlap(left: Course, right: Course, includeLeftSuspended: Boolean = false): Boolean =
    left.weekday == right.weekday && left.startPeriod <= right.endPeriod && right.startPeriod <= left.endPeriod &&
        left.weeks.any { (includeLeftSuspended || courseHeldOnWeek(left, it)) && courseHeldOnWeek(right, it) }

internal fun CourseSyncState.requireCourse(id: String): Course =
    requireNotNull(snapshot.courses.firstOrNull { it.id == id }) { "课程不存在" }

internal fun CourseSyncState.replaceCourses(courses: List<Course>): CourseSyncState {
    val effective = snapshot.copy(courses = courses)
    if (conflicts.isEmpty()) return copy(snapshot = effective)
    val imported = baseline.courses.associateBy(Course::id)
    val roots = courses.mapNotNull(Course::rescheduledFromCourseId).toSet()
    val protected = courses.filter { !sameCourse(it, imported[it.id]) || it.id in roots }
    return copy(snapshot = effective, conflicts = conflicts.map { conflict ->
        val remote = conflict.remoteCourse
        val related = if (remote == null) emptyList() else protected.filter {
            it.id != conflict.courseId && it.rescheduledFromCourseId != conflict.courseId &&
                coursesOverlap(it, remote, includeLeftSuspended = true)
        }.map(Course::id)
        conflict.copy(localCourse = courses.firstOrNull { it.id == conflict.courseId },
            relatedCourseIds = related,
            kind = if (related.isNotEmpty()) CourseSyncConflictKind.Overlap else conflict.kind)
    })
}
