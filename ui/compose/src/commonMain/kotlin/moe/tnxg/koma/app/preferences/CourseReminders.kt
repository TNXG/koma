package moe.tnxg.koma.app.preferences

import androidx.compose.runtime.Composable
import kotlinx.datetime.*
import moe.tnxg.koma.domain.calendar.addDays
import moe.tnxg.koma.domain.calendar.mondayOf
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.TimetableSnapshot

internal data class CourseReminder(val at: Instant, val course: Course)

internal fun nextCourseReminder(snapshot: TimetableSnapshot, startDate: String, now: Instant,
    leadMinutes: Int, zone: TimeZone): CourseReminder? {
    val monday = mondayOf(LocalDate.parse(startDate))
    val slots = snapshot.timeGrid.associateBy { it.index }
    var next: CourseReminder? = null
    for (course in snapshot.courses) {
        val slot = slots[course.startPeriod] ?: continue
        if (slot.start !in 0..1439 || course.weekday !in 1..7) continue
        for (week in course.weeks) {
            if (week !in 1..snapshot.semester.totalWeeks) continue
            val date = addDays(monday, (week - 1) * 7 + course.weekday - 1)
            val starts = LocalDateTime(date, LocalTime(slot.start / 60, slot.start % 60)).toInstant(zone)
            val at = Instant.fromEpochMilliseconds(starts.toEpochMilliseconds() - leadMinutes * 60_000L)
            if (at <= now || next?.at?.let { at >= it } == true) continue
            next = CourseReminder(at, course)
        }
    }
    return next
}

@Composable
internal expect fun SyncCourseReminders(snapshot: TimetableSnapshot?, startDate: String, preferences: AppPreferences)
