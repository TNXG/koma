package moe.tnxg.koma.feature.home.header

import androidx.compose.runtime.Immutable

internal fun smooth(from: Float, to: Float, value: Float): Float {
    val t = ((value - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (3 - 2 * t)
}

/** Scroll already supplies progress; do not ease the user's drag a second time. */
internal fun dateFlightOffset(source: Float, target: Float, progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return source + (target - source) * p
}

internal fun dateFlightScale(sourceFontSize:Float,targetFontSize:Float,progress:Float):Float =
    dateFlightOffset(1f,targetFontSize/sourceFontSize,progress)

@Immutable
internal data class HeaderMotion(
    val current: String?, val incoming: String?, val progress: Float,
    val docks: Boolean, val veil: Float,
)

internal data class BlockGeometry(val top: Float, val height: Float, val dateHeight: Float)

internal fun headerMotion(
    positions: Map<String, BlockGeometry>, scroll: Float, head: Float, gliding: Boolean,
): HeaderMotion {
    val dock = head + 1
    var current: Map.Entry<String, BlockGeometry>? = null
    var next: Map.Entry<String, BlockGeometry>? = null
    for (entry in positions.entries) {
        if (entry.value.top - scroll <= dock) {
            if (current == null || entry.value.top > current.value.top) current = entry
        } else if (next == null || entry.value.top < next.value.top) {
            next = entry
        }
    }
    val p = if (!gliding && current != null && next != null)
        ((head + 73 - (next.value.top - scroll)) / 72).coerceIn(0f, 1f) else 0f
    val t = (scroll / 24).coerceIn(0f, 1f)
    return HeaderMotion(current?.key, next?.key?.takeIf { p > 0 }, p,
        next?.value?.dateHeight?.let { it > 0 } == true, 1 - (1 - t) * (1 - t))
}
