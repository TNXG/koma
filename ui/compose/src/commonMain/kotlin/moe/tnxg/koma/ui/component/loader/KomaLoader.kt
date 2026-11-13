package moe.tnxg.koma.ui.component.loader

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.ui.theme.KomaTheme
import kotlin.math.floor

@Composable
fun KomaLoader(size: Dp = 20.dp, color: Color = KomaTheme.colors.ink4) {
    val transition = rememberInfiniteTransition()
    val angle by transition.animateFloat(0f, 360f,
        infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart))
    Canvas(Modifier.size(size)) {
        val radius = this.size.minDimension / 2
        rotate(floor(angle / 30) * 30) {
            repeat(12) { index ->
                rotate(index * 30f) {
                    drawLine(color.copy(alpha = (index + 1) / 12f),
                        Offset(center.x, center.y - radius * .48f),
                        Offset(center.x, center.y - radius * .85f),
                        strokeWidth = radius * .19f, cap = StrokeCap.Round)
                }
            }
        }
    }
}
