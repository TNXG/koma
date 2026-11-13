package moe.tnxg.koma.ui.component.list

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

/** One rounded surface holding related rows; separators live between rows inside it. */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(19.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(KomaTheme.colors.surface, shape),
        content = content,
    )
}

@Composable
fun SettingsSection(text: String, first: Boolean = false) {
    KomaText(
        text = text,
        modifier = Modifier.padding(
            start = 12.dp,
            top = if (first) 0.dp else 22.dp,
            bottom = 8.dp,
        ),
        size = 11,
        weight = FontWeight.SemiBold,
        color = KomaTheme.colors.ink3,
        letterSpacing = 0.5f,
    )
}

@Composable
fun SettingsDivider(indent: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 56.dp else 16.dp, end = 16.dp)
            .height(1.dp)
            .background(KomaTheme.colors.line2),
    )
}

/**
 * Tappable row: optional leading glyph, title, then either a value, a tag, or a trailing action
 * slot. Values carry the current state so a page never has to explain itself in body copy.
 */
@Composable
fun SettingsNavRow(
    title: String,
    glyph: KomaGlyph? = null,
    value: String? = null,
    tag: String? = null,
    tint: Color = KomaTheme.colors.ink,
    iconTint: Color = KomaTheme.colors.ink2,
    chevron: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val c = KomaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .then(
                if (onClick != null) {
                    Modifier
                        .semantics { role = Role.Button }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (glyph != null) {
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                KomaIcon(glyph, description = null, modifier = Modifier.size(21.dp), color = iconTint)
            }
        }
        KomaText(
            text = title,
            modifier = Modifier.weight(1f),
            size = 14,
            weight = FontWeight.SemiBold,
            color = tint,
            letterSpacing = -0.25f,
            maxLines = 1,
        )
        if (!value.isNullOrBlank()) {
            KomaText(value, size = 12.5f, weight = FontWeight.Medium, color = c.ink4, maxLines = 1)
        }
        if (!tag.isNullOrBlank()) SettingsTag(tag)
        if (onClick != null && chevron) {
            KomaIcon(KomaGlyph.Right, description = null, modifier = Modifier.size(15.dp), color = c.ink4)
        }
    }
}

@Composable
fun SettingsTag(text: String) {
    val c = KomaTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(c.surface2)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        KomaText(text, size = 10, color = c.ink3)
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit,
) {
    val c = KomaTheme.colors
    val trackColor by animateColorAsState(
        targetValue = if (!enabled) c.line2 else if (checked) c.accent else c.line,
        animationSpec = tween(180),
        label = "switchTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 0.dp,
        animationSpec = tween(180),
        label = "switchThumb",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .semantics { role = Role.Switch }
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onToggle(!checked) },
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KomaText(
            text = title,
            modifier = Modifier.weight(1f),
            size = 14,
            weight = FontWeight.SemiBold,
            color = if (enabled) c.ink else c.ink4,
            letterSpacing = -0.25f,
        )
        Box(
            modifier = Modifier
                .width(43.dp)
                .height(25.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(trackColor)
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(start = thumbOffset)
                    .size(19.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

/** Single-choice row: the selected option turns accent and carries a check on the right. */
@Composable
fun SettingsRadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    glyph: KomaGlyph? = null,
) {
    val c = KomaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .semantics { role = Role.RadioButton }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (glyph != null) {
            KomaIcon(
                glyph = glyph,
                description = null,
                modifier = Modifier.size(19.dp),
                color = if (selected) c.accent else c.ink2,
            )
        }
        KomaText(
            text = title,
            modifier = Modifier.weight(1f),
            size = 14,
            weight = FontWeight.SemiBold,
            color = if (selected) c.accent else c.ink,
            letterSpacing = -0.25f,
            maxLines = 1,
        )
        CheckMark(visible = selected, color = c.accent)
    }
}

@Composable
fun CheckMark(visible: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        if (!visible) return@Canvas
        val side = size.width
        val path = Path().apply {
            moveTo(side * 0.18f, side * 0.53f)
            lineTo(side * 0.42f, side * 0.76f)
            lineTo(side * 0.84f, side * 0.26f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = side * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
