package moe.tnxg.koma.feature.home.week

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.calendar.*
import moe.tnxg.koma.domain.model.*
import moe.tnxg.koma.domain.visual.courseVisual
import moe.tnxg.koma.domain.weeks.weeksToString
import moe.tnxg.koma.feature.home.dock.dockPress
import moe.tnxg.koma.feature.home.timeline.course.courseTone
import moe.tnxg.koma.ui.component.icon.*
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

private enum class DetailIcon { Time, Teacher, Location, Note, Info }

/** mikcb: 48dp badge, compact header, independent 12×10dp detail tiles, then actions. */
@Composable
internal fun CourseDrawerDetails(course: Course, courses: List<Course>, date: LocalDate, week: Int,
    timeGrid: List<TimeSlot>, onSelect: (String) -> Unit, onNote: () -> Unit,
    onReschedule: () -> Unit, onSuspend: () -> Unit, onDelete: () -> Unit) {
    val c = KomaTheme.colors
    val tone = courseTone(courseVisual(course.name).tone)
    val weeks = "${weeksToString(course.weeks)} 周"
    val suspended = week in course.suspendedWeeks
    val start = timeGrid.firstOrNull { it.index == course.startPeriod }?.start
    val end = timeGrid.firstOrNull { it.index == course.endPeriod }?.end
    val clock = if (start != null && end != null) "${formatTime(start)}–${formatTime(end)}" else "未设置钟点"
    val others = courses.filter { it.id != course.id }
    var expanded by remember(course.id) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(48.dp).background(tone.copy(alpha = .14f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center) { KomaIcon(KomaGlyph.Book, null, Modifier.size(24.dp), tone) }
        Column(Modifier.weight(1f).padding(start = 12.dp, top = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KomaText(course.name, Modifier.weight(1f, fill = false), size = 14, weight = FontWeight.Medium,
                    lineHeight = 1.35f, maxLines = 2)
                if (suspended) KomaText("已停课", size = 11, color = c.danger)
                else if (course.rescheduledFromCourseId != null) KomaText("已调课", size = 11, color = tone)
            }
            KomaText("第 $week 周 · $weeks", Modifier.padding(top = 6.dp), size = 11, color = c.ink3, maxLines = 2)
        }
    }
    Spacer(Modifier.height(14.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailTile(DetailIcon.Time, "周${weekdayLabel(date)} · 第 ${course.startPeriod}–${course.endPeriod} 节",
            "${formatMonthDay(date)} · 第 $week 周", trailing = clock)
        DetailTile(DetailIcon.Teacher, course.teacher?.takeIf(String::isNotBlank) ?: "未填写教师", weeks)
        DetailTile(DetailIcon.Location, course.location?.takeIf(String::isNotBlank) ?: "未填写地点", weeks)
        val sessionNote = course.sessionNotes[week].orEmpty()
        val wholeNote = course.note
        DetailTile(DetailIcon.Note, sessionNote.ifBlank { wholeNote }.ifBlank { "添加课程备注" },
            when {
                sessionNote.isNotBlank() && wholeNote.isNotBlank() -> "课程简介与单周备注"
                sessionNote.isNotBlank() -> "本节课备注"
                wholeNote.isNotBlank() -> "课程简介"
                else -> "记录备忘、作业或考核要求"
            }, onClick = onNote)
    }
    Spacer(Modifier.height(12.dp))
    DetailTile(DetailIcon.Info,
        if (suspended) "第 $week 周已停课，不发送课前提醒。" else "手动修改存放在本机；同步教务课表时优先保留。",
        notice = true)
    if (others.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface2.copy(alpha = .55f))) {
            Row(Modifier.fillMaxWidth().dockPress { expanded = !expanded }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).background(c.danger.copy(alpha = .1f), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center) { KomaIcon(KomaGlyph.Calendar, null, Modifier.size(17.dp), c.danger) }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    KomaText("相关课程 · ${others.size} 门", size = 14)
                    KomaText(if (expanded) "选择课程查看详情" else others.joinToString(" · ") { it.name },
                        Modifier.padding(top = 2.dp), size = 11, color = c.ink3, maxLines = 1)
                }
                KomaIcon(if (expanded) KomaGlyph.Down else KomaGlyph.Right,
                    if (expanded) "收起相关课程" else "展开相关课程", Modifier.size(20.dp), c.ink3)
            }
            if (expanded) others.forEach { other ->
                Row(Modifier.fillMaxWidth().dockPress { onSelect(other.id) }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(courseTone(courseVisual(other.name).tone), CircleShape))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        KomaText(other.name, size = 14, maxLines = 2)
                        KomaText("第 ${other.startPeriod}–${other.endPeriod} 节 · ${other.location.orEmpty()}",
                            Modifier.padding(top = 2.dp), size = 11, color = c.ink3, maxLines = 2)
                    }
                    KomaText("切换", Modifier.padding(start = 6.dp), size = 11, color = c.accent)
                }
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DrawerSecondaryAction("调课", onReschedule, Modifier.weight(1f), courseHeldOnWeek(course, week))
        DrawerSecondaryAction(if (suspended) "恢复" else "停课", onSuspend, Modifier.weight(1f), week in course.weeks)
    }
    Spacer(Modifier.height(10.dp))
    Box(Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(RoundedCornerShape(16.dp))
        .background(c.danger.copy(alpha = .1f))
        .semantics { role = Role.Button }
        .dockPress(onClick = onDelete).padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
        KomaText("删除", size = 15, weight = FontWeight.Bold, color = c.danger)
    }
}

