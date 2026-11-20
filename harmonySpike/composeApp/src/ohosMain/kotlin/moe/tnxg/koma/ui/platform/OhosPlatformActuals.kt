@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package moe.tnxg.koma.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalArkUIViewController
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val controller = LocalArkUIViewController.current
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnBack by rememberUpdatedState(onBack)
    DisposableEffect(controller) {
        // Keep composition order stable when enabled changes: sheets/profile beat route handlers.
        val cancel = controller.onBackPressedDispatcher.addOnBackPressedCallback {
            if (currentEnabled && controller.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                currentOnBack()
                true
            } else {
                false
            }
        }
        onDispose { cancel() }
    }
}

@Composable
actual fun PlatformForeground(onForeground: () -> Unit) {
    LifecycleEventEffect(
        event = Lifecycle.Event.ON_RESUME,
        lifecycleOwner = LocalArkUIViewController.current,
        onEvent = onForeground,
    )
}

@Composable
actual fun Modifier.gestureExclusionBand(limit: Dp): Modifier = this
