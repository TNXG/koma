package moe.tnxg.koma.app.state

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import moe.tnxg.koma.domain.course.CourseSyncConflictKind
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.weeks.weeksToString
import moe.tnxg.koma.ui.component.button.*
import moe.tnxg.koma.ui.component.sheet.KomaSheet
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
internal fun SyncConflictSheet(app: AppController) {
    val conflicts = app.timetable.conflicts
    val conflict = conflicts.firstOrNull()
    val c = KomaTheme.colors
    var error by remember(conflict?.id) { mutableStateOf<String?>(null) }
    KomaSheet(app.route == Route.Home && conflicts.isNotEmpty() && app.showSyncConflicts,
        { app.showSyncConflicts = false }) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState())
            .padding(top = 12.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            KomaText("课表同步冲突", size = 22, weight = FontWeight.Bold)
            KomaText("${conflicts.size} 项冲突待解决 · 本地手动调整已优先保留，未被覆盖。", size = 12, color = c.ink3)
            if (conflict != null) {
                if (conflict.kind == CourseSyncConflictKind.Overlap) {
                    app.timetable.snapshot?.courses?.filter { it.id in conflict.relatedCourseIds }?.forEach {
                        ConflictCourse("本机手动调整", it)
                    }
                } else ConflictCourse("本机课程", conflict.localCourse)
                ConflictCourse("教务课程", conflict.remoteCourse)
                KomaText(when (conflict.kind) {
                    CourseSyncConflictKind.RemoteDeleted -> "教务端已不再提供这门课程。采用教务结果将移除本地课程及调课记录。"
                    CourseSyncConflictKind.Overlap -> "教务课程与本地手动调整后的课程时间重叠。采用教务结果将对本地冲突周次做停课处理，保留备注及其他周次。"
                    else -> "教务排课已变更，与本地修改冲突。请选择保留哪一份版本。"
                }, size = 13, color = c.ink2)
                fun resolve(useRemote: Boolean) {
                    try { app.timetable.resolveConflict(conflict.id, useRemote); error = null }
                    catch (failure: Exception) { error = failure.message ?: "处理失败，请重试" }
                }
                KomaButton("保留本机手动调整", { resolve(false) })
                KomaButton(if (conflict.remoteCourse == null) "采用教务结果：移除课程" else "采用教务课程", { resolve(true) })
                error?.let { KomaText(it, size = 12, color = c.danger) }
            }
            KomaTextButton("稍后处理（教务账号中可继续）", { app.showSyncConflicts = false }, Modifier.padding(vertical = 12.dp))
        }
    }
}

@Composable
private fun ConflictCourse(label: String, course: Course?) {
    val c = KomaTheme.colors
    Column(Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        KomaText(label, size = 11, color = c.ink4)
        if (course == null) KomaText("课程已移除", size = 14, color = c.danger)
        else {
            KomaText(course.name, size = 16, weight = FontWeight.Bold)
            KomaText("周${listOf("一", "二", "三", "四", "五", "六", "日").getOrElse(course.weekday - 1) { "?" }} · 第 ${course.startPeriod}–${course.endPeriod} 节 · ${weeksToString(course.weeks)} 周",
                size = 12, color = c.ink3)
            KomaText(course.location.orEmpty().ifBlank { "未填写地点" }, size = 12, color = c.ink3)
            if (course.suspendedWeeks.isNotEmpty()) KomaText("停课：${weeksToString(course.suspendedWeeks)} 周", size = 12, color = c.danger)
            if (course.note.isNotBlank()) KomaText("备注：${course.note}", size = 12, color = c.ink2)
        }
    }
}
