package moe.tnxg.koma.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.app.preferences.AccentPreference
import moe.tnxg.koma.app.preferences.ThemePreference
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.DarkColors
import moe.tnxg.koma.ui.theme.KomaColors
import moe.tnxg.koma.ui.theme.KomaTheme
import moe.tnxg.koma.ui.theme.LightColors

@Composable
internal fun ThemeModePicker(
    selected: ThemePreference,
    accent: AccentPreference,
    onSelect: (ThemePreference) -> Unit,
) {
    val c = KomaTheme.colors
    val systemDark = isSystemInDarkTheme()
    Column(Modifier.padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KomaText("主题模式", size = 12, color = c.ink3, modifier = Modifier.weight(1f))
            KomaText(
                "首页样式", size = 11, color = c.ink3,
                modifier = Modifier.clip(RoundedCornerShape(9.dp))
                    .background(c.surface2).padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (theme in listOf(ThemePreference.Light, ThemePreference.Dark, ThemePreference.System)) {
                val dark = theme == ThemePreference.Dark || theme == ThemePreference.System && systemDark
                val chosen = selected == theme
                val shape = RoundedCornerShape(15.dp)
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                        .selectable(selected = chosen, role = Role.RadioButton, onClick = { onSelect(theme) })
                        .padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(0.58f)
                            .clip(shape)
                            .border(2.dp, if (chosen) c.accent else Color.Transparent, shape)
                            .padding(3.dp),
                    ) {
                        ThemeSkeleton(
                            colors = if (dark) DarkColors else LightColors,
                            accent = accent,
                            split = theme == ThemePreference.System,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    KomaText(
                        theme.label(), size = 13, maxLines = 1,
                        weight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (chosen) c.accent else c.ink3,
                    )
                    Box(Modifier.height(22.dp), contentAlignment = Alignment.Center) {
                        if (chosen) KomaText("✓", size = 14, color = c.accent)
                    }
                }
            }
        }
    }
}

/**
 * Static placeholders, not a loading state or miniature copies of private course data.
 * Following the system draws both palettes along a diagonal seam through the centre.
 */
@Composable
private fun ThemeSkeleton(
    colors: KomaColors,
    accent: AccentPreference,
    split: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.fillMaxWidth().aspectRatio(0.58f)) {
        if (!split) {
            drawSkeleton(colors, accentColorOf(accent, colors == DarkColors))
            return@Canvas
        }
        val seam = Path().apply {
            moveTo(size.width * 0.78f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(size.width * 0.22f, size.height)
            close()
        }
        drawSkeleton(LightColors, accentColorOf(accent, false))
        clipPath(seam) { drawSkeleton(DarkColors, accentColorOf(accent, true)) }
    }
}

private fun DrawScope.drawSkeleton(colors: KomaColors, accent: Color) {
    drawRect(colors.bg)
    val w = size.width
    val h = size.height
    fun block(x: Float, y: Float, width: Float, height: Float, color: Color, radius: Float = 0.025f) {
        drawRoundRect(color, Offset(w * x, h * y), Size(w * width, h * height), CornerRadius(w * radius))
    }
    block(0.08f, 0.07f, 0.43f, 0.025f, colors.ink3.copy(alpha = 0.5f))
    block(0.08f, 0.115f, 0.29f, 0.012f, colors.ink5.copy(alpha = 0.65f))
    drawLine(colors.line, Offset(w * 0.15f, h * 0.21f), Offset(w * 0.15f, h * 0.59f), w * 0.012f)
    for (index in 0..1) {
        val top = 0.2f + index * 0.21f
        val tone = if (index == 0) accent else colors.toneRose
        block(0.05f, top + 0.02f, 0.06f, 0.012f, colors.ink5)
        drawCircle(tone.copy(alpha = 0.7f), w * 0.017f, Offset(w * 0.15f, h * (top + 0.032f)))
        block(0.22f, top, 0.7f, 0.17f, tone.copy(alpha = 0.12f), 0.05f)
        block(0.28f, top + 0.033f, 0.43f, 0.018f, tone.copy(alpha = 0.4f))
        block(0.28f, top + 0.072f, 0.31f, 0.012f, colors.ink5.copy(alpha = 0.6f))
        block(0.28f, top + 0.104f, 0.22f, 0.012f, colors.ink5.copy(alpha = 0.4f))
    }
    block(0.08f, 0.64f, 0.84f, 0.14f, colors.surface, 0.05f)
    block(0.14f, 0.676f, 0.32f, 0.018f, colors.ink5.copy(alpha = 0.7f))
    block(0.14f, 0.717f, 0.5f, 0.012f, colors.ink5.copy(alpha = 0.4f))
    block(0.22f, 0.86f, 0.56f, 0.09f, colors.surface2, 0.1f)
    block(0.24f, 0.87f, 0.17f, 0.07f, accent.copy(alpha = 0.15f), 0.08f)
    for (index in 0..2) {
        drawCircle(if (index == 0) accent else colors.ink5, w * 0.025f, Offset(w * (0.325f + index * 0.175f), h * 0.905f))
    }
}
