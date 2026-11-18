package moe.tnxg.koma.domain.course

import kotlinx.serialization.Serializable
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.domain.model.courseHeldOnWeek

@Serializable
enum class CourseSyncConflictKind { RemoteChanged, RemoteDeleted, Overlap }

@Serializable
data class CourseSyncConflict(
    val id: String,
    val courseId: String,
    val kind: CourseSyncConflictKind,
    val localCourse: Course?,
    val remoteCourse: Course?,
    val relatedCourseIds: List<String> = emptyList(),
)

/** A locally deleted course (`week == null`) or one deleted occurrence, ignored by later imports. */
@Serializable
data class CourseIgnore(val courseId: String, val week: Int? = null)

/** The latest remote baseline is distinct from the effective, locally edited timetable. */
@Serializable
data class CourseSyncState(
    val baseline: TimetableSnapshot,
    val snapshot: TimetableSnapshot,
    val conflicts: List<CourseSyncConflict> = emptyList(),
    val ignored: List<CourseIgnore> = emptyList(),
)

fun synchronizeCourses(previous: CourseSyncState?, remote: TimetableSnapshot): CourseSyncState {
    if (previous == null || previous.snapshot.schoolId != remote.schoolId || previous.snapshot.term != remote.term) {
        return CourseSyncState(remote, remote)
    }
    val baseline = previous.baseline.courses.associateBy(Course::id)
    val local = previous.snapshot.courses.associateBy(Course::id)
    val incoming = remote.courses.associateBy(Course::id)
    val derivedRoots = local.values.mapNotNull(Course::rescheduledFromCourseId).toSet()
    val manuallyEdited = local.values.filter {
        !sameCourse(it, baseline[it.id]) || it.id in derivedRoots
    }.map(Course::id).toSet()
    val result = linkedMapOf<String, Course>()
    val conflicts = linkedMapOf<String, CourseSyncConflict>()
    val previousConflicts = previous.conflicts.associateBy(CourseSyncConflict::courseId)
    val ids = baseline.keys + incoming.keys + local.keys
    for (id in ids) {
        val before = baseline[id]
        val mine = local[id]
        val theirs = incoming[id]
        val modified = !sameCourse(mine, before) || id in derivedRoots
        val oldConflict = previousConflicts[id]
        when {
            !modified || sameCourse(mine, theirs) -> theirs?.let { result[id] = it }
            sameCourse(before, theirs) -> {
                mine?.let { result[id] = it }
                if (oldConflict != null) conflicts[id] = oldConflict.copy(localCourse = mine, remoteCourse = theirs)
            }
            else -> {
                mine?.let { result[id] = it }
                conflicts[id] = CourseSyncConflict("course:$id", id,
                    if (theirs == null) CourseSyncConflictKind.RemoteDeleted else CourseSyncConflictKind.RemoteChanged,
                    mine, theirs)
            }
        }
    }
    // An incoming new/re-timed ID must not slip past an edited course simply because identity changed.
    val protected = result.values.filter { it.id in manuallyEdited }
    for (theirs in remote.courses) {
        if (sameCourse(theirs, baseline[theirs.id])) continue
        val overlaps = protected.filter {
            it.id != theirs.id && it.rescheduledFromCourseId != theirs.id &&
                coursesOverlap(it, theirs, includeLeftSuspended = true)
        }
        if (overlaps.isEmpty()) continue
        val mine = local[theirs.id]
        if (mine == null) result.remove(theirs.id) else result[theirs.id] = mine
        conflicts[theirs.id] = CourseSyncConflict("course:${theirs.id}", theirs.id,
            CourseSyncConflictKind.Overlap, mine, theirs, overlaps.map(Course::id))
    }
    val whole = previous.ignored.filter { it.week == null }.map(CourseIgnore::courseId).toSet()
    val weeks = previous.ignored.filter { it.week != null }.groupBy(CourseIgnore::courseId) { it.week!! }
    val effective = result.values.filterNot { it.id in whole }.map { course ->
        val deleted = weeks[course.id] ?: return@map course
        course.copy(weeks = course.weeks - deleted.toSet())
    }.filter { it.weeks.isNotEmpty() }
    return CourseSyncState(remote, remote.copy(courses = effective), conflicts.values.toList(), previous.ignored)
}

fun resolveCourseConflict(state: CourseSyncState, id: String, useRemote: Boolean): CourseSyncState {
    val conflict = requireNotNull(state.conflicts.firstOrNull { it.id == id }) { "同步冲突不存在" }
    if (!useRemote) return state.copy(conflicts = state.conflicts.filterNot { it.id == id })
    val remote = conflict.remoteCourse
    val courses = state.snapshot.courses.mapNotNull { course ->
        when {
            course.id == conflict.courseId || course.rescheduledFromCourseId == conflict.courseId -> null
            conflict.kind == CourseSyncConflictKind.Overlap && remote != null &&
                course.id in conflict.relatedCourseIds && coursesOverlap(course, remote) -> {
                val affected = course.weeks.filter { courseHeldOnWeek(remote, it) && courseHeldOnWeek(course, it) }
                course.copy(suspendedWeeks = (course.suspendedWeeks + affected).distinct().sorted())
            }
            else -> course
        }
    } + listOfNotNull(remote?.copy(
        // Notes are local-only data: adopting the school's schedule must not discard them.
        note = conflict.localCourse?.note ?: remote.note,
        sessionNotes = conflict.localCourse?.sessionNotes ?: remote.sessionNotes,
    ))
    val updated = state.replaceCourses(courses)
    return updated.copy(conflicts = updated.conflicts.filterNot {
        it.id == id || it.courseId == conflict.courseId ||
            state.snapshot.courses.any { course -> course.id == it.courseId && course.rescheduledFromCourseId == conflict.courseId }
    })
}

internal fun sameCourse(left: Course?, right: Course?): Boolean = when {
    left === right -> true
    left == null || right == null -> false
    else -> left.id == right.id && left.name == right.name && left.teacher == right.teacher &&
        left.location == right.location && left.weekday == right.weekday &&
        left.startPeriod == right.startPeriod && left.endPeriod == right.endPeriod &&
        left.note == right.note && normalizedNotes(left) == normalizedNotes(right) &&
        left.rescheduledFromCourseId == right.rescheduledFromCourseId &&
        left.rescheduledFromWeek == right.rescheduledFromWeek &&
        sameWeeks(left.weeks, right.weeks) && sameWeeks(left.suspendedWeeks, right.suspendedWeeks)
}

private fun normalizedNotes(course: Course): Map<Int, String> =
    course.sessionNotes.filterValues { it.isNotBlank() }.entries
        .sortedBy { it.key }.associate { it.key to it.value }

private fun sameWeeks(left: List<Int>, right: List<Int>): Boolean =
    left == right || left.all { it in right } && right.all { it in left }
