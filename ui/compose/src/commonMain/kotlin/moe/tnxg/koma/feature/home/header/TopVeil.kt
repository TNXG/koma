package moe.tnxg.koma.feature.home.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import moe.tnxg.koma.ui.theme.KomaTheme

private data class BlurBand(val radius:Float,val from:Float,val to:Float)
private val blurBands=listOf(BlurBand(1f,70f,100f),BlurBand(2f,56f,92f),
    BlurBand(4f,42f,78f),BlurBand(8f,26f,62f),BlurBand(14f,8f,46f))

@Composable
internal fun TopVeil(source:GraphicsLayer,opacity:()->Float,height:Float) {
    val background=KomaTheme.colors.bg
    val tint=remember(background) {
        Brush.verticalGradient(0f to background.copy(alpha=.93f),.58f to background.copy(alpha=.86f),
            .86f to background.copy(alpha=.32f),1f to background.copy(alpha=0f))
    }
    Box(Modifier.fillMaxWidth().height(height.dp)) {
        blurBands.forEach { band ->
            val drawing=remember(source,band,background) {
                Modifier.drawWithCache {
                    // Half-resolution filtering keeps all five Web bands without full-size blur targets.
                    val sampleScale=.5f
                    val radius=band.radius.dp.toPx()*sampleScale
                    val blurred=obtainGraphicsLayer()
                    blurred.renderEffect=BlurEffect(radius,radius,TileMode.Clamp)
                    val sampleSize=IntSize(ceil(size.width*sampleScale).toInt(),
                        ceil(size.height*sampleScale+3*radius).toInt())
                    val mask=Brush.verticalGradient(0f to Color.Black,
                        (band.from/band.to) to Color.Black,1f to Color.Transparent)
                    blurred.record(size=sampleSize) {
                        clipRect {
                            drawRect(background)
                            // Keep a live display-list dependency, clipped to this top slice.
                            scale(sampleScale,pivot=Offset.Zero) { drawLayer(source) }
                        }
                    }
                    onDrawBehind {
                        scale(1/sampleScale,pivot=Offset.Zero) { drawLayer(blurred) }
                        drawRect(mask,blendMode=BlendMode.DstIn)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height((height*band.to/100).dp).graphicsLayer {
                alpha=opacity()
                clip=true
                compositingStrategy=CompositingStrategy.Offscreen
            }.then(drawing))
        }
        Box(Modifier.fillMaxSize().graphicsLayer{alpha=opacity()}.background(tint))
    }
}
