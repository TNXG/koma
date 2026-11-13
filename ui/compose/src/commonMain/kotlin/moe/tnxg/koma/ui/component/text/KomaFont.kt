package moe.tnxg.koma.ui.component.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import moe.tnxg.koma.resources.Res
import moe.tnxg.koma.resources.Inter400
import moe.tnxg.koma.resources.Inter500
import moe.tnxg.koma.resources.Inter600
import moe.tnxg.koma.resources.Inter700
import moe.tnxg.koma.resources.Inter800
import org.jetbrains.compose.resources.Font

@Composable
fun komaFontFamily(): FontFamily {
    val normal = Font(Res.font.Inter400, FontWeight.Normal)
    val medium = Font(Res.font.Inter500, FontWeight.Medium)
    val semibold = Font(Res.font.Inter600, FontWeight.SemiBold)
    val bold = Font(Res.font.Inter700, FontWeight.Bold)
    val extraBold = Font(Res.font.Inter800, FontWeight.ExtraBold)
    return remember(normal, medium, semibold, bold, extraBold) {
        FontFamily(normal, medium, semibold, bold, extraBold)
    }
}
