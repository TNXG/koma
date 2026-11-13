package moe.tnxg.koma.domain.week

import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.calendar.addDays
import moe.tnxg.koma.domain.calendar.mondayOf
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.TimeSlot
import moe.tnxg.koma.domain.model.TimetableSnapshot

data class WeekCourseGroup(val startPeriod: Int, val endPeriod: Int, val courses: List<Course>)
data class WeekDay(val date: LocalDate, val groups: List<WeekCourseGroup>)
data class WeekSchedule(val week: Int, val days: List<WeekDay>, val periodCount: Int)

fun buildWeekSchedule(snapshot: TimetableSnapshot, startDate: String, week: Int): WeekSchedule {
    val boundedWeek = week.coerceIn(1, snapshot.semester.totalWeeks.coerceAtLeast(1))
    val monday = addDays(mondayOf(LocalDate.parse(startDate)), (boundedWeek - 1) * 7)
    val active = snapshot.courses.filter { boundedWeek in it.weeks }.groupBy(Course::weekday)
    val days = (1..7).map { weekday ->
        val sorted = active[weekday].orEmpty().sortedWith(compareBy(Course::startPeriod, Course::endPeriod, Course::id))
        val groups = mutableListOf<WeekCourseGroup>()
        var members = mutableListOf<Course>()
        var start = 0
        var end = 0
        for (course in sorted) {
            if (members.isNotEmpty() && course.startPeriod > end) {
                groups += WeekCourseGroup(start, end, members)
                members = mutableListOf()
            }
            if (members.isEmpty()) { start = course.startPeriod; end = course.endPeriod }
            else end = maxOf(end, course.endPeriod)
            members += course
        }
        if (members.isNotEmpty()) groups += WeekCourseGroup(start, end, members)
        WeekDay(addDays(monday, weekday - 1), groups)
    }
    return WeekSchedule(boundedWeek, days, maxOf(1,
        snapshot.timeGrid.maxOfOrNull(TimeSlot::index) ?: 0,
        snapshot.courses.maxOfOrNull(Course::endPeriod) ?: 0))
}

/** Fractional period coordinate; breaks occupy only the gap between actual period indices. */
fun weekTimePosition(timeGrid: List<TimeSlot>, minutes: Int): Float? {
    val slots = timeGrid.sortedBy(TimeSlot::index)
    if (slots.isEmpty() || minutes < slots.first().start || minutes > slots.last().end) return null
    for ((index, slot) in slots.withIndex()) {
        if (minutes in slot.start..slot.end) {
            return slot.index - 1 + (minutes - slot.start).toFloat() / (slot.end - slot.start).coerceAtLeast(1)
        }
        val next = slots.getOrNull(index + 1) ?: continue
        if (minutes > slot.end && minutes < next.start) {
            val fraction = (minutes - slot.end).toFloat() / (next.start - slot.end)
            return slot.index + fraction * (next.index - 1 - slot.index)
        }
    }
    return null
}
