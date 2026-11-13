package moe.tnxg.koma.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class KomaColors(
    val bg: Color, val surface: Color, val surface2: Color,
    val line: Color, val line2: Color,
    val ink: Color, val ink2: Color, val ink3: Color, val ink4: Color, val ink5: Color,
    val accent: Color, val accent2: Color, val accentSoft: Color, val danger: Color,
    val scrim: Color,
    val toneBlue: Color, val toneAmber: Color, val toneRose: Color, val toneGreen: Color,
    val toneViolet: Color, val toneOrange: Color, val toneTeal: Color,
)

internal val LightColors = KomaColors(
    bg = Color(0xFFF7F7F6), surface = Color.White, surface2 = Color(0xFFF2F2F1),
    line = Color(0xFFE5E5E3), line2 = Color(0xFFF4F4F6),
    ink = Color(0xFF1B1C20), ink2 = Color(0xFF6B6F78), ink3 = Color(0xFF8A8E97),
    ink4 = Color(0xFFA2A6AF), ink5 = Color(0xFFC4C7CE),
    accent = Color(0xFF4F5BD5), accent2 = Color(0xFF8A93E5), accentSoft = Color(0xFFEEF0FC),
    danger = Color(0xFFC25B5B), scrim = Color(0x401B1C20),
    toneBlue = Color(0xFF5574D8), toneAmber = Color(0xFFB98A2F), toneRose = Color(0xFFC95F78),
    toneGreen = Color(0xFF4C9A78), toneViolet = Color(0xFF7A6BD0), toneOrange = Color(0xFFC07A4A),
    toneTeal = Color(0xFF3F9098),
)

internal val DarkColors = KomaColors(
    bg = Color(0xFF111214), surface = Color(0xFF1C1D21), surface2 = Color(0xFF27282D),
    line = Color(0xFF2F3036), line2 = Color(0xFF26272C),
    ink = Color(0xFFF1F1EF), ink2 = Color(0xFFB3B6BE), ink3 = Color(0xFF8F939C),
    ink4 = Color(0xFF8F939C), ink5 = Color(0xFF6F737C),
    accent = Color(0xFF7C8FF0), accent2 = Color(0xFF8A93E5), accentSoft = Color(0xFF232A46),
    danger = Color(0xFFE27878), scrim = Color(0x401B1C20),
    toneBlue = Color(0xFF8FA4F0), toneAmber = Color(0xFFD9AA4C), toneRose = Color(0xFFE08BA1),
    toneGreen = Color(0xFF6FB894), toneViolet = Color(0xFFA99BEF), toneOrange = Color(0xFFD99B73),
    toneTeal = Color(0xFF6FB6BC),
)
