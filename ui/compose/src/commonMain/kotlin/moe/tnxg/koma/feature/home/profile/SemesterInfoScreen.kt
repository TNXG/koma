package moe.tnxg.koma.feature.home.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.domain.calendar.today
import moe.tnxg.koma.domain.calendar.weekIndex
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.ui.component.list.SettingsDivider
import moe.tnxg.koma.ui.component.list.SettingsGroup
import moe.tnxg.koma.ui.component.list.SettingsNavRow
import moe.tnxg.koma.ui.component.page.SubpageScaffold
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

private fun schoolNameOf(schoolId: String): String = when (schoolId) {
    "xjvut" -> "新疆理工职业大学"
    else -> schoolId
}

@Composable
fun SemesterInfoScreen(
    snapshot: TimetableSnapshot,
    startDate: String,
    onBack: () -> Unit,
    onEditStart: () -> Unit,
) {
    val c = KomaTheme.colors
    val totalWeeks = snapshot.semester.totalWeeks.coerceAtLeast(1)
    val currentWeek = remember(startDate) {
        runCatching { weekIndex(startDate, today()) }.getOrNull()
    }
    val weekStatus = when {
        currentWeek == null -> "未知"
        currentWeek <= 0 -> "未开学"
        currentWeek > totalWeeks -> "已结课"
        else -> "第 $currentWeek 周"
    }
    val count = remember(snapshot) { snapshot.courses.map { it.name }.distinct().size }

    SubpageScaffold(title = "学期与课表", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface)
                .padding(20.dp),
        ) {
            KomaText(snapshot.semester.name, size = 22, weight = FontWeight.Bold, color = c.ink, letterSpacing = -0.8f)
            KomaText(
                text = schoolNameOf(snapshot.schoolId),
                modifier = Modifier.padding(top = 5.dp),
                size = 11,
                color = c.ink3,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp).height(4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                for (i in 1..totalWeeks) {
                    val segment = when {
                        currentWeek != null && i < currentWeek -> c.accent.copy(alpha = 0.65f)
                        currentWeek != null && i == currentWeek -> c.accent
                        else -> c.surface2
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(segment),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                KomaText(weekStatus, size = 10, color = c.ink3)
                KomaText("共 $totalWeeks 周", size = 10, color = c.ink3)
            }
        }

        SettingsGroup(modifier = Modifier.padding(top = 18.dp)) {
            SettingsNavRow(
                title = "开学日期",
                value = startDate.replace("-", "."),
                onClick = onEditStart,
            )
            SettingsDivider()
            SettingsNavRow(
                title = "学期周数",
                value = "$totalWeeks 周",
                tag = "导入",
                chevron = false,
            )
            SettingsDivider()
            SettingsNavRow(
                title = "课程数量",
                value = "$count 门",
                tag = "导入",
                chevron = false,
            )
        }
    }
}
