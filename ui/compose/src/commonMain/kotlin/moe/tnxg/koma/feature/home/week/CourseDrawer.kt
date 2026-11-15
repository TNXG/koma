package moe.tnxg.koma.feature.home.week

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.app.state.LocalAppController
import moe.tnxg.koma.domain.calendar.formatMonthDay
import moe.tnxg.koma.domain.calendar.weekIndex
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.sheet.ConfirmSheet
import moe.tnxg.koma.ui.component.sheet.KomaSheet
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme
import top.yukonga.miuix.kmp.blur.Backdrop

private enum class CourseStage { Detail, Note, Suspend, Reschedule }

/**
 * One sheet per stage, all sampling the same weekly backdrop: only the topmost sheet is visible,
 * and cancelling an editor returns to the detail sheet instead of closing the whole drawer.
 */
@Composable
internal fun CourseDrawer(courses: List<Course>, date: LocalDate, backdrop: Backdrop, onDismiss: () -> Unit) {
    val session = LocalAppController.current.timetable
    val snapshot = session.snapshot ?: return
    var displayedCourses by remember { mutableStateOf(courses) }
    var displayedDate by remember { mutableStateOf(date) }
    if (courses.isNotEmpty()) { displayedCourses = courses; displayedDate = date }
    val ids = remember(displayedCourses) { displayedCourses.map(Course::id) }
    var selectedId by remember(ids, displayedDate) { mutableStateOf(displayedCourses.firstOrNull()?.id) }
    val course = snapshot.courses.firstOrNull { it.id == selectedId } ?: displayedCourses.firstOrNull()
    var stage by remember(selectedId, displayedDate) { mutableStateOf(CourseStage.Detail) }
    var error by remember(stage, selectedId) { mutableStateOf<String?>(null) }
    var deleteScope by remember(selectedId) { mutableStateOf(false) }
    var deleteWholeConfirm by remember(selectedId) { mutableStateOf<Boolean?>(null) }
    val week = weekIndex(session.startDate, displayedDate)
    val window = LocalWindowInfo.current
    val maxHeight = with(LocalDensity.current) { window.containerSize.height.toDp() * .8f }

    fun run(action: () -> Unit, closeDrawer: Boolean = false) {
        try {
            action()
            error = null
            if (closeDrawer) onDismiss() else stage = CourseStage.Detail
        } catch (failure: Exception) {
            error = failure.message ?: "保存失败，请重试"
        }
    }

    KomaSheet(courses.isNotEmpty() && stage == CourseStage.Detail,
        { if (stage == CourseStage.Detail) onDismiss() else stage = CourseStage.Detail },
        glassBackdrop = backdrop) {
        if (course == null) return@KomaSheet
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight - 48.dp).verticalScroll(rememberScrollState())
            .padding(top = 14.dp, bottom = 8.dp)) {
            CourseDrawerDetails(course, displayedCourses, displayedDate, week, snapshot.timeGrid,
                { selectedId = it }, { stage = CourseStage.Note },
                { stage = CourseStage.Reschedule }, { stage = CourseStage.Suspend },
                { deleteScope = true })
            error?.let {
                KomaText(it, Modifier.padding(top = 10.dp), size = 12, color = KomaTheme.colors.danger)
            }
        }
    }

    if (course == null) return
    val detail = "第 ${course.startPeriod}–${course.endPeriod} 节 · ${formatMonthDay(displayedDate)}"
    if (deleteScope) {
        KomaSheet(true, { deleteScope = false }, glassBackdrop = backdrop) {
            DeleteScopeSheet(open = true, courseName = course.name, week = week,
                canDeleteOccurrence = week in course.weeks, detail = detail,
                onDismiss = { deleteScope = false },
                onWhole = { deleteScope = false; deleteWholeConfirm = true },
                onWeek = { deleteScope = false; deleteWholeConfirm = false })
        }
    }
    val confirming = deleteWholeConfirm
    ConfirmSheet(open = confirming != null, icon = KomaGlyph.Exit,
        title = if (confirming == true) "删除本课？" else "删除本节？",
        message = if (confirming == true) "名为「${course.name}」的课程会从本机课表中移除，包括手动调课记录。"
        else "只移除第 $week 周的这一次课，其他周次与课程备注保留。",
        details = listOf("本机操作，不会修改教务系统", "之后同步会忽略这次删除，不再自动恢复"),
        confirmLabel = "删除", cancelLabel = "取消", destructive = true,
        onConfirm = {
            val whole = confirming == true
            deleteWholeConfirm = null
            run({ session.delete(course.id, if (whole) null else week) }, closeDrawer = true)
        },
        onDismiss = { deleteWholeConfirm = null })
    CourseNoteSheet(stage == CourseStage.Note, course, week, backdrop, error,
        onDismiss = { stage = CourseStage.Detail }) { note, sessionNote ->
        run({ session.updateNotes(course.id, note, week, sessionNote) })
    }
    KomaSheet(stage == CourseStage.Suspend, { stage = CourseStage.Detail }, glassBackdrop = backdrop) {
        val suspended = week in course.suspendedWeeks
        SuspendSheetContent(course.name, week, suspended, course.suspendedWeeks.isNotEmpty(),
            { run({ session.setSuspended(course.id, week, !suspended) }) },
            { run({ session.setSuspended(course.id, week, !suspended, allWeeks = true) }) },
            { stage = CourseStage.Detail }, error)
    }
    CourseRescheduleSheet(stage == CourseStage.Reschedule, course, week, snapshot, backdrop, error,
        onDismiss = { stage = CourseStage.Detail }) { targetWeek, weekday, start, end, location ->
        run({ session.reschedule(course.id, week, targetWeek, weekday, start, end, location) }, closeDrawer = true)
    }
}
