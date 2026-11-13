// Adapted from SukiSU's DampedDragAnimation / DragGestureInspector (compose-miuix-ui).
package moe.tnxg.koma.feature.home.dock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/** 单一镜片状态跨页面保留；重定向弹簧从当前值和速度继续。 */
internal class DockMotion(private val scope: CoroutineScope, initialIndex: Int) {
    val index = Animatable(initialIndex.toFloat(), .001f)
    val progress = Animatable(0f, .001f)
    val scaleX = Animatable(1f, .001f)
    val scaleY = Animatable(1f, .001f)
    val offset = Animatable(0f)
    private var pressJob: Job? = null
    private var releaseJob: Job? = null
    var dragging = false
        private set

    fun moveTo(value: Float) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            index.animateTo(value.coerceIn(0f, 2f), spring(1f, 1000f, .001f))
        }
    }

    fun press() {
        dragging = true
        releaseJob?.cancel()
        pressJob?.cancel()
        pressJob = scope.launch {
            launch { progress.animateTo(1f, spring(1f, 1000f, .001f)) }
            launch { scaleX.animateTo(78f / 56f, spring(.6f, 250f, .001f)) }
            launch { scaleY.animateTo(78f / 56f, spring(.7f, 250f, .001f)) }
        }
    }

    fun dragBy(delta: Float) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { offset.snapTo(offset.value + delta) }
    }

    fun release() {
        dragging = false
        releaseJob?.cancel()
        releaseJob = scope.launch {
            withFrameNanos { }
            snapshotFlow { index.value }.first { abs(it - index.targetValue) < .05f }
            pressJob?.cancel()
            launch { progress.animateTo(0f, spring(1f, 1000f, .001f)) }
            launch { scaleX.animateTo(1f, spring(.6f, 250f, .001f)) }
            launch { scaleY.animateTo(1f, spring(.7f, 250f, .001f)) }
            launch { offset.animateTo(0f, spring(1f, 300f, .5f)) }
        }
    }

    fun animateTo(index: Int) {
        press()
        moveTo(index.toFloat())
        release()
    }
}

/** 在 down 即开始，不等 touch slop；保持同一 pointer，第二根手指不抢镜片。 */
internal suspend fun PointerInputScope.inspectDockGestures(
    onStart: (Offset) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        onStart(down.position)
        var finished = false
        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.changedToUpIgnoreConsumed()) {
                    onEnd()
                    finished = true
                    break
                }
                if (change.pressed && change.position != change.previousPosition) {
                    onDrag(change, change.positionChangeIgnoreConsumed())
                }
            }
        } finally {
            if (!finished) onCancel()
        }
    }
}
