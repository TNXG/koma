package moe.tnxg.koma.ui.component.sheet

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.colorControls
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported

/** Thick, softly lit glass: samples only the page behind the sheet, never its own text. */
@Composable
internal fun Modifier.sheetGlass(source: Backdrop, shape: Shape, surface: Color): Modifier {
    if (!isRuntimeShaderSupported()) return background(surface, shape)
    val dark = surface.luminance() < .2f
    val edge = remember(dark) {
        Highlight(width = .8.dp, alpha = if (dark) .65f else .9f,
            style = BloomStroke(color = Color.White.copy(alpha = .16f), innerBlurRadius = 3.dp,
                primaryLight = LightSource(LightPosition(.15f, -.4f, .2f), Color.White, .8f),
                secondaryLight = LightSource(LightPosition(.9f, .8f, -.5f), Color.White, .18f), dualPeak = true))
    }
    val wash = remember(dark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) .07f else .26f),
            Color.White.copy(alpha = if (dark) .015f else .045f), Color.Transparent))
    }
    return drawBackdrop(backdrop = source, shape = { shape }, effects = {
        padding = maxOf(padding, 72.dp.toPx())
        colorControls(saturation = 1.22f)
        blur(24.dp.toPx())
    }, highlight = { edge }, onDrawSurface = {
        drawRect(surface.copy(alpha = if (dark) .76f else .68f))
        drawRect(wash)
    })
}
