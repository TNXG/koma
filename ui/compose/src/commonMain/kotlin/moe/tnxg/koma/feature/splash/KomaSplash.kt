package moe.tnxg.koma.feature.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.tnxg.koma.app.preferences.LocalAppPreferences
import moe.tnxg.koma.ui.component.button.KomaButton
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

// Same curve as page transitions; the splash is the app's first frame, so it borrows the token.
private val Ease = CubicBezierEasing(.22f, .9f, .24f, 1f)
private const val PieceMs = 240
private const val ExitMs = 220

/** Visible launch content stays on screen until the destination has rendered beneath it. */
@Composable
fun KomaSplash(
    visible: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onIntroFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalAppPreferences.current.reduceMotion
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = EnterTransition.None,
        exit = fadeOut(tween(if (reduceMotion) 120 else ExitMs, easing = Ease)),
    ) {
        SplashSurface(error, onRetry, reduceMotion, onIntroFinished)
    }
}

@Composable
private fun SplashSurface(
    error: String?,
    onRetry: () -> Unit,
    reduceMotion: Boolean,
    onIntroFinished: () -> Unit,
) {
    val colors = KomaTheme.colors
    val reveals = remember { splashPieces.map { Animatable(if (reduceMotion) 1f else 0f) } }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            reveals.forEach { it.snapTo(1f) }
        } else {
            coroutineScope {
                reveals.forEachIndexed { index, reveal ->
                    launch {
                        delay(splashPieces[index].delayMs.toLong())
                        reveal.animateTo(1f, tween(PieceMs, easing = Ease))
                    }
                }
            }
        }
        onIntroFinished()
    }
    Box(Modifier.fillMaxSize().background(colors.bg)) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SplashMark(reveals, Modifier.size(84.dp))
            Spacer(Modifier.height(24.dp))
            KomaText("Koma", size = 17, weight = FontWeight.Bold, letterSpacing = -.2f)
            Spacer(Modifier.height(20.dp))
            if (error == null) {
                LoadingRail(reduceMotion)
            } else {
                LaunchError(error, onRetry)
            }
        }
    }
}

@Composable
private fun LoadingRail(reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val colors = KomaTheme.colors
    val position = if (reduceMotion) null else rememberInfiniteTransition().animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
    )
    Canvas(
        modifier.size(64.dp, 2.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(colors.line, cornerRadius = radius)
        val segment = size.width * .32f
        // Reduced motion keeps the cue but drops the sweep.
        val x = (size.width - segment) * (position?.value ?: .34f)
        drawRoundRect(
            colors.accent,
            topLeft = Offset(x, 0f),
            size = Size(segment, size.height),
            cornerRadius = radius,
        )
    }
}

@Composable
private fun LaunchError(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        KomaText(message, size = 13, color = KomaTheme.colors.ink3, maxLines = 3)
        Spacer(Modifier.height(16.dp))
        KomaButton("读取失败，重试", onRetry, Modifier.width(180.dp))
    }
}
