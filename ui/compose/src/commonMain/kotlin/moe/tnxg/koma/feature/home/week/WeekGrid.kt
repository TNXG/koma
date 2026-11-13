package moe.tnxg.koma.feature.home.week

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.calendar.*
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.TimeSlot
import moe.tnxg.koma.domain.visual.courseVisual
import moe.tnxg.koma.domain.week.*
import moe.tnxg.koma.feature.home.dock.dockPress
import moe.tnxg.koma.feature.home.timeline.course.courseTone
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme
import moe.tnxg.koma.app.preferences.LocalAppPreferences
import moe.tnxg.koma.app.preferences.ScheduleDensity
import androidx.compose.ui.graphics.graphicsLayer

private val AxisWidth = 30.dp

@Composable
internal fun WeekDayHeader(days: List<WeekDay>, selected: LocalDate, today: LocalDate,
    week: Int, onWeek: () -> Unit, onDay: (LocalDate) -> Unit) {
    val c = KomaTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.width(AxisWidth).height(40.dp).dockPress(onClick = onWeek), contentAlignment = Alignment.Center) {
            KomaText("$week 周", size = 10, color = c.ink4, weight = FontWeight.SemiBold)
        }
        days.forEach { day ->
            val active = day.date == selected
            val isToday = day.date == today
            Column(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(8.dp))
                .background(if (active) c.accentSoft else Color.Transparent)
                .semantics { contentDescription = "${formatMonthDay(day.date)}，周${weekdayLabel(day.date)}，查看当天课程" }
                .dockPress { onDay(day.date) }.padding(vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                KomaText("周${weekdayLabel(day.date)}", size = 10, lineHeight = 1.2f, weight = FontWeight.SemiBold,
                    color = if (active || isToday) c.accent else c.ink2)
                KomaText("${day.date.monthNumber}/${day.date.dayOfMonth}", Modifier.padding(top = 2.dp),
                    size = 8.5f, lineHeight = 1.2f, weight = FontWeight.Medium,
                    color = if (active || isToday) c.accent else c.ink4)
                if (isToday) Box(Modifier.padding(top = 2.dp).width(18.dp).height(2.dp).background(c.accent.copy(alpha = .35f)))
            }
        }
    }
}

