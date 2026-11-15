package moe.tnxg.koma.ui.component.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import moe.tnxg.koma.app.preferences.LocalAppPreferences

internal val LocalPageActive = compositionLocalOf { true }

// Match the app's existing entrance curve for both tabs and pushed pages.
internal fun <T> pageAnimationSpec(reduceMotion: Boolean) = tween<T>(
    durationMillis = if (reduceMotion) 0 else 220,
    easing = CubicBezierEasing(.22f, .9f, .24f, 1f),
)

@Composable
internal fun <T> PageTransition(
    page: T,
    depth: (T) -> Int,
    content: @Composable (T) -> Unit,
) {
    val reduceMotion = LocalAppPreferences.current.reduceMotion
    val active = LocalPageActive.current
    AnimatedContent(
        targetState = page,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            if (reduceMotion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val direction = if (depth(targetState) > depth(initialState)) 1 else -1
                (slideInHorizontally(pageAnimationSpec(false)) { direction * it / 8 } +
                    fadeIn(pageAnimationSpec(false))) togetherWith
                    (slideOutHorizontally(pageAnimationSpec(false)) { -direction * it / 8 } +
                        fadeOut(pageAnimationSpec(false)))
            }.using(sizeTransform = null)
        },
        label = "pageNavigation",
    ) { current ->
        // Outgoing pages stay composed for their exit, but must not handle back or edge swipes.
        CompositionLocalProvider(LocalPageActive provides (active && current == page)) {
            content(current)
        }
    }
}