@Composable
private fun DetailTile(icon: DetailIcon, title: String, subtitle: String? = null,
    trailing: String? = null, notice: Boolean = false, onClick: (() -> Unit)? = null) {
    val c = KomaTheme.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface2.copy(alpha = .55f))
        .then(if (onClick != null) Modifier.semantics(mergeDescendants = true) { role = Role.Button }.dockPress(onClick = onClick) else Modifier)
        .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        DrawerDetailIcon(icon, c.ink3)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            KomaText(title, size = if (notice) 11 else 14, color = if (notice) c.ink3 else c.ink,
                lineHeight = 1.4f, maxLines = if (notice) Int.MAX_VALUE else 2)
            if (!subtitle.isNullOrBlank()) KomaText(subtitle, Modifier.padding(top = 2.dp), size = 11,
                color = c.ink3, lineHeight = 1.35f, maxLines = 2)
        }
        if (trailing != null) KomaText(trailing, Modifier.padding(start = 8.dp), size = 11, color = c.ink3)
        if (onClick != null) KomaIcon(KomaGlyph.Right, null, Modifier.padding(start = 8.dp).size(20.dp), c.ink3)
    }
}

@Composable
internal fun DrawerSecondaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, tint: Color? = null) {
    val c = KomaTheme.colors
    Box(modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(16.dp))
        .background((tint ?: c.surface2).copy(alpha = .65f))
        .semantics { role = Role.Button }
        .then(if (enabled) Modifier.dockPress(onClick = onClick) else Modifier)
        .padding(horizontal = 12.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        KomaText(label, size = 14, weight = FontWeight.Medium,
            color = when { !enabled -> c.ink5; tint != null -> c.danger; else -> c.ink })
    }
}

@Composable
private fun DrawerDetailIcon(icon: DetailIcon, color: Color) {
    when (icon) {
        DetailIcon.Teacher -> KomaIcon(KomaGlyph.Person, null, Modifier.size(18.dp), color)
        DetailIcon.Location -> KomaIcon(KomaGlyph.Location, null, Modifier.size(18.dp), color)
        DetailIcon.Note -> KomaIcon(KomaGlyph.Book, null, Modifier.size(18.dp), color)
        DetailIcon.Time, DetailIcon.Info -> Canvas(Modifier.size(18.dp)) {
            val stroke = size.width / 12
            drawCircle(color, radius = size.width * .39f, style = Stroke(stroke))
            if (icon == DetailIcon.Time) {
                drawLine(color, center, Offset(center.x, size.height * .26f), stroke, StrokeCap.Round)
                drawLine(color, center, Offset(size.width * .67f, center.y), stroke, StrokeCap.Round)
            } else {
                drawCircle(color, radius = stroke / 2, center = Offset(center.x, size.height * .32f))
                drawLine(color, Offset(center.x, center.y), Offset(center.x, size.height * .69f), stroke, StrokeCap.Round)
            }
        }
    }
}
