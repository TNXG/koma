// Adapted from compose-miuix-ui / SukiSU FloatingBottomBar (Apache-2.0).
package moe.tnxg.koma.feature.home.dock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.sensor.rememberDeviceTilt
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val dockSpecular = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = .12f),
        innerBlurRadius = 2.dp,
        primaryLight = LightSource(LightPosition(.5f, -.3f, -.05f), Color.White, 1f),
        secondaryLight = LightSource(LightPosition(.5f, .8f, -.5f), Color.White, .4f),
        dualPeak = true,
    ),
)

/** 和上游一样，重力方向以 3° 量化，避免每次传感器更新都重建高光。 */
@Composable
internal fun rememberDockHighlight(extraDegrees: Float): State<Highlight> {
    val tilt = rememberDeviceTilt()
    val angle = remember(tilt) {
        derivedStateOf {
            val gravity = tilt.value
            val step = (3.0 * PI / 180.0).toFloat()
            if (gravity.gravityX * gravity.gravityX + gravity.gravityY * gravity.gravityY > .01f) {
                (atan2(gravity.gravityY, gravity.gravityX) / step).roundToInt() * step
            } else (-PI / 2).toFloat()
        }
    }
    return remember(angle, extraDegrees) {
        derivedStateOf {
            val style = dockSpecular.style as BloomStroke
            val radians = angle.value + (extraDegrees * PI / 180.0).toFloat()
            dockSpecular.copy(style = style.copy(primaryLight = style.primaryLight.copy(
                position = LightPosition(.5f + cos(radians), .7f + sin(radians), style.primaryLight.position.z),
            )))
        }
    }
}
