package moe.tnxg.koma.ui.component.button

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.ui.component.loader.KomaLoader
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
fun KomaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = KomaTheme.colors
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled && !loading) .985f else 1f, tween(150))
    val background by animateColorAsState(if (enabled) colors.accent else colors.line, tween(200))
    Box(
        modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(18.dp)).background(background)
            .clickable(interactions, indication = null, enabled = enabled && !loading, onClick = onClick)
            .padding(15.dp),
        contentAlignment = Alignment.Center,
    ) {
        KomaText(text, Modifier.alpha(if (loading) 0f else 1f), 15, FontWeight.Bold,
            if (enabled) Color.White else colors.ink4)
        if (loading) KomaLoader(18.dp, if (enabled) Color.White else colors.ink4)
    }
}

@Composable
fun KomaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = KomaTheme.colors.accent,
    enabled: Boolean = true,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val opacity by animateFloatAsState(if (!enabled) .35f else if (pressed) .6f else 1f, tween(150))
    KomaText(text, modifier.alpha(opacity).clickable(interactions, null, enabled, onClick = onClick),
        13, FontWeight.Bold, color)
}
