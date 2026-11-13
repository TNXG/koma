package moe.tnxg.koma.domain.calendar

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.TimetableSnapshot

fun today(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
fun nowMinutes(): Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).let { it.hour * 60 + it.minute }
fun addDays(date: LocalDate, days: Int): LocalDate = LocalDate.fromEpochDays(date.toEpochDays() + days)
fun daysBetween(a: LocalDate, b: LocalDate): Int = b.toEpochDays() - a.toEpochDays()
fun isoWeekday(date: LocalDate): Int = date.dayOfWeek.ordinal + 1
fun mondayOf(date: LocalDate): LocalDate = addDays(date, 1 - isoWeekday(date))
fun formatMonthDay(date: LocalDate): String = "${date.monthNumber}月${date.dayOfMonth}日"
fun weekdayLabel(date: LocalDate): String = "一二三四五六日"[isoWeekday(date) - 1].toString()
fun weekIndex(startDate: String, date: LocalDate): Int {
    val days = daysBetween(LocalDate.parse(startDate), date)
    return (if (days >= 0) days / 7 else (days - 6) / 7) + 1
}
fun formatTime(minutes: Int): String = "${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"
fun formatDuration(minutes: Int): String {
    val total = minutes.coerceAtLeast(0)
    if (total < 60) return "$total 分钟"
    return if (total % 60 == 0) "${total / 60} 小时" else "${total / 60} 小时 ${total % 60} 分钟"
}

data class CourseTime(val course: Course, val start: Int?, val end: Int?)

fun coursesOnDate(snapshot: TimetableSnapshot, startDate: String, date: LocalDate): List<CourseTime> {
    val week = weekIndex(startDate, date)
    val weekday = isoWeekday(date)
    return snapshot.courses.filter { it.weekday == weekday && week in it.weeks }.sortedBy(Course::startPeriod).map { course ->
        CourseTime(course, snapshot.timeGrid.find { it.index == course.startPeriod }?.start,
            snapshot.timeGrid.find { it.index == course.endPeriod }?.end)
    }
}
