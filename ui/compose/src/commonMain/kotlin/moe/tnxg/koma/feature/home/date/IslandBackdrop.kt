package moe.tnxg.koma.feature.home.date

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/**
 * Blurs the already-recorded timeline below the island. The source layer covers the whole
 * screen, so the island's own position in the root decides which slice is sampled.
 */
internal fun Modifier.islandBackdrop(
    source: GraphicsLayer?,
    radius: Dp = 18.dp,
    origin: () -> Offset,
): Modifier {
    if (source == null) return this
    return drawWithCache {
        val r = radius.toPx()
        val layer = obtainGraphicsLayer()
        layer.renderEffect = BlurEffect(r, r, TileMode.Clamp)
        onDrawBehind {
            val point = origin()
            val width = ceil(size.width + 2 * r).toInt()
            val height = ceil(size.height + 2 * r).toInt()
            if (width <= 0 || height <= 0) return@onDrawBehind
            // Record an oversized slice so the blur never fades against transparent edges.
            layer.record(IntSize(width, height)) {
                withTransform({ translate(r - point.x, r - point.y) }) { drawLayer(source) }
            }
            withTransform({ translate(-r, -r) }) { drawLayer(layer) }
        }
    }
}
