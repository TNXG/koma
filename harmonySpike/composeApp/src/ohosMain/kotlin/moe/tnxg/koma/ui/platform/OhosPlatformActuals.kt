@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package moe.tnxg.koma.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.Dp

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled, onBack)
}

@Composable
actual fun PlatformForeground(onForeground: () -> Unit) {
    // M1：前台回调降级为 no-op（ArkUIViewController 生命周期桥接待后续里程碑接入）
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { } }
}

@Composable
actual fun Modifier.gestureExclusionBand(limit: Dp): Modifier = this
