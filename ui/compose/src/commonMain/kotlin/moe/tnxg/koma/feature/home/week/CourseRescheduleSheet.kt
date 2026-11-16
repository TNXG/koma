package moe.tnxg.koma.feature.home.week

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.tnxg.koma.domain.calendar.formatTime
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.TimeSlot
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.feature.home.dock.dockPress
import moe.tnxg.koma.ui.component.button.KomaButton
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon
import moe.tnxg.koma.ui.component.sheet.KomaSheet
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.component.text.komaFontFamily
import moe.tnxg.koma.ui.theme.KomaTheme
import top.yukonga.miuix.kmp.blur.Backdrop

private enum class ReschedulePicker { Week, Start, End }

private val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * Reschedule follows mikcb's flow: current occurrence → −/field/+ target week → weekday chips →
 * start/end section fields with picker sheets → location field → live target preview → confirm.
 */
@Composable
internal fun CourseRescheduleSheet(open: Boolean, course: Course, sourceWeek: Int,
    snapshot: TimetableSnapshot, backdrop: Backdrop, error: String?,
    onDismiss: () -> Unit, onConfirm: (Int, Int, Int, Int, String?) -> Unit) {
    val total = snapshot.semester.totalWeeks.coerceAtLeast(1)
    val sections = remember(snapshot.timeGrid, course) {
        (snapshot.timeGrid.map(TimeSlot::index) + listOf(course.startPeriod, course.endPeriod))
            .filter { it >= 1 }.distinct().sorted()
    }
    var week by remember(open, course.id) { mutableIntStateOf(sourceWeek) }
    var day by remember(open, course.id) { mutableIntStateOf(course.weekday) }
    var start by remember(open, course.id) { mutableIntStateOf(course.startPeriod) }
    var end by remember(open, course.id) { mutableIntStateOf(course.endPeriod) }
    var location by remember(open, course.id) { mutableStateOf(course.location.orEmpty()) }
    var picker by remember(open, course.id) { mutableStateOf<ReschedulePicker?>(null) }
    var editingLocation by remember(open, course.id) { mutableStateOf(false) }
    val changed = week != sourceWeek || day != course.weekday || start != course.startPeriod ||
        end != course.endPeriod || location.trim() != course.location.orEmpty().trim()
    val clock = clockRange(snapshot.timeGrid, start, end)
    val currentClock = clockRange(snapshot.timeGrid, course.startPeriod, course.endPeriod)
    val currentLine = "当前：第 $sourceWeek 周 · 周${weekdays[course.weekday - 1]} · " +
        "第 ${course.startPeriod}–${course.endPeriod} 节" + (currentClock?.let { " · $it" } ?: "")
    val maxHeight = sheetMaxHeight()

    KomaSheet(open, onDismiss, glassBackdrop = backdrop) {
        Column(Modifier.fillMaxWidth().imePadding()) {
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight - 96.dp).verticalScroll(rememberScrollState())) {
                DrawerSheetHeader(KomaGlyph.Calendar, "调整单次课程",
                    "仅移动第 $sourceWeek 周的这一次安排，不影响其他周次")
                Spacer(Modifier.height(12.dp))
                KomaText(currentLine, size = 11, color = KomaTheme.colors.ink3)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepButton("−", "上一周", week > 1) { week-- }
                    DrawerFieldCard("调到第几周", "第 $week 周", Modifier.weight(1f).padding(horizontal = 8.dp)) { picker = ReschedulePicker.Week }
                    StepButton("＋", "下一周", week < total) { week++ }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    weekdays.forEachIndexed { index, label ->
                        val selected = day == index + 1
                        Box(Modifier.weight(1f).padding(horizontal = 2.dp)) {
                            WeekdayChip(label, selected) { day = index + 1 }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    DrawerFieldCard("开始节次", "第 $start 节", Modifier.weight(1f)) { picker = ReschedulePicker.Start }
                    Spacer(Modifier.width(12.dp))
                    DrawerFieldCard("结束节次", "第 $end 节", Modifier.weight(1f)) { picker = ReschedulePicker.End }
                }
                if (clock != null) KomaText(clock, Modifier.padding(top = 4.dp), size = 11, color = KomaTheme.colors.ink3)
                Spacer(Modifier.height(10.dp))
                DrawerFieldCard("上课地点", location.ifBlank { "手动输入" }, Modifier.fillMaxWidth()) { editingLocation = true }
                if (changed) {
                    Spacer(Modifier.height(10.dp))
                    KomaText("→ 第 $week 周 · 周${weekdays[day - 1]} · 第 $start–$end 节" +
                        (clock?.let { " · $it" } ?: ""), size = 13, weight = FontWeight.SemiBold,
                        color = KomaTheme.colors.accent)
                }
                if (error != null) KomaText(error, Modifier.padding(top = 10.dp), size = 12, color = KomaTheme.colors.danger)
                Spacer(Modifier.height(12.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DrawerSecondaryAction("取消", onDismiss, Modifier.weight(1f))
                KomaButton("确认调课", { onConfirm(week, day, start, end, location.trim().ifEmpty { null }) },
                    Modifier.weight(1f), enabled = changed)
            }
        }
    }

    PickerSheet(picker == ReschedulePicker.Week, "调到第几周", (1..total).map { it to "第 $it 周" },
        week, backdrop, onPick = { week = it; picker = null }, onDismiss = { picker = null })
    PickerSheet(picker == ReschedulePicker.Start, "开始节次", sections.map { it to "第 $it 节" },
        start, backdrop, onPick = { start = it; if (end < it) end = it; picker = null },
        onDismiss = { picker = null })
    PickerSheet(picker == ReschedulePicker.End, "结束节次",
        sections.filter { it >= start }.map { it to "第 $it 节" }, end, backdrop,
        onPick = { end = it; picker = null }, onDismiss = { picker = null })
    LocationSheet(editingLocation, location, backdrop,
        onConfirm = { location = it; editingLocation = false }, onDismiss = { editingLocation = false })
}

@Composable
private fun StepButton(label: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = KomaTheme.colors
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
        .background(c.surface2.copy(alpha = if (enabled) .8f else .4f))
        .semantics { role = Role.Button; contentDescription = description }
        .then(if (enabled) Modifier.dockPress(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center) {
        KomaText(label, size = 20, weight = FontWeight.Medium, color = if (enabled) c.ink2 else c.ink5)
    }
}

@Composable
private fun WeekdayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = KomaTheme.colors
    Box(Modifier.fillMaxWidth().clip(CircleShape)
        .background(if (selected) c.accent.copy(alpha = .12f) else c.surface2.copy(alpha = .5f))
        .semantics { role = Role.RadioButton }
        .dockPress(onClick = onClick).padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
        KomaText(label, size = 11, weight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) c.accent else c.ink2)
    }
}

