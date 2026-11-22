package moe.tnxg.koma.ui.component.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 窗口自适应的三档断点（参考 mikcb `QingyuAdaptiveRules` 的已验证口径，判据按**窗口宽度**
 * 而不是设备型号：折叠屏展开、分屏、平板横竖屏都自然工作）：
 *
 * - **紧凑**：宽 < 600dp（手机竖屏），布局与宽屏适配落地前完全一致；
 * - **中宽 / 宽屏**：宽 ≥ 600dp，内容列与贴底弹层限宽居中；内容列上限 [CONTENT_COLUMN_DP]。
 *
 * 「宽屏（横扁窗口）」目前只区分弹层与内容列是否限宽，分栏布局尚未启用；
 * [isWide] 供后续按窗口形状收放布局时使用，不参与本次限宽判定。
 */
object AdaptiveRules {
    /** 中宽档最小窗口宽（dp）：达到即内容列与弹层限宽居中。 */
    const val MEDIUM_WIDE_MIN_WIDTH_DP = 600f

    /** 宽屏判据里的「横扁」阈值：高宽比严格低于它视为横扁窗口。 */
    const val WIDE_MAX_HEIGHT_RATIO = 1.2f

    /** 内容列 / 弹层宽度上限（dp）。 */
    val CONTENT_COLUMN_DP = 640.dp

    /** 横扁（宽屏）判据。宽为 0 时高宽比按 Float 除法得 Infinity，返回 false。 */
    fun isWide(widthDp: Float, heightDp: Float): Boolean =
        widthDp >= MEDIUM_WIDE_MIN_WIDTH_DP && heightDp / widthDp < WIDE_MAX_HEIGHT_RATIO

    /** 内容列宽度上限；紧凑档返回 null（保持通栏）。 */
    fun contentColumnWidth(widthDp: Float): Dp? =
        if (widthDp >= MEDIUM_WIDE_MIN_WIDTH_DP) CONTENT_COLUMN_DP else null

    /** 贴底弹层宽度上限；紧凑档返回 null（贴边距铺满）。 */
    fun bottomSheetWidth(widthDp: Float): Dp? =
        if (widthDp >= MEDIUM_WIDE_MIN_WIDTH_DP) CONTENT_COLUMN_DP else null
}
