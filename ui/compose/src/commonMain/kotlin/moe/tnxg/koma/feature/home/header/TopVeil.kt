package moe.tnxg.koma.feature.home.header

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.layer.GraphicsLayer
import moe.tnxg.koma.ui.component.veil.EdgeVeil
import moe.tnxg.koma.ui.component.veil.VeilEdge

/** Home header's frosted top: blur and background tint both peak at the screen edge. */
@Composable
internal fun TopVeil(source: GraphicsLayer, opacity: () -> Float, height: Float) =
    EdgeVeil(source, opacity, height, VeilEdge.Top)
