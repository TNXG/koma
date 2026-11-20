package moe.tnxg.koma.feature.importing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.tnxg.koma.app.preferences.LocalAppPreferences
import moe.tnxg.koma.data.timetable.parseTimetableJson
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.ui.component.button.KomaButton
import moe.tnxg.koma.ui.component.button.KomaTextButton
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.list.SettingsGroup
import moe.tnxg.koma.ui.component.list.SettingsNavRow
import moe.tnxg.koma.ui.component.page.SubpageScaffold
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.component.text.komaFontFamily
import moe.tnxg.koma.ui.platform.rememberJsonFilePicker
import moe.tnxg.koma.ui.theme.KomaTheme

/** Critically damped (Apple response ≈ 0.32 s): crisp, no overshoot, startable from any frame. */
private fun <T> importMotion(reduceMotion: Boolean): FiniteAnimationSpec<T> =
    if (reduceMotion) tween(180)
    else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f)

/**
 * One step at a time: paste or pick a file, parse, then review the semester summary and confirm.
 * Parsing never touches the stored timetable; only the confirm action replaces it.
 */
@Suppress("DEPRECATION")
@Composable
fun JsonImportScreen(onBack: () -> Unit, onImport: (TimetableSnapshot) -> Unit) {
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalAppPreferences.current.reduceMotion
    var raw by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    var preview by remember { mutableStateOf<TimetableSnapshot?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var parsing by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) }

    fun updateInput(value: String) {
        revision++
        preview = null
        error = null
        raw = value
    }

    val openFile = rememberJsonFilePicker { result ->
        result.fold(
            onSuccess = ::updateInput,
            onFailure = {
                revision++
                preview = null
                error = it.message ?: "文件读取失败，请重新选择，或直接粘贴 JSON。"
            },
        )
    }

    SubpageScaffold(
        title = "导入离线课表",
        onBack = { if (preview != null) preview = null else onBack() },
    ) {
        AnimatedContent(
            targetState = preview,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = {
                val direction = if (targetState != null) 1 else -1
                if (reduceMotion) {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                } else {
                    (fadeIn(tween(180)) +
                        slideInVertically(importMotion(reduceMotion)) { direction * it / 16 }) togetherWith
                        (fadeOut(tween(140)) +
                            slideOutVertically(importMotion(reduceMotion)) { -direction * it / 16 })
                }.using(SizeTransform(
                    clip = false,
                    sizeAnimationSpec = { _, _ -> importMotion(reduceMotion) },
                ))
            },
            contentKey = { it != null },
            label = "importStep",
        ) { snapshot ->
            if (snapshot == null) {
                InputStep(
                    raw = raw,
                    error = error,
                    parsing = parsing,
                    onInput = ::updateInput,
                    onPickFile = openFile,
                    onOpenHelp = { helpOpen = true },
                    onParse = {
                        focus.clearFocus()
                        val submitted = raw
                        val submittedRevision = revision
                        error = null
                        parsing = true
                        scope.launch {
                            try {
                                val parsed = withContext(Dispatchers.Default) { parseTimetableJson(submitted) }
                                if (revision == submittedRevision) preview = parsed
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                if (revision == submittedRevision) {
                                    error = importErrorMessage(failure)
                                }
                            } finally {
                                parsing = false
                            }
                        }
                    },
                )
            } else {
                ImportPreview(
                    snapshot = snapshot,
                    error = error,
                    onEdit = {
                        error = null
                        preview = null
                    },
                    onConfirm = {
                        try {
                            onImport(snapshot)
                        } catch (failure: Exception) {
                            error = "无法保存课表：${failure.message?.take(200) ?: "请重试"}"
                        }
                    },
                )
            }
        }
    }
    JsonHelpSheet(open = helpOpen, onDismiss = { helpOpen = false })
}

