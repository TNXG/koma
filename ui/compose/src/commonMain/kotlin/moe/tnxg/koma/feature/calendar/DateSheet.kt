package moe.tnxg.koma.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import moe.tnxg.koma.domain.calendar.addDays
import moe.tnxg.koma.domain.calendar.mondayOf
import moe.tnxg.koma.domain.calendar.today
import moe.tnxg.koma.ui.component.sheet.KomaSheet
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
fun DateSheet(
    open: Boolean,
    value: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    hint: String = "",
) {
    var month by remember { mutableStateOf(LocalDate(value.year, value.monthNumber, 1)) }
    LaunchedEffect(open) { if (open) month = LocalDate(value.year, value.monthNumber, 1) }
    val colors = KomaTheme.colors
    KomaSheet(open, onDismiss) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            MonthArrow(false) { month = month.plus(DatePeriod(months = -1)) }
            KomaText("${month.year}年${month.monthNumber}月", size = 15, weight = FontWeight.Bold, letterSpacing = -.15f)
            MonthArrow(true) { month = month.plus(DatePeriod(months = 1)) }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Box(Modifier.weight(1f).padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    KomaText(it, size = 11, weight = FontWeight.SemiBold, color = colors.ink5)
                }
            }
        }
        val start = mondayOf(month)
        repeat(6) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(7) { column ->
                    val day = addDays(start, row * 7 + column)
                    val selected = day == value
                    val selectedWeek = mondayOf(day) == value
                    val shape = RoundedCornerShape(if (selectedWeek && !selected) 0.dp else 13.dp)
                    val background = when { selected -> colors.accentSoft; selectedWeek -> colors.surface2; else -> colors.surface }
                    var style = Modifier.weight(1f).height(42.dp).background(background, shape)
                    if (selected) style = style.border(1.5.dp, colors.accent2, shape)
                    Box(style.clickable(remember { MutableInteractionSource() }, null) { onPick(day); onDismiss() },
                        contentAlignment = Alignment.Center) {
                        KomaText(day.dayOfMonth.toString(), size = 15,
                            weight = if (day.monthNumber == month.monthNumber) FontWeight.SemiBold else FontWeight.Medium,
                            color = when { selected -> colors.accent; day.monthNumber != month.monthNumber -> colors.ink5; else -> colors.ink })
                        if (day == today()) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                            .size(4.dp).background(colors.accent, CircleShape))
                    }
                }
            }
            if (row < 5) Spacer(Modifier.height(2.dp))
        }
        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            KomaText(hint.ifEmpty { "点任意一天，自动取那一周的周一" }, size = 12,
                weight = FontWeight.Medium, color = colors.ink4)
        }
    }
}

@Composable
private fun MonthArrow(forward: Boolean, onClick: () -> Unit) {
    val colors = KomaTheme.colors
    Box(Modifier.size(36.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        contentAlignment = Alignment.Center) {
        KomaIcon(if (forward) KomaGlyph.Right else KomaGlyph.Left,
            if (forward) "下个月" else "上个月", Modifier.size(16.dp), color = colors.ink2)
    }
}
