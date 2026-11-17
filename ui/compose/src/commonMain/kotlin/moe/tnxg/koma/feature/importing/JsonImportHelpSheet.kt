package moe.tnxg.koma.feature.importing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.app.preferences.LocalAppPreferences
import moe.tnxg.koma.data.timetable.JSON_TIMETABLE_AI_PROMPT
import moe.tnxg.koma.ui.component.button.KomaButton
import moe.tnxg.koma.ui.component.button.KomaTextButton
import moe.tnxg.koma.ui.component.sheet.KomaSheet
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

/** Teaches the AI workflow in user language; the prompt itself stays collapsed behind a toggle. */
@Suppress("DEPRECATION")
@Composable
internal fun JsonHelpSheet(open: Boolean, onDismiss: () -> Unit) {
    val colors = KomaTheme.colors
    val clipboard = LocalClipboardManager.current
    val reduceMotion = LocalAppPreferences.current.reduceMotion
    var feedback by remember { mutableStateOf<String?>(null) }
    var showPrompt by remember { mutableStateOf(false) }

    fun copyPrompt() {
        feedback = try {
            clipboard.setText(AnnotatedString(JSON_TIMETABLE_AI_PROMPT))
            "提示词已复制，去发给 AI 吧"
        } catch (_: Exception) {
            "复制失败，请检查系统剪贴板权限"
        }
    }

    KomaSheet(open = open, onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            KomaText(
                text = "用 AI 识图导入",
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                size = 17,
                weight = FontWeight.Bold,
                letterSpacing = -0.4f,
            )
            KomaText(
                text = "不用手抄课表，让 AI 看图整理，再把结果粘贴回来。",
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                size = 12,
                color = colors.ink3,
            )
            Column(
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HelpStep("1", "复制下面的提示词")
                HelpStep("2", "把提示词和课表截图发给任意支持图片的 AI，如 ChatGPT、豆包、Kimi、通义千问")
                HelpStep("3", "把 AI 回复的内容粘贴回上一页，预览后导入")
            }
            KomaText(
                text = "截图里有姓名、学号时，发送前先遮住。",
                modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                size = 11.5f,
                color = colors.ink4,
            )
            KomaButton("复制提示词", onClick = ::copyPrompt, modifier = Modifier.padding(top = 14.dp))
            AnimatedVisibility(
                visible = feedback != null,
                enter = fadeIn(tween(160)) + expandVertically(tween(if (reduceMotion) 0 else 180)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(140)),
            ) {
                KomaText(
                    text = feedback.orEmpty(),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    size = 12,
                    color = colors.accent,
                )
            }
            KomaTextButton(
                text = if (showPrompt) "收起提示词" else "查看提示词",
                onClick = { showPrompt = !showPrompt },
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
                color = colors.ink3,
            )
            AnimatedVisibility(
                visible = showPrompt,
                enter = fadeIn(tween(160)) + expandVertically(tween(if (reduceMotion) 0 else 180)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(140)),
            ) {
                Box(
                    Modifier.fillMaxWidth().heightIn(max = 260.dp)
                        .background(colors.surface2, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    SelectionContainer {
                        KomaText(JSON_TIMETABLE_AI_PROMPT, size = 11, color = colors.ink2, lineHeight = 1.6f)
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpStep(number: String, text: String) {
    val colors = KomaTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        KomaText(number, Modifier.width(20.dp), size = 12,
            weight = FontWeight.SemiBold, color = colors.accent)
        KomaText(text, Modifier.weight(1f), size = 12.5f, color = colors.ink2)
    }
}
