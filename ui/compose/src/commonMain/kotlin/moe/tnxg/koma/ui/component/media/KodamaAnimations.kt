package moe.tnxg.koma.ui.component.media

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember

@Composable
internal fun rememberKodamaBlink(
    infinite: InfiniteTransition,
    seed: Long,
    enabled: Boolean,
): State<Float> = if (!enabled) {
    remember { mutableFloatStateOf(1f) }
} else {
    val blinkSeed = seed * 31L
    val delayMs = ((blinkSeed % 40L) * 100L).toInt()
    val durationMs = (2000L + (blinkSeed % 40L) * 100L).toInt()
    infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = durationMs
                1f at 0
                1f at (durationMs * 0.92f).toInt()
                0.05f at (durationMs * 0.96f).toInt()
                1f at durationMs
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(delayMs),
        ),
        label = "blink",
    )
}

@Composable
internal fun rememberKodamaFloat(infinite: InfiniteTransition, enabled: Boolean): State<Float> =
    if (!enabled) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 3000
                    0f at 0
                    -0.03f at 1500
                    0f at 3000
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "float",
        )
    }

@Composable
internal fun rememberKodamaSway(infinite: InfiniteTransition, enabled: Boolean): State<Float> =
    if (!enabled) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 5000
                    0f at 0
                    -0.02f at 1250
                    0.02f at 3750
                    0f at 5000
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "sway",
        )
    }

@Composable
internal fun rememberKodamaEyeWanderX(infinite: InfiniteTransition, enabled: Boolean): State<Float> =
    if (!enabled) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 8000
                    0f at 0
                    0.015f at 1600
                    0.005f at 3200
                    -0.015f at 4800
                    -0.005f at 6400
                    0f at 8000
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "eyeWanderX",
        )
    }

@Composable
internal fun rememberKodamaEyeWanderY(infinite: InfiniteTransition, enabled: Boolean): State<Float> =
    if (!enabled) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 8000
                    0f at 0
                    -0.005f at 1600
                    0.005f at 3200
                    0f at 4800
                    -0.005f at 6400
                    0f at 8000
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "eyeWanderY",
        )
    }

@Composable
internal fun rememberKodamaEyebrowBounce(infinite: InfiniteTransition, enabled: Boolean): State<Float> =
    if (!enabled) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 4000
                    0f at 0
                    0f at 3520
                    -0.02f at 3760
                    0f at 4000
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "eyebrowBounce",
        )
    }

@Composable
internal fun rememberKodamaGlance(
    infinite: InfiniteTransition,
    seed: Long,
    enabled: Boolean,
): State<Float> = if (!enabled) {
    remember { mutableFloatStateOf(0f) }
} else {
    val glanceSeed = seed * 37L
    val durationMs = (6000L + (glanceSeed % 30L) * 100L).toInt()
    val delayMs = ((glanceSeed % 20L) * 100L).toInt()
    infinite.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = durationMs
                0f at 0
                0f at (durationMs * 0.82f).toInt()
                1f at (durationMs * 0.88f).toInt() using LinearEasing
                1f at (durationMs * 0.92f).toInt()
                0f at durationMs
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(delayMs),
        ),
        label = "glance",
    )
}
