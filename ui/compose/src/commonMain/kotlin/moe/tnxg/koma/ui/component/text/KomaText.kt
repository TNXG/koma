package moe.tnxg.koma.ui.component.text

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
fun KomaText(
    text: String,
    modifier: Modifier = Modifier,
    size: Int = 14,
    weight: FontWeight = FontWeight.Normal,
    color: Color = KomaTheme.colors.ink,
    maxLines: Int = Int.MAX_VALUE,
    letterSpacing: Float = 0f,
    lineHeight: Float = 1.45f,
) {
    KomaText(text, modifier, size.toFloat(), weight, color, maxLines, letterSpacing, lineHeight)
}

@Composable
fun KomaText(
    text: String,
    modifier: Modifier = Modifier,
    size: Float,
    weight: FontWeight = FontWeight.Normal,
    color: Color = KomaTheme.colors.ink,
    maxLines: Int = Int.MAX_VALUE,
    letterSpacing: Float = 0f,
    lineHeight: Float = 1.45f,
) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            fontWeight = weight,
            fontFamily = komaFontFamily(),
            fontFeatureSettings = "cv11, ss01",
            lineHeight = (size * lineHeight).sp,
            letterSpacing = letterSpacing.sp,
        ),
    )
}
