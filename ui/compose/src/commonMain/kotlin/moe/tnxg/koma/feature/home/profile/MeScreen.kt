package moe.tnxg.koma.feature.home.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.domain.calendar.formatMonthDay
import moe.tnxg.koma.domain.calendar.today
import moe.tnxg.koma.domain.calendar.weekIndex
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon
import moe.tnxg.koma.ui.component.list.SettingsDivider
import moe.tnxg.koma.ui.component.list.SettingsGroup
import moe.tnxg.koma.ui.component.list.SettingsNavRow
import moe.tnxg.koma.ui.component.media.ProfileAvatar
import moe.tnxg.koma.ui.component.media.ProfileCover
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.component.veil.EdgeVeil
import moe.tnxg.koma.ui.component.veil.VeilEdge
import moe.tnxg.koma.ui.theme.KomaTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

@Composable
fun MeScreen(
    snapshot: TimetableSnapshot,
    startDate: String,
    store: PreferenceStore,
    onProfile: () -> Unit,
    onSemester: () -> Unit,
    onAccount: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    val c = KomaTheme.colors
    val identityShadow = remember { Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 1f), 8f) }
    val displayName = store.profileName.ifBlank {
        snapshot.studentName?.takeIf(String::isNotBlank) ?: "同学"
    }
    val week = weekIndex(startDate, today())
    val totalWeeks = snapshot.semester.totalWeeks.coerceAtLeast(1)
    val count = remember(snapshot) { snapshot.courses.map { it.name }.distinct().size }
    val formattedStart = remember(startDate) {
        runCatching { formatMonthDay(LocalDate.parse(startDate)) }.getOrDefault(startDate)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        val heroHeight = maxHeight
        val coverBackdrop = rememberLayerBackdrop { drawRect(c.bg); drawContent() }

        // The cover stays put as the page background; only the content slides over it.
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().layerBackdrop(coverBackdrop)) {
                ProfileCover(
                    photoPath = store.coverPhoto,
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.TopCenter,
                )
            }
            // Home's top veil mirrored: the photo frosts into the page background toward the bottom.
            EdgeVeil(
                source = coverBackdrop.graphicsLayer,
                opacity = { 1f },
                height = heroHeight.value * 0.72f,
                edge = VeilEdge.Bottom,
                sourceHeight = heroHeight.value,
                strength = 1.8f,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.34f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
            KomaText(
                text = "我的",
                modifier = Modifier.statusBarsPadding().padding(start = 22.dp, top = 22.dp),
                size = 29,
                weight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = -1f,
            )
        }

        val viewportPx = with(LocalDensity.current) { heroHeight.toPx() }
        val scroll = rememberScrollState()
        var contentPx by remember { mutableIntStateOf(0) }

        // Only overflow scrolls; a page that already fits stays put.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll, enabled = contentPx > viewportPx),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 104.dp)
                    .onGloballyPositioned { contentPx = it.size.height },
            ) {
                Spacer(Modifier.height(heroHeight * 0.26f))

                Box(Modifier.padding(top = 24.dp, bottom = 22.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ProfileAvatar(
                            seed = store.avatarSeed,
                            photoPath = store.avatarPhoto,
                            modifier = Modifier.size(64.dp).border(3.dp, c.bg, CircleShape),
                        )
                        Column(Modifier.weight(1f)) {
                            KomaText(displayName, size = 21, weight = FontWeight.ExtraBold, color = Color.White,
                                letterSpacing = -0.6f, maxLines = 1, shadow = identityShadow)
                            KomaText("新疆理工职业大学", Modifier.padding(top = 4.dp), size = 11,
                                color = Color.White.copy(alpha = 0.8f), shadow = identityShadow)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    SemesterSummaryCard(
                        modifier = Modifier
                            .semantics { role = Role.Button }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onSemester,
                            ),
                        semesterName = snapshot.semester.name,
                        week = week,
                        totalWeeks = totalWeeks,
                        courseCount = count,
                        formattedStart = formattedStart,
                    )

                    SettingsGroup(modifier = Modifier.padding(top = 16.dp)) {
                        SettingsNavRow(title = "个人资料", glyph = KomaGlyph.Person, onClick = onProfile)
                        SettingsDivider(indent = true)
                        SettingsNavRow(title = "教务账号", glyph = KomaGlyph.Bank, onClick = onAccount)
                        SettingsDivider(indent = true)
                        SettingsNavRow(title = "设置", glyph = KomaGlyph.Settings, onClick = onSettings)
                        SettingsDivider(indent = true)
                        SettingsNavRow(title = "关于 Koma", glyph = KomaGlyph.Book, onClick = onAbout)
                    }
                }
            }
        }
    }
}

@Composable
private fun SemesterSummaryCard(
    modifier: Modifier,
    semesterName: String,
    week: Int,
    totalWeeks: Int,
    courseCount: Int,
    formattedStart: String,
) {
    val c = KomaTheme.colors
    val shape = RoundedCornerShape(19.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface, shape)
            .padding(start = 18.dp, top = 17.dp, end = 18.dp, bottom = 15.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KomaText(semesterName, size = 11, weight = FontWeight.Medium, color = c.ink2)
            KomaIcon(KomaGlyph.Right, description = null, modifier = Modifier.size(15.dp), color = c.ink4)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                when {
                    week < 1 -> KomaText("未开学", size = 24, weight = FontWeight.Bold, color = c.ink, letterSpacing = -1f)
                    week > totalWeeks -> KomaText("已结课", size = 24, weight = FontWeight.Bold, color = c.ink, letterSpacing = -1f)
                    else -> {
                        KomaText("第", size = 11, color = c.ink3, modifier = Modifier.padding(bottom = 4.dp))
                        KomaText("$week", size = 34, weight = FontWeight.Bold, color = c.ink, letterSpacing = -1.8f)
                        KomaText("周 / 共 $totalWeeks 周", size = 11, color = c.ink3, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                KomaText("$courseCount", size = 18, weight = FontWeight.Bold, color = c.ink)
                KomaText("门课程", size = 11, color = c.ink2, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 17.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(c.surface2),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((week.toFloat() / totalWeeks).coerceIn(0f, 1f))
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c.accent.copy(alpha = 0.75f)),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            KomaText("学期进度", size = 10, color = c.ink3)
            KomaText("$formattedStart 开学", size = 10, color = c.ink3)
        }
    }
}
