package moe.tnxg.koma.domain.timeline

import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.calendar.CourseTime
import moe.tnxg.koma.domain.calendar.addDays
import moe.tnxg.koma.domain.calendar.coursesOnWeekday
import moe.tnxg.koma.domain.calendar.daysBetween
import moe.tnxg.koma.domain.calendar.isoWeekday
import moe.tnxg.koma.domain.model.TimetableSnapshot

enum class AgendaState { NoTimetable, NeedsStart, BeforeTerm, AfterTerm, NoClasses, Classes, Finished }
enum class ClassState { Finished, Current, Next, Later, Unknown }

data class AgendaCourse(val time: CourseTime, val state: ClassState)

class TodayAgenda internal constructor(
    val state: AgendaState,
    val date: LocalDate,
    val week: Int? = null,
    val courses: List<AgendaCourse> = emptyList(),
    val nextChangeMinute: Int? = null,
    private val focusIndex: Int = 0,
) {
    /** Keep chronological order and include the current or next course when space is limited. */
    fun visibleCourses(capacity: Int): List<AgendaCourse> {
        if (capacity <= 0) return emptyList()
        if (courses.size <= capacity) return courses
        val first = focusIndex.coerceAtMost(courses.size - capacity)
        return courses.subList(first, first + capacity)
    }
}

fun todayAgenda(snapshot: TimetableSnapshot?, startDate: String?, date: LocalDate, minute: Int): TodayAgenda {
    if (snapshot == null) return TodayAgenda(AgendaState.NoTimetable, date)
    val start = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: return TodayAgenda(AgendaState.NeedsStart, date)
    val offset = daysBetween(start, date)
    val week = if (offset < 0) 0 else offset / 7 + 1
    if (week > snapshot.semester.totalWeeks) return TodayAgenda(AgendaState.AfterTerm, date)
    if (week > 0) {
        val times = coursesOnWeekday(snapshot, week, isoWeekday(date))
        if (times.isNotEmpty()) return classify(date, week, times, minute)
    }

    val nextOffset = nextClassOffsets(snapshot, isoWeekday(start), offset, limit = 1)[0]
    if (nextOffset != Int.MAX_VALUE) return upcomingAgenda(snapshot, start, nextOffset)
    return if (week == 0) TodayAgenda(AgendaState.BeforeTerm, date)
    else TodayAgenda(AgendaState.NoClasses, date, week)
}

/** The next [limit] distinct teaching dates strictly after the date already on screen. */
fun upcomingAgendas(snapshot: TimetableSnapshot?, startDate: String?, after: LocalDate, limit: Int): List<TodayAgenda> {
    if (snapshot == null || startDate == null || limit <= 0) return emptyList()
    val start = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: return emptyList()
    val offset = daysBetween(start, after)
    return buildList {
        for (next in nextClassOffsets(snapshot, isoWeekday(start), offset, limit)) {
            if (next == Int.MAX_VALUE) break
            add(upcomingAgenda(snapshot, start, next))
        }
    }
}

private fun upcomingAgenda(snapshot: TimetableSnapshot, start: LocalDate, offset: Int): TodayAgenda {
    val date = addDays(start, offset)
    val week = offset / 7 + 1
    return classify(date, week, coursesOnWeekday(snapshot, week, isoWeekday(date)), null)
}

private fun nextClassOffsets(snapshot: TimetableSnapshot, startWeekday: Int, after: Int, limit: Int): IntArray {
    val nearest = IntArray(limit) { Int.MAX_VALUE }
    val weeks = 1..snapshot.semester.totalWeeks
    for (course in snapshot.courses) {
        if (course.weekday !in 1..7) continue
        val weekdayOffset = (course.weekday - startWeekday + 7) % 7
        for (week in course.weeks) {
            if (week !in weeks) continue
            val candidate = (week - 1) * 7 + weekdayOffset
            if (candidate <= after || candidate >= nearest.last() || week in course.suspendedWeeks) continue
            val result = nearest.binarySearch(candidate)
            if (result >= 0) continue
            val index = -result - 1
            nearest.copyInto(nearest, index + 1, index, nearest.lastIndex)
            nearest[index] = candidate
        }
    }
    return nearest
}

private fun classify(date: LocalDate, week: Int, times: List<CourseTime>, minute: Int?): TodayAgenda {
    var current = -1
    var next = -1
    var unknown = -1
    var boundary = 24 * 60
    val courses = times.mapIndexedTo(ArrayList(times.size)) { index, time ->
        val start = time.start
        val end = time.end
        val state = if (minute == null) {
            when {
                start == null -> ClassState.Unknown
                index == 0 -> ClassState.Next
                else -> ClassState.Later
            }
        } else when {
            end != null && end <= minute -> ClassState.Finished
            start != null && start > minute -> {
                boundary = minOf(boundary, start)
                if (next == -1) {
                    next = index
                    ClassState.Next
                } else ClassState.Later
            }
            start != null && end != null -> {
                boundary = minOf(boundary, end)
                if (current == -1) current = index
                ClassState.Current
            }
            else -> {
                if (end != null) boundary = minOf(boundary, end)
                if (unknown == -1) unknown = index
                ClassState.Unknown
            }
        }
        AgendaCourse(time, state)
    }
    val focus = when {
        minute == null -> 0
        current >= 0 -> current
        next >= 0 -> next
        unknown >= 0 -> unknown
        else -> courses.lastIndex
    }
    val focusedCourse = courses[focus]
    val state = if (focusedCourse.state == ClassState.Finished) AgendaState.Finished else AgendaState.Classes
    val visibleFocus = if (state == AgendaState.Classes && courses.removeAll { it.state == ClassState.Finished }) {
        courses.indexOf(focusedCourse)
    } else focus
    return TodayAgenda(state, date, week, courses, boundary.takeIf { it < 24 * 60 }, visibleFocus)
}
