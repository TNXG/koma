package moe.tnxg.koma.ui.component.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon
import moe.tnxg.koma.ui.component.list.CheckMark
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

data class SheetOption(
    val title: String,
    val selected: Boolean = false,
    val tint: Color? = null,
    val swatch: Color? = null,
    val onClick: () -> Unit,
)

/** Bottom sheet listing the one choice a row opens; picking an option dismisses it. */
@Composable
fun OptionSheet(
    open: Boolean,
    title: String,
    options: List<SheetOption>,
    onDismiss: () -> Unit,
    error: String? = null,
) {
    val c = KomaTheme.colors
    KomaSheet(open = open, onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().widthIn(max = 430.dp)) {
            KomaText(
                text = title,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 10.dp),
                size = 17,
                weight = FontWeight.Bold,
                color = c.ink,
                letterSpacing = -0.4f,
            )
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .semantics { role = Role.RadioButton }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                option.onClick()
                                onDismiss()
                            },
                        )
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (option.swatch != null) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(option.swatch),
                        )
                    }
                    KomaText(
                        text = option.title,
                        modifier = Modifier.weight(1f),
                        size = 15,
                        weight = FontWeight.Medium,
                        color = option.tint ?: c.ink,
                    )
                    CheckMark(visible = option.selected, color = c.accent)
                }
            }
            if (!error.isNullOrBlank()) {
                KomaText(
                    text = error,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    size = 11,
                    color = c.danger,
                )
            }
        }
    }
}

/** Destructive confirmation with an explicit list of what changes. */
@Composable
fun ConfirmSheet(
    open: Boolean,
    icon: KomaGlyph,
    title: String,
    message: String,
    details: List<String>,
    confirmLabel: String,
    cancelLabel: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    error: String? = null,
) {
    val c = KomaTheme.colors
    val accent = if (destructive) c.danger else c.accent
    KomaSheet(open = open, onDismiss = onDismiss, horizontalPadding = 24.dp) {
        Column(modifier = Modifier.fillMaxWidth().widthIn(max = 430.dp)) {
            Box(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(accent.copy(alpha = 0.08f))
                    .padding(11.dp),
                contentAlignment = Alignment.Center,
            ) {
                KomaIcon(glyph = icon, description = null, modifier = Modifier.size(22.dp), color = accent)
            }
            KomaText(title, size = 21, weight = FontWeight.Bold, color = c.ink, letterSpacing = -0.6f, lineHeight = 1.45f)
            KomaText(
                text = message,
                modifier = Modifier.padding(top = 10.dp),
                size = 12,
                color = c.ink2,
                lineHeight = 1.8f,
            )
            if (details.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surface)
                        .padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    details.forEach { detail ->
                        KomaText(detail, size = 11, color = c.ink2)
                    }
                }
            }
            if (!error.isNullOrBlank()) {
                KomaText(error, modifier = Modifier.padding(top = 10.dp), size = 11, color = c.danger)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surface2)
                        .semantics { role = Role.Button }
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    KomaText(cancelLabel, size = 13, weight = FontWeight.SemiBold, color = c.ink2)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent)
                        .semantics { role = Role.Button }
                        .clickable(onClick = onConfirm)
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    KomaText(confirmLabel, size = 13, weight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}
