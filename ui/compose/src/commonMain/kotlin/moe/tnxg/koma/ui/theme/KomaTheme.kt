package moe.tnxg.koma.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalColors = staticCompositionLocalOf { LightColors }

object KomaTheme {
    val colors: KomaColors
        @Composable get() = LocalColors.current
}

@Composable
fun KomaTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalColors provides if (dark) DarkColors else LightColors) {
        content()
    }
}