@Composable
internal fun WeekGrid(schedule: WeekSchedule, timeGrid: List<TimeSlot>, today: LocalDate, minutes: Int,
    modifier: Modifier = Modifier, onCourse: (LocalDate, List<Course>) -> Unit) {
    val c = KomaTheme.colors
    val prefs = LocalAppPreferences.current
    val slots = remember(timeGrid) { timeGrid.associateBy(TimeSlot::index) }
    val now = remember(timeGrid, minutes) { weekTimePosition(timeGrid, minutes) }
    BoxWithConstraints(modifier) {
        val padding = 8.dp
        val rowHeight = ((maxHeight - padding * 2) / schedule.periodCount).coerceAtLeast(28.dp)
        val gridHeight = rowHeight * schedule.periodCount
        val compact = rowHeight < 46.dp
        Row(Modifier.padding(horizontal = 6.dp, vertical = padding),
            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Column(Modifier.width(AxisWidth)) {
                repeat(schedule.periodCount) { i ->
                    Column(Modifier.height(rowHeight).fillMaxWidth().padding(top = 4.dp, end = 2.dp),
                        horizontalAlignment = Alignment.End) {
                        KomaText("${i + 1}", size = 13, lineHeight = 1.1f, weight = FontWeight.Bold, color = c.ink3)
                        slots[i + 1]?.let {
                            KomaText(formatTime(it.start), size = 7, lineHeight = 1.1f, weight = FontWeight.Medium, color = c.ink5)
                            if (!compact) KomaText(formatTime(it.end), size = 7, lineHeight = 1.1f,
                                weight = FontWeight.Medium, color = c.ink5)
                        }
                    }
                }
            }
            schedule.days.take(if(prefs.showWeekend)7 else 5).forEach { day ->
                Box(Modifier.weight(1f).height(gridHeight)) {
                    day.groups.forEach { group ->
                        WeekGridCard(group, day.date, today, slots, minutes, rowHeight, compact,
                            group.courses.all { schedule.week !in it.weeks },
                            Modifier.offset(y = rowHeight * (group.startPeriod - 1) + 2.dp)
                                .fillMaxWidth().height(rowHeight * (group.endPeriod - group.startPeriod + 1) - 4.dp)) {
                            onCourse(day.date, group.courses)
                        }
                    }
                    // “现在”只画在正在上课的时段里；课间和放学后不显示，也不随钟点滑过空白。
                    if (day.date == today && now != null && activeGroupFor(day, slots, minutes) != null) {
                        Box(Modifier.offset(y = rowHeight * now).fillMaxWidth().height(2.dp)
                            .background(c.accent, RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
    }
}

internal fun activeGroupFor(day: WeekDay, slots: Map<Int, TimeSlot>, minutes: Int): WeekCourseGroup? =
    day.groups.firstOrNull { group ->
        val start = slots[group.startPeriod]?.start ?: return@firstOrNull false
        val end = slots[group.endPeriod]?.end ?: return@firstOrNull false
        minutes in start until end
    }

@Composable
private fun WeekGridCard(group: WeekCourseGroup, date: LocalDate, today: LocalDate, slots: Map<Int, TimeSlot>,
    minutes: Int, rowHeight: Dp, compact: Boolean, inactive: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = KomaTheme.colors
    val prefs = LocalAppPreferences.current
    val course = group.courses.first()
    val tone = if(prefs.colorCourses)courseTone(courseVisual(course.name).tone) else c.accent
    val end = slots[group.endPeriod]?.end
    val start = slots[group.startPeriod]?.start
    val past = date < today || (date == today && end != null && minutes >= end)
    val ongoing = date == today && start != null && end != null && minutes in start until end
    val conflict = group.courses.size > 1
    val shape = RoundedCornerShape(9.dp)
    val foreground = if (past) lerp(tone, c.ink3, .4f) else lerp(tone, c.ink, .2f)
    val fill = lerp(c.surface, tone, if (ongoing) .2f else if (past) .06f else .11f)
    val location = course.location?.takeIf { it.isNotBlank() }
    val textSize = when(prefs.density){ScheduleDensity.Compact->9;ScheduleDensity.Standard->10;ScheduleDensity.Relaxed->12}
    Column(modifier.graphicsLayer { alpha=if(inactive).4f else 1f }.clip(shape).background(fill)
        .then(if (ongoing || conflict) Modifier.border(1.dp, if (conflict) c.toneAmber.copy(alpha = .65f) else tone, shape) else Modifier)
        .semantics { contentDescription = "${course.name}，${course.location.orEmpty()}，第 ${group.startPeriod} 至 ${group.endPeriod} 节" +
            if (conflict) "，${group.courses.size} 门重叠课程" else "" }
        .dockPress(scale = .97f, onClick = onClick).padding(horizontal = 4.dp, vertical = 5.dp)) {
        if (conflict) KomaText("${group.courses.size} 门", size = 8, lineHeight = 1.1f, weight = FontWeight.Bold, color = c.toneAmber)
        KomaText(course.name, size = textSize, lineHeight = 1.25f, weight = FontWeight.Bold, color = foreground,
            maxLines = if (group.endPeriod > group.startPeriod) 5 else 2)
        if (prefs.showRoom && location != null && (!compact || rowHeight * (group.endPeriod - group.startPeriod + 1) >= 52.dp)) {
            KomaText(location, Modifier.padding(top = 3.dp), size = 8.5f, lineHeight = 1.2f, weight = FontWeight.Medium,
                color = foreground.copy(alpha = .65f), maxLines = 2)
        }
    }
}
