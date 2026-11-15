package moe.tnxg.koma.feature.home.week

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.feature.home.dock.dockPress
import moe.tnxg.koma.ui.component.button.KomaButton
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon
import moe.tnxg.koma.ui.component.sheet.KomaSheet
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.component.text.komaFontFamily
import moe.tnxg.koma.ui.theme.KomaTheme
import top.yukonga.miuix.kmp.blur.Backdrop

@Composable
internal fun sheetMaxHeight(): Dp = with(LocalDensity.current) {
    LocalWindowInfo.current.containerSize.height.toDp() * .78f
}

/** Header shared by the nested editors: tinted glyph badge, title, one explanatory line. */
@Composable
internal fun DrawerSheetHeader(glyph: KomaGlyph, title: String, subtitle: String) {
    val c = KomaTheme.colors
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.size(44.dp).background(c.accent.copy(alpha = .12f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center) { KomaIcon(glyph, null, Modifier.size(22.dp), c.accent) }
        Column(Modifier.weight(1f).padding(start = 12.dp, top = 2.dp)) {
            KomaText(title, size = 14, weight = FontWeight.Medium)
            KomaText(subtitle, Modifier.padding(top = 4.dp), size = 11, color = c.ink3, lineHeight = 1.4f, maxLines = 2)
        }
    }
}

/** Compact select field: label above the current value, with an affordance chevron. */
@Composable
internal fun DrawerFieldCard(label: String, value: String, modifier: Modifier = Modifier,
    onClick: () -> Unit) {
    val c = KomaTheme.colors
    Row(modifier.clip(RoundedCornerShape(10.dp)).background(c.surface2.copy(alpha = .7f))
        .semantics(mergeDescendants = true) { role = Role.Button; contentDescription = "$label，$value" }
        .dockPress(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            KomaText(label, size = 10, color = c.ink4)
            KomaText(value, Modifier.padding(top = 2.dp), size = 13, weight = FontWeight.Medium, maxLines = 1)
        }
        KomaIcon(KomaGlyph.Right, null, Modifier.padding(start = 6.dp).size(16.dp), c.ink3)
    }
}

@Composable
private fun DrawerOptionRow(glyph: KomaGlyph, title: String, subtitle: String, enabled: Boolean = true,
    onClick: () -> Unit) {
    val c = KomaTheme.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(c.surface2.copy(alpha = if (enabled) .6f else .35f))
        .semantics(mergeDescendants = true) { role = Role.Button }
        .then(if (enabled) Modifier.dockPress(onClick = onClick) else Modifier)
        .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        KomaIcon(glyph, null, Modifier.size(18.dp), if (enabled) c.ink2 else c.ink5)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            KomaText(title, size = 14, weight = FontWeight.Medium, color = if (enabled) c.ink else c.ink5)
            if (subtitle.isNotBlank()) KomaText(subtitle, Modifier.padding(top = 2.dp), size = 11,
                color = if (enabled) c.ink3 else c.ink5)
        }
        KomaIcon(KomaGlyph.Right, null, Modifier.size(16.dp), if (enabled) c.ink3 else c.ink5)
    }
}

/** Two note layers, matching the reference: shared course description plus one week's note. */
@Composable
internal fun CourseNoteSheet(open: Boolean, course: Course, week: Int, backdrop: Backdrop,
    error: String?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var whole by remember(open, course.id) { mutableStateOf(course.note) }
    var session by remember(open, course.id, week) { mutableStateOf(course.sessionNotes[week].orEmpty()) }
    val maxHeight = sheetMaxHeight()
    KomaSheet(open, onDismiss, glassBackdrop = backdrop) {
        Column(Modifier.fillMaxWidth().imePadding()) {
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight - 96.dp).verticalScroll(rememberScrollState())) {
                DrawerSheetHeader(KomaGlyph.Book, "课程备注",
                    "第 $week 周 · 周${listOf("一", "二", "三", "四", "五", "六", "日")[course.weekday - 1]} · $courseNameHint")
                Spacer(Modifier.height(16.dp))
                NoteField("课程简介", "同名课程共享，长期保留",
                    "例如：期末占比 40%，教材第 2 版", whole) { whole = it.take(200) }
                Spacer(Modifier.height(18.dp))
                NoteField("本节课备注", "只对第 $week 周生效，其他周次不受影响",
                    "例如：交作业、带计算器、复习第 3 章", session) { session = it.take(200) }
                if (error != null) KomaText(error, Modifier.padding(top = 10.dp), size = 12, color = KomaTheme.colors.danger)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DrawerSecondaryAction("取消", onDismiss, Modifier.weight(1f))
                KomaButton("保存", { onSave(whole.trim(), session.trim()) }, Modifier.weight(1f))
            }
        }
    }
}

