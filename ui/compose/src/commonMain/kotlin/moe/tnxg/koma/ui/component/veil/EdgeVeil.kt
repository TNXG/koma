package moe.tnxg.koma.ui.component.veil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import moe.tnxg.koma.ui.theme.KomaTheme

enum class VeilEdge { Top, Bottom }

private data class BlurBand(val radius: Float, val from: Float, val to: Float)

// Faint bands reach deepest into the content; each stronger band stops nearer the edge, so
// the blur ramps up to the edge without a visible seam. `from`/`to` are percentages of [height].
private val blurBands = listOf(
    BlurBand(1f, 70f, 100f), BlurBand(2f, 56f, 92f),
    BlurBand(4f, 42f, 78f), BlurBand(8f, 26f, 62f), BlurBand(14f, 8f, 46f),
)

/**
 * Blurs and tints an already-recorded [source] toward one screen edge. [VeilEdge.Bottom] is the
 * vertical mirror of the home header's top veil: the strongest band hugs the bottom edge and the
 * background tint is opaque there instead of at the top.
 *
 * [sourceHeight] is the recorded layer's height in dp and only matters for a bottom veil, where
 * each band must sample the source's bottom slice instead of its top one. [strength] scales every
 * band's radius for sources that carry more detail than the home timeline.
 */
@Composable
fun EdgeVeil(
    source: GraphicsLayer,
    opacity: () -> Float,
    height: Float,
    edge: VeilEdge,
    sourceHeight: Float = height,
    strength: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val background = KomaTheme.colors.bg
    val density = LocalDensity.current
    val tint = remember(background, edge) { veilTint(background, edge) }
    val sourceHeightPx = with(density) { sourceHeight.dp.toPx() }
    Box(
        modifier.fillMaxWidth().height(height.dp),
        contentAlignment = if (edge == VeilEdge.Top) Alignment.TopCenter else Alignment.BottomCenter,
    ) {
        blurBands.forEach { band ->
            val drawing = remember(source, band, background, edge, sourceHeightPx, strength) {
                Modifier.drawWithCache {
                    // Half-resolution filtering keeps all five bands without full-size blur targets.
                    val sampleScale = .5f
                    val radius = band.radius.dp.toPx() * sampleScale * strength
                    val blurred = obtainGraphicsLayer()
                    blurred.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                    val sampleSize = IntSize(ceil(size.width * sampleScale).toInt(),
                        ceil(size.height * sampleScale + 3 * radius).toInt())
                    // A bottom band starts below the source's top, so its sample is shifted up.
                    val shift = if (edge == VeilEdge.Bottom) size.height - sourceHeightPx else 0f
                    val mask = if (edge == VeilEdge.Top)
                        Brush.verticalGradient(0f to Color.Black,
                            (band.from / band.to) to Color.Black, 1f to Color.Transparent)
                    else
                        Brush.verticalGradient(0f to Color.Transparent,
                            (1f - band.from / band.to) to Color.Black, 1f to Color.Black)
                    blurred.record(size = sampleSize) {
                        clipRect {
                            drawRect(background)
                            // Keep a live display-list dependency, clipped to this edge slice.
                            scale(sampleScale, pivot = Offset.Zero) {
                                withTransform({ translate(0f, shift) }) { drawLayer(source) }
                            }
                        }
                    }
                    onDrawBehind {
                        scale(1 / sampleScale, pivot = Offset.Zero) { drawLayer(blurred) }
                        drawRect(mask, blendMode = BlendMode.DstIn)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height((height * band.to / 100).dp).graphicsLayer {
                alpha = opacity()
                clip = true
                compositingStrategy = CompositingStrategy.Offscreen
            }.then(drawing))
        }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = opacity() }.background(tint))
    }
}

private fun veilTint(background: Color, edge: VeilEdge): Brush = when (edge) {
    VeilEdge.Top -> Brush.verticalGradient(0f to background.copy(alpha = .93f),
        .58f to background.copy(alpha = .86f), .86f to background.copy(alpha = .32f),
        1f to background.copy(alpha = 0f))

    VeilEdge.Bottom -> Brush.verticalGradient(0f to background.copy(alpha = 0f),
        .14f to background.copy(alpha = .32f), .42f to background.copy(alpha = .86f),
        1f to background.copy(alpha = .93f))
}
