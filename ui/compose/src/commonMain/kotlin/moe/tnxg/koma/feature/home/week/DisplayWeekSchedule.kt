package moe.tnxg.koma.feature.home.week

import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.domain.week.WeekDay
import moe.tnxg.koma.domain.week.WeekSchedule
import moe.tnxg.koma.domain.week.buildWeekSchedule

/** Inactive courses never obscure or count as a conflict with current teaching-week courses. */
internal fun displayWeekSchedule(snapshot: TimetableSnapshot, startDate: String, week: Int,
    showOtherWeeks: Boolean): WeekSchedule {
    // Keep cancelled occurrences reachable for restoration without restoring them to reminders/timeline.
    val originalsById = snapshot.courses.associateBy { it.id }
    val displaySnapshot = snapshot.copy(courses = snapshot.courses.map { it.copy(suspendedWeeks = emptyList()) })
    val rawActive = buildWeekSchedule(displaySnapshot, startDate, week)
    val active = rawActive.copy(days = rawActive.days.map { day ->
        day.copy(groups = day.groups.map { group -> group.copy(courses = group.courses.map { originalsById.getValue(it.id) }) })
    })
    if (!showOtherWeeks) return active
    val inactive = snapshot.courses.filter { active.week !in it.weeks }
    if (inactive.isEmpty()) return active
    val originals = inactive.associateBy { it.id }
    val other = buildWeekSchedule(snapshot.copy(courses = inactive.map { it.copy(weeks = listOf(active.week), suspendedWeeks = emptyList()) }),
        startDate, active.week)
    return active.copy(days = active.days.mapIndexed { index, day ->
        val extra = other.days[index].groups.filter { ghost ->
            day.groups.none { it.startPeriod <= ghost.endPeriod && ghost.startPeriod <= it.endPeriod }
        }.map { group -> group.copy(courses = group.courses.map { originals.getValue(it.id) }) }
        WeekDay(day.date, (day.groups + extra).sortedBy { it.startPeriod })
    })
}
