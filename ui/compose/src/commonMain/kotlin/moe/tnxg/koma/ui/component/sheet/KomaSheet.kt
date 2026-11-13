package moe.tnxg.koma.ui.component.sheet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import moe.tnxg.koma.ui.platform.PlatformBackHandler
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
fun KomaSheet(open: Boolean, onDismiss: () -> Unit, horizontalPadding: Dp = 16.dp,
    bottomPadding: Dp = 32.dp, bottomBlur: Dp = 0.dp, content: @Composable ColumnScope.() -> Unit) {
    val colors = KomaTheme.colors
    val progress = remember { Animatable(0f) }
    var mounted by remember { mutableStateOf(false) }
    var height by remember { mutableFloatStateOf(600f) }
    var drag by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val interactions = remember { MutableInteractionSource() }
    LaunchedEffect(open) {
        if (open) mounted = true
        progress.animateTo(if (open) 1f else 0f,
            tween(500, easing = CubicBezierEasing(.32f, .72f, 0f, 1f)))
        if (!open) { mounted = false; drag = 0f }
    }
    PlatformBackHandler(mounted, onDismiss)
    if (!mounted) return
    Box(Modifier.fillMaxSize().semantics { dialog() }) {
        Box(Modifier.fillMaxSize()
            .background(colors.scrim.copy(alpha = colors.scrim.alpha * progress.value * (1 - drag / height).coerceIn(0f, 1f)))
            .clickable(interactions, indication = null, onClick = onDismiss))
        Box(Modifier.align(Alignment.BottomCenter).widthIn(max = 430.dp).fillMaxWidth()) {
            val record = rememberGraphicsLayer()
            Column(
                Modifier.fillMaxWidth()
                    .onSizeChanged { height = it.height.toFloat().coerceAtLeast(1f) }
                    .graphicsLayer { translationY = height * (1 - progress.value) + drag }
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(colors.surface)
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .draggable(
                        state = rememberDraggableState { delta -> drag = (drag + delta).coerceAtLeast(0f) },
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            if (drag > height * .25f || velocity > with(density) { 500.dp.toPx() }) {
                                onDismiss()
                            } else {
                                scope.launch {
                                    val settle = Animatable(drag)
                                    settle.animateTo(0f, tween(500, easing = CubicBezierEasing(.32f, .72f, 0f, 1f))) {
                                        drag = value
                                    }
                                }
                            }
                        },
                    )
                    .drawWithContent {
                        record.record { this@drawWithContent.drawContent() }
                        drawLayer(record)
                    }
                    .padding(start = horizontalPadding, end = horizontalPadding, bottom = bottomPadding),
            ) {
                Box(Modifier.fillMaxWidth().height(16.dp), contentAlignment = Alignment.BottomCenter) {
                    Spacer(Modifier.width(100.dp).height(4.dp).clip(RoundedCornerShape(20.dp)).background(colors.line))
                }
                content()
            }
            if (bottomBlur > 0.dp) {
                val blurHeight = with(density) { bottomBlur.toPx() }
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(bottomBlur)
                    .graphicsLayer {
                        translationY = height * (1 - progress.value) + drag
                        clip = true
                    }) {
                    listOf(2.dp, 4.dp, 8.dp, 14.dp).forEachIndexed { index, blur ->
                        val radius = with(density) { blur.toPx() }
                        Box(Modifier.fillMaxSize().graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }.drawWithCache {
                            val blurred = obtainGraphicsLayer()
                            blurred.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                            blurred.record {
                                translate(top = -(height - blurHeight)) { drawLayer(record) }
                            }
                            val mask = Brush.verticalGradient(
                                0f to Color.Transparent,
                                (index + 1) / 5f to Color.Transparent,
                                1f to Color.Black)
                            onDrawBehind {
                                drawLayer(blurred)
                                drawRect(mask, blendMode = BlendMode.DstIn)
                            }
                        })
                    }
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                        0f to colors.surface.copy(alpha = 0f),
                        1f to colors.surface.copy(alpha = .9f))))
                }
            }
        }
    }
}
