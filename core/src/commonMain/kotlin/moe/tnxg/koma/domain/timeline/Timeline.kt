package moe.tnxg.koma.domain.timeline

import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.calendar.*
import moe.tnxg.koma.domain.model.TimetableSnapshot

const val MIN_GAP_MINUTES = 40

fun buildTimeline(snapshot: TimetableSnapshot, startDate: String, today: LocalDate, nowMinutes: Int): List<TimelineBlock> {
    val termStart = LocalDate.parse(startDate)
    val out = mutableListOf<TimelineBlock>()
    var nextMarked = false
    repeat((snapshot.semester.totalWeeks * 7).coerceAtLeast(0)) { offset ->
        val date = addDays(termStart, offset)
        val iso = date.toString()
        val rows = coursesOnDate(snapshot, startDate, date).map { item ->
            val state: RowState
            val pct: Float
            when {
                date < today || date == today && item.end != null && item.end <= nowMinutes -> {
                    state = RowState.Past; pct = 100f
                }
                date == today && item.start != null && item.start <= nowMinutes -> {
                    state = RowState.Now
                    pct = (nowMinutes - item.start) * 100f / ((item.end ?: item.start) - item.start).coerceAtLeast(1)
                }
                date == today && !nextMarked -> { nextMarked = true; state = RowState.Next; pct = 0f }
                else -> { state = RowState.Later; pct = 0f }
            }
            CourseRow(item.course, item.start, item.end, state, pct)
        }
        val weekday = isoWeekday(date)
        val week = offset / 7 + 1
        if (weekday == 1) out += WeekBlock(iso, week)
        if (rows.isNotEmpty() || weekday == 1) {
            val diff = daysBetween(today, date)
            val relative = listOf("今天", "明天", "后天").getOrNull(diff)
            out += DayBlock(iso, date, relative, week, withGaps(rows, date, today, nowMinutes), rows)
        } else {
            val previous = out.lastOrNull()
            if (previous is FreeBlock && isoWeekday(previous.end) != 7) {
                out[out.lastIndex] = previous.copy(end = date, days = previous.days + 1)
            } else out += FreeBlock(iso, date, date, if (weekday >= 6) "周末" else "无课", 1)
        }
    }
    return out
}

private fun withGaps(rows: List<CourseRow>, date: LocalDate, today: LocalDate, now: Int): List<Stop> = buildList {
    rows.forEachIndexed { index, row ->
        val end = rows.getOrNull(index - 1)?.end
        val start = row.start
        if (end != null && start != null && start - end >= MIN_GAP_MINUTES) {
            val state = when {
                date < today || date == today && start <= now -> RowState.Past
                date == today && end <= now -> RowState.Now
                else -> RowState.Later
            }
            add(GapStop(end, start, start - end, state))
        }
        add(row)
    }
}

fun blockAnchors(blocks: List<TimelineBlock>): Map<String, String> = buildMap {
    blocks.forEach { block ->
        when (block) {
            is DayBlock -> put(block.anchor, block.anchor)
            is FreeBlock -> repeat(block.days) { put(addDays(block.start, it).toString(), block.anchor) }
            is WeekBlock -> Unit
        }
    }
}

fun countCoursesByDay(blocks: List<TimelineBlock>): Map<String, Int> = blocks.filterIsInstance<DayBlock>()
    .associate { it.anchor to it.courses.size }

fun describeDay(snapshot: TimetableSnapshot, startDate: String, date: LocalDate, today: LocalDate, nowMinutes: Int): DaySummary {
    val start = LocalDate.parse(startDate)
    val week = weekIndex(startDate, date)
    if (week < 1) return DaySummary(listOf("距离开学 ${daysBetween(date, start)} 天"), false)
    if (week > snapshot.semester.totalWeeks) return DaySummary(listOf("学期已结束"), false)
    val rows = coursesOnDate(snapshot, startDate, date)
    val last = when {
        rows.isEmpty() -> "无课"
        date == today -> "${rows.size} 节课，剩 ${rows.count { it.end == null || it.end > nowMinutes }} 节"
        else -> "${rows.size} 节课"
    }
    return DaySummary(listOf("第 $week 周", if (week % 2 == 1) "单周" else "双周", last), rows.isEmpty())
}
