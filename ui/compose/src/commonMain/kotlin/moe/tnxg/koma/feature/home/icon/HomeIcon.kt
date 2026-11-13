package moe.tnxg.koma.feature.home.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon

@Composable
internal fun HomeIcon(vector: KomaGlyph, color: Color, modifier: Modifier = Modifier, label: String? = null) {
    KomaIcon(vector, label, modifier, color)
}
