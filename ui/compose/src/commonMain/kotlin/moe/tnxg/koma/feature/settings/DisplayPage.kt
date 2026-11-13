package moe.tnxg.koma.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.app.preferences.AppPreferences
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.app.preferences.ScheduleDensity
import moe.tnxg.koma.domain.calendar.today
import moe.tnxg.koma.domain.calendar.weekdayLabel
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.domain.visual.courseVisual
import moe.tnxg.koma.domain.week.WeekCourseGroup
import moe.tnxg.koma.feature.home.timeline.course.courseTone
import moe.tnxg.koma.ui.component.list.SettingsDivider
import moe.tnxg.koma.ui.component.list.SettingsGroup
import moe.tnxg.koma.ui.component.list.SettingsNavRow
import moe.tnxg.koma.ui.component.list.SettingsSection
import moe.tnxg.koma.ui.component.list.SettingsSwitchRow
import moe.tnxg.koma.ui.component.page.SubpageScaffold
import moe.tnxg.koma.ui.component.sheet.OptionSheet
import moe.tnxg.koma.ui.component.sheet.SheetOption
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
internal fun DisplayPage(
    store: PreferenceStore,
    snapshot: TimetableSnapshot,
    startDate: String,
    onBack: () -> Unit,
) {
    val c = KomaTheme.colors
    val prefs = store.value
    var densityPicker by remember { mutableStateOf(false) }
    val todayDate = remember { today() }
    val preview = remember(snapshot, startDate, todayDate, prefs.showWeekend, prefs.showOtherWeekCourses) {
        buildDisplayWeekPreview(
            snapshot = snapshot,
            startDate = startDate,
            todayDate = todayDate,
            showWeekend = prefs.showWeekend,
            showOtherWeekCourses = prefs.showOtherWeekCourses,
        )
    }

    SubpageScaffold(
        title = "课表显示",
        onBack = onBack,
        error = store.error,
        onDismissError = store::clearError,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WeekDisplayPreview(preview = preview, todayDate = todayDate, prefs = prefs)
        }

        SettingsSection("显示内容")
        SettingsGroup {
            SettingsSwitchRow(
                title = "显示周末",
                checked = prefs.showWeekend,
                onToggle = { checked -> store.update(prefs.copy(showWeekend = checked)) },
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = "显示非本周课程",
                checked = prefs.showOtherWeekCourses,
                onToggle = { checked -> store.update(prefs.copy(showOtherWeekCourses = checked)) },
            )
        }

        SettingsSection("排版")
        SettingsGroup {
            SettingsNavRow(
                title = "显示密度",
                value = prefs.density.label(),
                onClick = { densityPicker = true },
            )
        }
    }

    OptionSheet(
        open = densityPicker,
        title = "显示密度",
        options = listOf(ScheduleDensity.Compact, ScheduleDensity.Standard, ScheduleDensity.Relaxed).map { density ->
            SheetOption(
                title = density.label(),
                selected = prefs.density == density,
                onClick = { store.update(prefs.copy(density = density)) },
            )
        },
        onDismiss = { densityPicker = false },
        error = store.error,
    )
}

