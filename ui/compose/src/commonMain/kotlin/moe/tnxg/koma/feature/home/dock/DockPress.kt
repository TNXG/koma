package moe.tnxg.koma.feature.home.dock

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
internal fun Modifier.dockPress(scale: Float = .94f, onClick: () -> Unit): Modifier {
    if (scale == 1f) return clickable(interactionSource = null, indication = null, onClick = onClick)
    val source=remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val value by animateFloatAsState(if(pressed)scale else 1f,tween(150))
    return graphicsLayer { scaleX=value;scaleY=value }.clickable(source,indication=null,onClick=onClick)
}
