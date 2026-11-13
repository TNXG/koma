package moe.tnxg.koma.domain.timeline

import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.model.Course

enum class RowState { Past, Now, Next, Later }
sealed interface Stop
data class CourseRow(val course: Course, val start: Int?, val end: Int?, val state: RowState, val pct: Float) : Stop
data class GapStop(val start: Int, val end: Int, val minutes: Int, val state: RowState) : Stop
sealed interface TimelineBlock { val anchor: String }
data class DayBlock(override val anchor: String, val date: LocalDate, val relative: String?, val week: Int,
    val stops: List<Stop>, val courses: List<CourseRow>) : TimelineBlock
data class FreeBlock(override val anchor: String, val start: LocalDate, val end: LocalDate, val label: String,
    val days: Int) : TimelineBlock
data class WeekBlock(override val anchor: String, val week: Int) : TimelineBlock
data class DaySummary(val parts: List<String>, val empty: Boolean)