private const val courseNameHint = "课程简介全周共享，本节课备注只影响这一周"

@Composable
private fun NoteField(label: String, hint: String, placeholder: String, text: String, onChange: (String) -> Unit) {
    val c = KomaTheme.colors
    KomaText(label, size = 14, weight = FontWeight.Medium)
    KomaText(hint, Modifier.padding(top = 3.dp), size = 11, color = c.ink3, lineHeight = 1.4f, maxLines = 2)
    Spacer(Modifier.height(8.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(c.surface2.copy(alpha = .65f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        BasicTextField(text, onChange,
            Modifier.fillMaxWidth().heightIn(min = 96.dp).semantics { contentDescription = label },
            textStyle = TextStyle(color = c.ink, fontSize = 15.sp, lineHeight = 22.sp, fontFamily = komaFontFamily()),
            cursorBrush = SolidColor(c.accent),
            decorationBox = { field ->
                Box {
                    if (text.isEmpty()) KomaText(placeholder, size = 15, color = c.ink5)
                    field()
                }
            })
        KomaText("${text.length}/200", Modifier.align(Alignment.End).padding(top = 6.dp), size = 10, color = c.ink5)
    }
}

/** Suspend scope: this occurrence or the whole course, with resume when already cancelled. */
@Composable
internal fun SuspendSheetContent(courseName: String, week: Int, suspendedThisWeek: Boolean,
    hasAnySuspended: Boolean, onWeek: () -> Unit, onAll: () -> Unit, onCancel: () -> Unit, error: String?) {
    val c = KomaTheme.colors
    DrawerSheetHeader(KomaGlyph.Calendar, if (suspendedThisWeek) "恢复课程" else "停课",
        "第 $week 周 · $courseName · 停课不会删除课程")
    Spacer(Modifier.height(14.dp))
    if (!suspendedThisWeek) {
        DrawerOptionRow(KomaGlyph.Calendar, "仅停第 $week 周", "只取消这一周的课，其他周次不变", onClick = onWeek)
        Spacer(Modifier.height(8.dp))
        DrawerOptionRow(KomaGlyph.Book, "全部周次停课", "这门课的所有周次都不再安排", onClick = onAll)
    } else {
        DrawerOptionRow(KomaGlyph.Calendar, "恢复第 $week 周", "重新启用这一周的课", onClick = onWeek)
        if (hasAnySuspended) {
            Spacer(Modifier.height(8.dp))
            DrawerOptionRow(KomaGlyph.Book, "恢复全部周次", "把所有停课周次一起恢复", onClick = onAll)
        }
    }
    if (error != null) KomaText(error, Modifier.padding(top = 10.dp), size = 12, color = c.danger)
    Spacer(Modifier.height(14.dp))
    DrawerSecondaryAction("取消", onCancel, Modifier.fillMaxWidth())
}

/** First delete step: whole course or this week's occurrence, exactly like the reference. */
@Composable
internal fun DeleteScopeSheet(open: Boolean, courseName: String, week: Int, canDeleteOccurrence: Boolean,
    detail: String, onDismiss: () -> Unit, onWhole: () -> Unit, onWeek: () -> Unit) {
    DrawerSheetHeader(KomaGlyph.Exit, "删除课程", "第 $week 周 · $courseName")
    Spacer(Modifier.height(14.dp))
    DrawerOptionRow(KomaGlyph.Exit, "删除本课", "$detail · 所有周次都会移除", onClick = onWhole)
    Spacer(Modifier.height(8.dp))
    DrawerOptionRow(KomaGlyph.Calendar, "删除本节",
        if (canDeleteOccurrence) "第 $week 周这一次 · 其他周次保留" else "这一周没有这门课",
        enabled = canDeleteOccurrence, onClick = onWeek)
    Spacer(Modifier.height(14.dp))
    DrawerSecondaryAction("取消", onDismiss, Modifier.fillMaxWidth())
}
