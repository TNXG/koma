package moe.tnxg.koma.feature.home.date

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

internal fun Modifier.islandFeather(): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
    clip = true
}.drawWithCache {
    val feather = 14.dp.toPx().coerceAtMost(size.height / 2)
    val top = Brush.verticalGradient(listOf(Color.Black, Color.Transparent), 0f, feather)
    val bottom = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), size.height - feather, size.height)
    onDrawWithContent {
        drawContent()
        // Erase only the two edges in this layer; never paint a mask over content.
        drawRect(top, blendMode = BlendMode.DstOut)
        drawRect(bottom, blendMode = BlendMode.DstOut)
    }
}