@Composable
private fun WeekDisplayPreview(
    preview: DisplayWeekPreview,
    todayDate: LocalDate,
    prefs: AppPreferences,
) {
    val c = KomaTheme.colors
    val rowHeight = when (prefs.density) {
        ScheduleDensity.Compact -> 15.dp
        ScheduleDensity.Standard -> 20.dp
        ScheduleDensity.Relaxed -> 25.dp
    }
    val visibleDayList = preview.schedule.days.take(preview.visibleDays)
    val gridHeight = rowHeight * preview.periodCount
    val hasAnyBlock = visibleDayList.any { it.groups.isNotEmpty() } ||
        preview.offWeekByWeekday.values.any { it.isNotEmpty() }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.Center) {
                KomaText("${preview.week}", size = 9, weight = FontWeight.SemiBold, color = c.ink4)
            }
            visibleDayList.forEach { day ->
                val isToday = day.date == todayDate
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isToday) c.accentSoft else Color.Transparent)
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    KomaText(
                        text = weekdayLabel(day.date),
                        size = 10.5f,
                        weight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isToday) c.accent else c.ink2,
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Column(modifier = Modifier.width(20.dp)) {
                    repeat(preview.periodCount) { idx ->
                        Box(
                            modifier = Modifier.height(rowHeight).fillMaxWidth(),
                            contentAlignment = Alignment.TopEnd,
                        ) {
                            KomaText(
                                text = "${idx + 1}".padStart(2, '0'),
                                modifier = Modifier.padding(end = 3.dp, top = 1.dp),
                                size = 8,
                                weight = FontWeight.Medium,
                                color = c.ink4,
                            )
                        }
                    }
                }

                visibleDayList.forEachIndexed { dayIndex, day ->
                    val weekday = dayIndex + 1
                    val offWeekGroups = preview.offWeekByWeekday[weekday].orEmpty()
                    Box(modifier = Modifier.weight(1f).height(gridHeight)) {
                        day.groups.forEach { group ->
                            PreviewCourseBlock(
                                group = group,
                                rowHeight = rowHeight,
                                maxPeriod = preview.periodCount,
                                offWeek = false,
                                prefs = prefs,
                            )
                        }
                        offWeekGroups.forEach { group ->
                            PreviewCourseBlock(
                                group = group,
                                rowHeight = rowHeight,
                                maxPeriod = preview.periodCount,
                                offWeek = true,
                                prefs = prefs,
                            )
                        }
                    }
                }
            }

            if (!hasAnyBlock) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(gridHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    KomaText(text = "本周暂无排课", size = 12, weight = FontWeight.Medium, color = c.ink4)
                }
            }
        }
    }
}

@Composable
private fun PreviewCourseBlock(
    group: WeekCourseGroup,
    rowHeight: Dp,
    maxPeriod: Int,
    offWeek: Boolean,
    prefs: AppPreferences,
) {
    if (group.startPeriod > maxPeriod) return
    val clampedEnd = group.endPeriod.coerceAtMost(maxPeriod)
    val span = (clampedEnd - group.startPeriod + 1).coerceAtLeast(1)
    val blockHeight = (rowHeight * span - 3.dp).coerceAtLeast(12.dp)
    val c = KomaTheme.colors
    val course = group.courses.first()
    val tone = courseTone(courseVisual(course.name).tone)
    val fill = when {
        offWeek -> c.surface2.copy(alpha = 0.72f)
        prefs.colorCourses -> lerp(c.surface, tone, 0.14f)
        else -> c.surface2
    }
    val textColor = when {
        offWeek -> c.ink4
        prefs.colorCourses -> lerp(tone, c.ink, 0.2f)
        else -> c.ink2
    }
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = Modifier
            .offset(y = rowHeight * (group.startPeriod - 1) + 1.dp)
            .fillMaxWidth()
            .height(blockHeight)
            .clip(shape)
            .background(fill, shape)
            .then(
                if (offWeek) Modifier.border(1.dp, c.line, shape)
                else if (prefs.colorCourses) Modifier.border(1.dp, tone.copy(alpha = 0.25f), shape)
                else Modifier.border(1.dp, c.line, shape),
            )
            .padding(horizontal = 3.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        KomaText(
            text = course.name,
            size = if (prefs.density == ScheduleDensity.Compact) 7.5f else 8.5f,
            lineHeight = 1.15f,
            weight = FontWeight.SemiBold,
            color = textColor,
            maxLines = if (span >= 2) 2 else 1,
        )
        val room = course.location?.takeIf { it.isNotBlank() }
        if (prefs.showRoom && room != null && blockHeight >= 32.dp) {
            KomaText(
                text = room,
                size = 7f,
                lineHeight = 1.1f,
                color = textColor.copy(alpha = 0.7f),
                maxLines = 1,
            )
        }
    }
}
