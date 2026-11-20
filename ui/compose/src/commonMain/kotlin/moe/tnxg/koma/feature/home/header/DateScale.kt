package moe.tnxg.koma.feature.home.header

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

/** 设计基准宽度：以常见 360dp 手机为 1.0，宽屏等比放大，避免日期标题在大屏上显得过小。 */
private const val DATE_BASE_WIDTH_DP = 360f

/** 顶部/分组日期标题的缩放系数，跟随窗口宽度（0.95–1.2），不改变其他文案。 */
@Composable
internal fun rememberDateTypeScale(): Float {
    val widthDp = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp().value
    }
    return (widthDp / DATE_BASE_WIDTH_DP).coerceIn(0.95f, 1.2f)
}
