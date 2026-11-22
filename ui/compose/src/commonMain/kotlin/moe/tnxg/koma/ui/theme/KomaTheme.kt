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

fun komaColors(dark: Boolean, preference: AccentPreference): KomaColors {
    val base = if (dark) DarkColors else LightColors
    val accent = when (preference) {
        AccentPreference.Iris -> base.accent
        AccentPreference.Blue -> base.toneBlue
        AccentPreference.Green -> base.toneGreen
        AccentPreference.Rose -> base.toneRose
        AccentPreference.Amber -> base.toneAmber
    }
    return base.copy(accent = accent, accent2 = lerp(accent, base.surface, .3f),
        accentSoft = lerp(base.bg, accent, if (dark) .18f else .08f))
}

@Composable
fun KomaTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = komaColors(dark, LocalAppPreferences.current.accent)
    CompositionLocalProvider(LocalColors provides colors) {
        content()
    }
}