@Composable
private fun PickerSheet(open: Boolean, title: String, options: List<Pair<Int, String>>,
    selected: Int, backdrop: Backdrop, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = KomaTheme.colors
    KomaSheet(open, onDismiss, glassBackdrop = backdrop) {
        Column(Modifier.fillMaxWidth()) {
            KomaText(title, size = 17, weight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                options.forEach { (value, label) ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .semantics { role = Role.RadioButton }
                        .dockPress(onClick = { onPick(value) }).padding(horizontal = 8.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        KomaText(label, Modifier.weight(1f), size = 15,
                            color = if (value == selected) c.accent else c.ink)
                        if (value == selected) KomaIcon(KomaGlyph.Right, null, Modifier.size(16.dp), c.accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationSheet(open: Boolean, initial: String, backdrop: Backdrop,
    onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val c = KomaTheme.colors
    var text by remember(open, initial) { mutableStateOf(initial) }
    KomaSheet(open, onDismiss, glassBackdrop = backdrop) {
        Column(Modifier.fillMaxWidth()) {
            KomaText("上课地点", size = 17, weight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(c.surface2.copy(alpha = .7f)).padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                KomaIcon(KomaGlyph.Location, null, Modifier.size(18.dp), c.ink3)
                BasicTextField(text, { text = it }, Modifier.weight(1f).padding(start = 10.dp)
                    .semantics { contentDescription = "上课地点" }, singleLine = true,
                    textStyle = TextStyle(color = c.ink, fontSize = 15.sp, fontFamily = komaFontFamily()),
                    cursorBrush = SolidColor(c.accent))
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DrawerSecondaryAction("取消", onDismiss, Modifier.weight(1f))
                KomaButton("确定", { onConfirm(text) }, Modifier.weight(1f))
            }
        }
    }
}

private fun clockRange(grid: List<TimeSlot>, start: Int, end: Int): String? {
    val from = grid.firstOrNull { it.index == start } ?: return null
    val to = grid.firstOrNull { it.index == end } ?: return null
    return "${formatTime(from.start)}–${formatTime(to.end)}"
}
