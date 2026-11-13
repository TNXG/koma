package moe.tnxg.koma.ui.platform

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import moe.tnxg.koma.app.preferences.LocalAppPreferences

private val EDGE_WIDTH = 28.dp
private const val COMMIT_FRACTION = 0.32f
private const val PROJECTION_SECONDS = 0.12f

/**
 * Edge-swipe back for a pushed page: only a swipe that starts inside [EDGE_WIDTH] and turns
 * horizontal is claimed, so vertical scrolling and taps keep working. The page follows the
 * finger 1:1; the release velocity predicts whether it closes, and a new grab interrupts the
 * settle animation exactly where it is.
 */
@Composable
fun Modifier.swipeBackToPop(onBack: () -> Unit, enabled: Boolean = true): Modifier {
    val reduceMotion = LocalAppPreferences.current.reduceMotion
    val edgeWidth = with(LocalDensity.current) { EDGE_WIDTH.toPx() }
    val settleMillis = if (reduceMotion) 0 else 220
    val scope = rememberCoroutineScope()
    var offset by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableIntStateOf(0) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    return this
        .onSizeChanged { width = it.width }
        .graphicsLayer { translationX = offset }
        .pointerInput(enabled, width, edgeWidth, settleMillis) {
            if (!enabled || width <= 0) return@pointerInput
            val limit = width.toFloat()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (down.position.x > edgeWidth) return@awaitEachGesture
                var claimed = false
                val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                    claimed = true
                    change.consume()
                }
                if (slop == null || !claimed) return@awaitEachGesture

                settleJob?.cancel()
                offset = (slop.position.x - down.position.x).coerceIn(0f, limit)
                val tracker = VelocityTracker()
                tracker.addPosition(slop.uptimeMillis, slop.position)
                horizontalDrag(slop.id) { change ->
                    tracker.addPosition(change.uptimeMillis, change.position)
                    change.consume()
                    offset = (offset + change.positionChange().x).coerceIn(0f, limit)
                }

                val projected = offset + tracker.calculateVelocity().x * PROJECTION_SECONDS
                val commit = offset > limit * COMMIT_FRACTION || projected > limit * COMMIT_FRACTION
                settleJob = scope.launch {
                    animate(
                        initialValue = offset,
                        targetValue = if (commit) limit else 0f,
                        animationSpec = tween(settleMillis),
                    ) { value, _ -> offset = value }
                    if (commit) onBack()
                }
            }
        }
}
