package moe.tnxg.koma.ui.component.icon

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
fun KomaIcon(glyph: KomaGlyph, description: String?, modifier: Modifier = Modifier,
    color: Color = KomaTheme.colors.ink) {
    val vector = remember(glyph) {
        ImageVector.Builder(glyph.name, 24.dp, 24.dp, 24f, 24f)
            .addPath(PathParser().parsePathString(glyph.path).toNodes(), fill = SolidColor(Color.Black))
            .build()
    }
    Image(vector, description, modifier, colorFilter = ColorFilter.tint(color))
}
