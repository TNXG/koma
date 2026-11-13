package moe.tnxg.koma.feature.home.week

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import moe.tnxg.koma.domain.calendar.*
import moe.tnxg.koma.domain.model.*
import moe.tnxg.koma.domain.visual.courseVisual
import moe.tnxg.koma.domain.weeks.weeksToString
import moe.tnxg.koma.feature.home.dock.dockPress
import moe.tnxg.koma.feature.home.timeline.course.courseTone
import moe.tnxg.koma.ui.component.button.KomaTextButton
import moe.tnxg.koma.ui.component.sheet.KomaSheet
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
internal fun WeekSelector(open: Boolean, snapshot: TimetableSnapshot, startDate: String, selectedWeek: Int,
    today: LocalDate, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = KomaTheme.colors
    val total = snapshot.semester.totalWeeks.coerceAtLeast(1)
    val first = remember(startDate) { LocalDate.parse(startDate) }
    val months = remember(first, total) {
        val end = addDays(first, total * 7 - 1)
        val out = mutableListOf<LocalDate>()
        var month = LocalDate(first.year, first.monthNumber, 1)
        while (month <= end) {
            out += month
            month = month.plus(DatePeriod(months = 1))
        }
        out
    }
    val counts = remember(snapshot, startDate, months) {
        val map = HashMap<LocalDate, Int>()
        months.forEach { month ->
            val next = month.plus(DatePeriod(months = 1))
            var day = month
            while (day < next) {
                val week = weekIndex(startDate, day)
                val weekday = isoWeekday(day)
                val n = snapshot.courses.count { it.weekday == weekday && week in it.weeks }
                if (n > 0) map[day] = n
                day = addDays(day, 1)
            }
        }
        map
    }
    val list = rememberLazyListState()
    fun monthIndex(date: LocalDate) = months.indexOfFirst { it.year == date.year && it.monthNumber == date.monthNumber }
    LaunchedEffect(open) {
        if (open) list.scrollToItem(monthIndex(addDays(mondayOf(first), (selectedWeek - 1) * 7)).coerceAtLeast(0))
    }
    KomaSheet(open, onDismiss, bottomPadding = 0.dp, bottomBlur = 42.dp) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp)) {
            Spacer(Modifier.width(24.dp))
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    KomaText(it, size = 10.5f, weight = FontWeight.SemiBold, color = c.ink4)
                }
            }
        }
        LazyColumn(Modifier.fillMaxWidth().height(470.dp), state = list) {
            items(months, key = { "${it.year}-${it.monthNumber}" }) { month ->
                MonthBlock(month, first, total, snapshot.semester.name, selectedWeek, today, counts, onPick)
            }
        }
    }
}

