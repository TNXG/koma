package moe.tnxg.koma.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.app.preferences.AppPreferences
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.domain.calendar.formatTime
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.domain.visual.courseVisual
import moe.tnxg.koma.feature.home.timeline.course.courseIcon
import moe.tnxg.koma.feature.home.timeline.course.courseTone
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon
import moe.tnxg.koma.ui.component.list.SettingsDivider
import moe.tnxg.koma.ui.component.list.SettingsGroup
import moe.tnxg.koma.ui.component.list.SettingsSection
import moe.tnxg.koma.ui.component.list.SettingsSwitchRow
import moe.tnxg.koma.ui.component.page.SubpageScaffold
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
internal fun CardsPage(
    store: PreferenceStore,
    snapshot: TimetableSnapshot,
    onBack: () -> Unit,
) {
    val c = KomaTheme.colors
    val prefs = store.value
    val sampleCourse = remember(snapshot) { pickCardPreviewCourse(snapshot) }

    SubpageScaffold(
        title = "课程卡片",
        onBack = onBack,
        error = store.error,
        onDismissError = store::clearError,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface)
                .padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.bg)
                    .padding(12.dp),
            ) {
                CardPreviewItem(snapshot = snapshot, course = sampleCourse, prefs = prefs)
            }
        }

        SettingsSection("卡片内容")
        SettingsGroup {
            SettingsSwitchRow(
                title = "显示上课地点",
                checked = prefs.showRoom,
                onToggle = { checked -> store.update(prefs.copy(showRoom = checked)) },
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = "显示授课教师",
                checked = prefs.showTeacher,
                onToggle = { checked -> store.update(prefs.copy(showTeacher = checked)) },
            )
        }

        SettingsSection("视觉样式")
        SettingsGroup {
            SettingsSwitchRow(
                title = "课程彩色标识",
                checked = prefs.colorCourses,
                onToggle = { checked -> store.update(prefs.copy(colorCourses = checked)) },
            )
        }
    }
}

@Composable
private fun CardPreviewItem(
    snapshot: TimetableSnapshot,
    course: Course?,
    prefs: AppPreferences,
) {
    val c = KomaTheme.colors
    if (course == null) {
        KomaText("暂无课程可用于预览", size = 12.5f, color = c.ink4)
        return
    }
    val visual = courseVisual(course.name)
    val tone = if (prefs.colorCourses) courseTone(visual.tone) else c.ink3
    val startMin = snapshot.timeGrid.find { it.index == course.startPeriod }?.start
    val endMin = snapshot.timeGrid.find { it.index == course.endPeriod }?.end
    val cardShape = RoundedCornerShape(16.dp)
    val fill = if (prefs.colorCourses) {
        Brush.linearGradient(listOf(lerp(c.surface, tone, 0.09f), lerp(c.surface, tone, 0.03f)))
    } else {
        Brush.linearGradient(listOf(c.surface, c.surface))
    }

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(
            modifier = Modifier.width(44.dp).fillMaxHeight().padding(end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            KomaText(startMin?.let(::formatTime).orEmpty(), size = 11, lineHeight = 1f, weight = FontWeight.SemiBold, color = c.ink3)
            val periodText = if (course.startPeriod == course.endPeriod) "${course.startPeriod} 节"
            else "${course.startPeriod}–${course.endPeriod} 节"
            KomaText(periodText, size = 10, lineHeight = 1f, weight = FontWeight.SemiBold, color = c.ink5)
            KomaText(endMin?.let(::formatTime).orEmpty(), size = 11, lineHeight = 1f, weight = FontWeight.SemiBold, color = c.ink3)
        }

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(fill, cardShape)
                    .then(if (!prefs.colorCourses) Modifier.border(1.dp, c.line, cardShape) else Modifier)
                    .drawWithContent {
                        drawContent()
                        drawCircle(c.bg, 6.dp.toPx(), Offset(0f, size.height / 2f))
                        drawCircle(c.bg, 6.dp.toPx(), Offset(size.width, size.height / 2f))
                    }
                    .padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            ) {
                KomaText(
                    text = course.name,
                    modifier = Modifier.padding(end = 26.dp),
                    size = 15.5f,
                    lineHeight = 1.28f,
                    weight = FontWeight.Bold,
                    color = c.ink,
                )
                val room = course.location?.takeIf { it.isNotBlank() }
                if (prefs.showRoom && room != null) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp, end = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        KomaIcon(
                            glyph = KomaGlyph.Location,
                            description = null,
                            modifier = Modifier.size(13.dp),
                            color = if (prefs.colorCourses) lerp(c.ink4, tone, 0.55f) else c.ink4,
                        )
                        KomaText(room, size = 12.5f, weight = FontWeight.Medium, color = c.ink3, maxLines = 1)
                    }
                }
                val teacher = course.teacher?.takeIf { it.isNotBlank() }
                if (prefs.showTeacher && teacher != null) {
                    Row(
                        modifier = Modifier.padding(top = 3.dp, end = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        KomaIcon(
                            glyph = KomaGlyph.Person,
                            description = null,
                            modifier = Modifier.size(13.dp),
                            color = if (prefs.colorCourses) lerp(c.ink4, tone, 0.55f) else c.ink4,
                        )
                        KomaText(teacher, size = 12.5f, weight = FontWeight.Medium, color = c.ink3, maxLines = 1)
                    }
                }
            }
            KomaIcon(
                glyph = courseIcon(visual.icon),
                description = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .size(22.dp)
                    .graphicsLayer { rotationZ = -4f },
                color = if (prefs.colorCourses) tone.copy(alpha = 0.72f) else c.ink4,
            )
        }
    }
}