@Composable
private fun InputStep(
    raw: String,
    error: String?,
    parsing: Boolean,
    onInput: (String) -> Unit,
    onPickFile: () -> Unit,
    onOpenHelp: () -> Unit,
    onParse: () -> Unit,
) {
    val colors = KomaTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ImportCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KomaText("课表内容", Modifier.weight(1f), size = 14, weight = FontWeight.SemiBold)
                if (raw.isNotEmpty()) KomaTextButton("清空", { onInput("") },
                    Modifier.padding(horizontal = 8.dp, vertical = 10.dp), color = colors.ink4)
                KomaTextButton("选择文件", onPickFile,
                    Modifier.padding(horizontal = 4.dp, vertical = 10.dp))
            }
            JsonInput(raw, onInput)
            InlineError(error)
            KomaButton(
                text = "预览课表",
                enabled = raw.isNotBlank() && !parsing,
                loading = parsing,
                onClick = onParse,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        SettingsGroup {
            SettingsNavRow(
                title = "用 AI 识图导入",
                glyph = KomaGlyph.Message,
                onClick = onOpenHelp,
            )
        }
    }
}

@Composable
private fun JsonInput(value: String, onChange: (String) -> Unit) {
    val colors = KomaTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .background(colors.surface2, shape)
            .border(1.dp, colors.line, shape)
            .clickable(interactions, indication = null) {
                focusRequester.requestFocus()
                keyboard?.show()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
            .semantics { contentDescription = "课表 JSON 内容，多行输入" },
    ) {
        if (value.isEmpty()) {
            KomaText("粘贴 AI 返回的内容", size = 13, color = colors.ink4)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            textStyle = TextStyle(
                color = colors.ink,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontFamily = komaFontFamily(),
            ),
            cursorBrush = SolidColor(colors.accent),
        )
    }
}

@Composable
private fun ImportPreview(
    snapshot: TimetableSnapshot,
    error: String?,
    onEdit: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = KomaTheme.colors
    ImportCard {
        KomaText("识别结果", size = 11.5f, weight = FontWeight.SemiBold,
            color = colors.ink3, letterSpacing = 0.5f)
        KomaText(snapshot.semester.name, size = 19, weight = FontWeight.Bold, letterSpacing = -0.45f)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ImportStat("第 1 周周一", snapshot.semester.startDate)
            ImportStat("总周数", "${snapshot.semester.totalWeeks} 周")
            ImportStat("课程", "${snapshot.courses.size} 门")
        }
        KomaText(
            text = if (snapshot.courses.isEmpty()) "空课表：导入后不会显示任何课程。"
            else "导入后，这份课表将替换当前课表。",
            size = 12,
            weight = FontWeight.Medium,
            color = if (snapshot.courses.isEmpty()) colors.danger else colors.ink3,
        )
        InlineError(error)
        KomaButton("确认导入", onConfirm)
        KomaTextButton("返回修改", onEdit,
            Modifier.align(Alignment.CenterHorizontally).padding(vertical = 6.dp))
    }
}

private fun importErrorMessage(failure: Exception): String {
    val raw = failure.message?.take(300)?.trim().orEmpty()
    if (raw.isEmpty()) return "没能识别这份课表，请确认粘贴了完整的 AI 回复。"
    val reason = if (raw.startsWith("\$")) raw.substringAfter('：', raw) else raw
    return "没能识别这份课表：$reason"
}

@Composable
private fun InlineError(error: String?) {
    val reduceMotion = LocalAppPreferences.current.reduceMotion
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn(importMotion(reduceMotion)) + expandVertically(importMotion(reduceMotion)),
        exit = fadeOut(tween(120)) + shrinkVertically(tween(120)),
    ) {
        KomaText(error.orEmpty(), Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            size = 12.5f, weight = FontWeight.Medium, color = KomaTheme.colors.danger)
    }
}

@Composable
private fun RowScope.ImportStat(label: String, value: String) {
    val colors = KomaTheme.colors
    Column(
        Modifier.weight(1f).background(colors.surface2, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        KomaText(label, size = 10.5f, weight = FontWeight.Medium, color = colors.ink4, maxLines = 1)
        KomaText(value, Modifier.padding(top = 2.dp), size = 13,
            weight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun ImportCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(KomaTheme.colors.surface, RoundedCornerShape(19.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}