@Composable
private fun MonthBlock(month: LocalDate, startDate: LocalDate, totalWeeks: Int, semester: String,
    selectedWeek: Int, today: LocalDate, counts: Map<LocalDate, Int>, onPick: (Int) -> Unit) {
    val c = KomaTheme.colors
    val next = month.plus(DatePeriod(months = 1))
    val length = (next.toEpochDays() - month.toEpochDays()).toInt()
    val lead = isoWeekday(month) - 1
    val cells = buildList<LocalDate?> {
        repeat(lead) { add(null) }
        for (day in 1..length) add(LocalDate(month.year, month.monthNumber, day))
        while (size % 7 != 0) add(null)
    }
    val firstWeek = weekIndex(startDate.toString(), month).coerceAtLeast(1)
    val lastWeek = weekIndex(startDate.toString(), addDays(month, length - 1)).coerceAtMost(totalWeeks)
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(Modifier.padding(top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
            KomaText("${month.monthNumber}月", size = 19, weight = FontWeight.ExtraBold, letterSpacing = -.4f)
            KomaText("$semester 第 $firstWeek–$lastWeek 周", Modifier.padding(start = 6.dp, bottom = 2.dp),
                size = 11.5f, weight = FontWeight.SemiBold, color = c.ink4, maxLines = 1)
        }
        cells.chunked(7).forEach { row ->
            val days = row.filterNotNull()
            val week = days.firstOrNull()?.let { weekIndex(startDate.toString(), it) } ?: 0
            val on = week == selectedWeek
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)
                .background(if (on) c.accentSoft else Color.Transparent, RoundedCornerShape(12.dp))) {
                Box(Modifier.width(24.dp).height(42.dp), contentAlignment = Alignment.Center) {
                    if (week in 1..totalWeeks) KomaText("$week", size = 10.5f, weight = FontWeight.Bold,
                        color = if (on) c.accent else c.ink5)
                }
                row.forEach { date ->
                    if (date == null) {
                        Spacer(Modifier.weight(1f).height(42.dp))
                    } else {
                        val isToday = date == today
                        val n = counts[date] ?: 0
                        Column(Modifier.weight(1f).height(42.dp)
                            .dockPress { onPick(weekIndex(startDate.toString(), date).coerceIn(1, totalWeeks)) },
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth().weight(1f)) {
                                KomaText(date.dayOfMonth.toString(), Modifier.align(Alignment.Center),
                                    size = 15, lineHeight = 1f, weight = FontWeight.Bold,
                                    color = if (isToday || on) c.accent else if (n > 0) c.ink else c.ink5)
                                if (n > 0) {
                                    KomaText("$n", Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp),
                                        size = 9, weight = FontWeight.Bold,
                                        color = if (isToday || on) c.accent2 else c.ink5)
                                    Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(3.dp)
                                        .background(if (isToday || on) c.accent else c.ink5, RoundedCornerShape(99.dp)))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WeekCourseSheet(courses: List<Course>, date: LocalDate, timeGrid: List<TimeSlot>, onDismiss: () -> Unit) {
    val c = KomaTheme.colors
    var displayed by remember { mutableStateOf(courses) }
    var displayedDate by remember { mutableStateOf(date) }
    if (courses.isNotEmpty()) { displayed = courses; displayedDate = date }
    val scroll = rememberScrollState()
    LaunchedEffect(courses) { if (courses.isNotEmpty()) scroll.scrollTo(0) }
    KomaSheet(courses.isNotEmpty(), onDismiss) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            KomaText(if (displayed.size > 1) "重叠课程 · ${displayed.size} 门" else "课程详情", size = 19, weight = FontWeight.Bold)
            KomaTextButton("关闭", onDismiss, Modifier.padding(8.dp), color = c.ink3)
        }
        KomaText("${formatMonthDay(displayedDate)} · 周${weekdayLabel(displayedDate)}", size = 12, color = c.ink3)
        Column(Modifier.fillMaxWidth().heightIn(max = 450.dp).verticalScroll(scroll).navigationBarsPadding()
            .padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            displayed.forEach { course ->
                val tone = courseTone(courseVisual(course.name).tone)
                Column(Modifier.fillMaxWidth().background(tone.copy(alpha = .08f), RoundedCornerShape(16.dp)).padding(16.dp)) {
                    KomaText(course.name, size = 19, weight = FontWeight.Bold, lineHeight = 1.3f)
                    val start = timeGrid.firstOrNull { it.index == course.startPeriod }?.start
                    val end = timeGrid.firstOrNull { it.index == course.endPeriod }?.end
                    val period = if (course.startPeriod == course.endPeriod) "第 ${course.startPeriod} 节" else "第 ${course.startPeriod}–${course.endPeriod} 节"
                    DetailField("节次", period)
                    DetailField("时间", if (start != null && end != null) "${formatTime(start)}–${formatTime(end)}" else "未设置")
                    DetailField("地点", course.location?.takeIf { it.isNotBlank() } ?: "未填写")
                    DetailField("教师", course.teacher?.takeIf { it.isNotBlank() } ?: "未填写")
                    DetailField("周次", "${weeksToString(course.weeks)} 周")
                }
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    val c = KomaTheme.colors
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        KomaText(label, size = 12, color = c.ink4)
        KomaText(value, Modifier.weight(1f), size = 12, weight = FontWeight.Medium, color = c.ink2)
    }
}
