package moe.tnxg.koma.ui.component.layout

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 读当前窗口宽度并换算限宽取值：读数是真实窗口（`LocalWindowInfo`）而非设备型号，
 * 分屏 / 折叠屏展开 / 平板横竖屏都自然工作。紧凑档两个上限都返回 null。
 */
@Composable
fun rememberAdaptiveColumn(): AdaptiveColumn {
    val container = LocalWindowInfo.current
    val density = LocalDensity.current
    val widthDp = remember(container.containerSize, density) {
        container.containerSize.width / density.density
    }
    return remember(widthDp) {
        AdaptiveColumn(AdaptiveRules.contentColumnWidth(widthDp), AdaptiveRules.bottomSheetWidth(widthDp))
    }
}

/** 一次组合的限宽取值；null = 紧凑档，保持现状（通栏 / 贴边距）。 */
class AdaptiveColumn(val content: Dp?, val sheet: Dp?)

/** 限宽居中的标准内容列修饰：宽度取 min(父宽, cap)，列始终居中（需配 Alignment.CenterHorizontally）。 */
@Composable
fun Modifier.adaptiveContentWidth(): Modifier {
    val cap = rememberAdaptiveColumn().content ?: return this
    return this.widthIn(max = cap)
}
