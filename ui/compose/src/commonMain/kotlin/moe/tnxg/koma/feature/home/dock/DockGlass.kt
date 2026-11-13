// Adapted from compose-miuix-ui / SukiSU FloatingBottomBar (Apache-2.0).
package moe.tnxg.koma.feature.home.dock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.colorControls
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight

/**
 * 把页面内容和底栏图标层合成一个采样源：镜片掠过图标时能同时折射两者。
 * 与 compose-miuix-ui 的 CombinedBackdrop 同构（Apache-2.0）。
 */
internal class CombinedBackdrop(
    val first: Backdrop,
    val second: Backdrop,
) : Backdrop {

    override val isCoordinatesDependent: Boolean
        get() = first.isCoordinatesDependent || second.isCoordinatesDependent

    override val offsetResidualX: Float get() = first.offsetResidualX
    override val offsetResidualY: Float get() = first.offsetResidualY

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int,
    ) {
        with(first) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
        with(second) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
    }
}

@Composable
internal fun rememberCombinedBackdrop(first: Backdrop, second: Backdrop): Backdrop =
    remember(first, second) { CombinedBackdrop(first, second) }

/** 页面玻璃底板；隐藏的强调色按钮层也使用相同处理，不能只录图标。 */
internal fun Modifier.dockGlass(
    source: Backdrop,
    shape: Shape,
    surface: Color,
    highlightOf: () -> Highlight?,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
): Modifier = drawBackdrop(
    backdrop = source,
    shape = { shape },
    effects = {
        // 折射要读形状外的像素，先把离屏层撑开，边缘才不会拿空内容做位移。
        padding = maxOf(padding, 40.dp.toPx())
        colorControls(saturation = 1.5f)
        blur(4.dp.toPx())
        lens(refractionHeight = 24.dp.toPx(), refractionAmount = 24.dp.toPx())
    },
    highlight = { highlightOf() },
    layerBlock = layerBlock,
    onDrawSurface = { drawRect(surface) },
)

/** 选中镜片只折射合成层，不二次模糊其中的按钮文字。 */
internal fun Modifier.indicatorGlass(
    source: Backdrop,
    shape: Shape,
    dark: Boolean,
    pressProgress: () -> Float,
    scaleX: () -> Float,
    scaleY: () -> Float,
    highlightOf: () -> Highlight,
): Modifier = drawBackdrop(
    backdrop = source,
    shape = { shape },
    effects = {
        val progress = pressProgress()
        lens(
            refractionHeight = 10.dp.toPx() * progress,
            refractionAmount = 14.dp.toPx() * progress,
            depthEffect = true,
            chromaticAberration = 0.5f,
        )
    },
    highlight = { highlightOf().copy(alpha = pressProgress()) },
    layerBlock = {
        this.scaleX = scaleX()
        this.scaleY = scaleY()
    },
    onDrawSurface = {
        val progress = pressProgress()
        drawRect(if (dark) Color.White.copy(alpha = .1f) else Color.Black.copy(alpha = .1f),
            alpha = 1f - progress)
        drawRect(Color.Black.copy(alpha = .03f * progress))
    },
)
