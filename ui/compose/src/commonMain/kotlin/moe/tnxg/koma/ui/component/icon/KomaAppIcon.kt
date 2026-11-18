package moe.tnxg.koma.ui.component.icon

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 应用内图标（欢迎页/关于页），由 app/res 自适应图标前景+背景合成，裁到 72dp 可见区。
 *
 * 用代码构建 ImageVector 而不是 composeResources 里的 VectorDrawable XML：
 * HMOS 的 Compose 矢量解析器不填 `namespaceURI`，`<aapt:attr>` 渐变填充整条路径会被丢弃
 * （只剩纯色路径，看起来像“错位”）。代码里的 [Brush] 渐变走 Skia，所有平台一致。
 * 改图标需与 app/res、harmonyApp 的分层图标同步。
 *
 * 注意：这里使用 `addGroup`/`clearGroup` 手动配对，而不是 1.10+ 的 lambda DSL，
 * 因为 OHOS 线固定在 CMP 1.9.2，lambda 版 `addGroup { }` 在 1.9.2 不存在。
 */
val KomaAppIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "KomaAppIcon",
        defaultWidth = 72.dp,
        defaultHeight = 72.dp,
        viewportWidth = 72f,
        viewportHeight = 72f,
    ).apply {
        addGroup(translationX = -18f, translationY = -18f)
        // 底板：白到页面底色
        addPath(
            pathData = addPathNodes("M0,0H108V108H0Z"),
            fill = Brush.linearGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFF2F2F1)),
                start = Offset(0f, 0f),
                end = Offset(0f, 108f),
            ),
        )
        addGroup(
            scaleX = 0.075f,
            scaleY = 0.075f,
            translationX = 15.6f,
            translationY = 15.6f,
        )
        // 空白节次
        listOf(
            "M454,232H570A26,26 0,0 1,596,258V296A26,26 0,0 1,570,322H454A26,26 0,0 1,428,296V258A26,26 0,0 1,454,232Z",
            "M454,350H570A26,26 0,0 1,596,376V414A26,26 0,0 1,570,440H454A26,26 0,0 1,428,414V376A26,26 0,0 1,454,350Z",
            "M454,584H570A26,26 0,0 1,596,610V648A26,26 0,0 1,570,674H454A26,26 0,0 1,428,648V610A26,26 0,0 1,454,584Z",
            "M454,702H570A26,26 0,0 1,596,728V766A26,26 0,0 1,570,792H454A26,26 0,0 1,428,766V728A26,26 0,0 1,454,702Z",
            "M650,467H766A26,26 0,0 1,792,493V531A26,26 0,0 1,766,557H650A26,26 0,0 1,624,531V493A26,26 0,0 1,650,467Z",
        ).forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color(0xFFEEF0FC))) }

        // 竖笔：主渐变
        addPath(
            pathData = addPathNodes(
                "M266,232H366A34,34 0,0 1,400,266V758A34,34 0,0 1,366,792H266A34,34 0,0 1,232,758V266A34,34 0,0 1,266,232Z"
            ),
            fill = Brush.linearGradient(
                colors = listOf(Color(0xFF5D69E0), Color(0xFF4450C8)),
                start = Offset(232f, 232f),
                end = Offset(232f, 792f),
            ),
        )

        // 实心节次
        addPath(
            pathData = addPathNodes(
                "M454,467H570A26,26 0,0 1,596,493V531A26,26 0,0 1,570,557H454A26,26 0,0 1,428,531V493A26,26 0,0 1,454,467Z"
            ),
            fill = SolidColor(Color(0xFF8A93E5)),
        )
        addPath(
            pathData = addPathNodes(
                "M658,232H758A34,34 0,0 1,792,266V406A34,34 0,0 1,758,440H658A34,34 0,0 1,624,406V266A34,34 0,0 1,658,232Z"
            ),
            fill = SolidColor(Color(0xFF7A6BD0)),
        )
        addPath(
            pathData = addPathNodes(
                "M658,584H758A34,34 0,0 1,792,618V758A34,34 0,0 1,758,792H658A34,34 0,0 1,624,758V618A34,34 0,0 1,658,584Z"
            ),
            fill = SolidColor(Color(0xFF5574D8)),
        )

        // 顶部高光
        listOf(
            "M272,266H328A8,8 0,0 1,328,282H272A8,8 0,0 1,272,266Z",
            "M664,266H712A8,8 0,0 1,712,282H664A8,8 0,0 1,664,266Z",
            "M664,618H712A8,8 0,0 1,712,634H664A8,8 0,0 1,664,618Z",
        ).forEach {
            addPath(
                pathData = addPathNodes(it),
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.85f,
            )
        }
        clearGroup()
        clearGroup()
    }.build()
}
