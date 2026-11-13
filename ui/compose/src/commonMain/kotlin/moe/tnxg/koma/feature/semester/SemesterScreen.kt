package moe.tnxg.koma.feature.semester

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.calendar.daysBetween
import moe.tnxg.koma.domain.calendar.formatMonthDay
import moe.tnxg.koma.domain.calendar.mondayOf
import moe.tnxg.koma.domain.calendar.today
import moe.tnxg.koma.domain.calendar.weekIndex
import moe.tnxg.koma.domain.model.Semester
import moe.tnxg.koma.feature.calendar.DateSheet
import moe.tnxg.koma.ui.component.button.KomaButton
import moe.tnxg.koma.ui.component.button.KomaTextButton
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
fun SemesterScreen(semester: Semester, confirmedDate: String, onConfirm: (String) -> Unit) {
    val colors = KomaTheme.colors
    val detected = remember(semester.startDate) { LocalDate.parse(semester.startDate) }
    var selected by remember(confirmedDate, detected) { mutableStateOf(confirmedDate.takeIf { it.isNotEmpty() }?.let(LocalDate::parse) ?: detected) }
    var calendar by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 34.dp)) {
            KomaText("设置开学时间", size = 26, weight = FontWeight.ExtraBold, letterSpacing = -.52f, lineHeight = 1.2f)
            Spacer(Modifier.height(6.dp))
            KomaText("第 1 周的周一，决定每天显示哪些课", size = 12.5f, weight = FontWeight.Medium, color = colors.ink4)
            Spacer(Modifier.height(22.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface)) {
                SemesterRow("开学", Modifier.clickable(remember { MutableInteractionSource() }, null) { calendar = true },
                    trailing = {
                        KomaIcon(KomaGlyph.Right, null, Modifier.size(14.dp), color = colors.ink5)
                    }) {
                    KomaText(selected.toString().replace('-', '/'), size = 14, weight = FontWeight.SemiBold)
                    KomaText("第 1 周 · ${formatMonthDay(selected)} 周一${if (selected == detected) " · 来自教务" else ""}",
                        Modifier.padding(top = 2.dp), 11.5f, FontWeight.Medium, colors.ink4)
                }
                Divider()
                SemesterRow("学期") { KomaText(semester.name, size = 14, weight = FontWeight.SemiBold) }
                Divider()
                SemesterRow("总周数") { KomaText("${semester.totalWeeks} 周", size = 14, weight = FontWeight.SemiBold) }
                Divider()
                val week = weekIndex(selected.toString(), today())
                val todayText = when {
                    week < 1 -> "距离开学 ${daysBetween(today(), selected)} 天"
                    week > semester.totalWeeks -> "学期已结束"
                    else -> "第 $week 周"
                }
                SemesterRow("今天") { KomaText(todayText, size = 14, weight = FontWeight.SemiBold) }
            }
            if (selected != detected) KomaTextButton("恢复教务日期（${formatMonthDay(detected)}）",
                { selected = mondayOf(detected) }, Modifier.padding(start = 2.dp, top = 16.dp), colors.ink3)
        }
        KomaButton("开始使用", { onConfirm(selected.toString()) },
            Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 22.dp))
    }
    DateSheet(calendar, selected, { selected = mondayOf(it) }, { calendar = false })
}

@Composable
private fun SemesterRow(
    label: String, modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        KomaText(label, Modifier.width(62.dp), 12.5f, FontWeight.Medium, KomaTheme.colors.ink4)
        Column(Modifier.weight(1f), content = content)
        trailing()
    }
}

@Composable
private fun Divider() { Box(Modifier.fillMaxWidth().height(1.dp).background(KomaTheme.colors.line2)) }
