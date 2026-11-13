package moe.tnxg.koma.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.lerp
import moe.tnxg.koma.app.preferences.AccentPreference
import moe.tnxg.koma.app.preferences.LocalAppPreferences

private val LocalColors = staticCompositionLocalOf { LightColors }

object KomaTheme {
    val colors: KomaColors
        @Composable get() = LocalColors.current
}

@Composable
fun KomaTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val base = if (dark) DarkColors else LightColors
    val accent = when (LocalAppPreferences.current.accent) {
        AccentPreference.Iris -> base.accent
        AccentPreference.Blue -> base.toneBlue
        AccentPreference.Green -> base.toneGreen
        AccentPreference.Rose -> base.toneRose
        AccentPreference.Amber -> base.toneAmber
    }
    val colors = base.copy(accent = accent, accent2 = lerp(accent, base.surface, .3f),
        accentSoft = lerp(base.bg, accent, if (dark) .18f else .08f))
    CompositionLocalProvider(LocalColors provides colors) {
        content()
    }
}
