package moe.tnxg.koma.ui.platform

import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
actual fun Modifier.gestureExclusionBand(limit: Dp): Modifier {
    val band = with(LocalDensity.current) { limit.toPx() }
    return systemGestureExclusion { coordinates ->
        // The system only honors about 200dp of exclusion per edge, so never ask for more.
        val height = coordinates.size.height.toFloat()
        val size = band.coerceAtMost(height)
        val top = (height - size) / 2f
        Rect(0f, top, coordinates.size.width.toFloat(), top + size)
    }
}
