package moe.tnxg.koma.feature.home.timeline.course

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.domain.timeline.RowState
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
internal fun CourseRail(state: RowState, tone: Color, pct: Float, first: Boolean, last: Boolean, modifier: Modifier) {
    val c=KomaTheme.colors
    val progress = animateFloatAsState(pct.coerceIn(0f,100f)/100f,tween(600,easing=CubicBezierEasing(.25f,.1f,.25f,1f)))
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            val x=size.width/2; val top=14.dp.toPx(); val bottom=size.height-top
            val future=state==RowState.Next || state==RowState.Later
            val line=when(state) { RowState.Now->lerp(c.line,tone,.22f); RowState.Past->c.line; else->lerp(c.line,tone,.42f) }
            drawLine(line,Offset(x,if(first)top else 0f),Offset(x,if(last)bottom else size.height),2.dp.toPx(),
                pathEffect=if(future) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),5.dp.toPx())) else null)
            if(state==RowState.Now) {
                val y=top+(bottom-top)*progress.value
                drawLine(tone,Offset(x,top),Offset(x,y),2.dp.toPx())
                drawCircle(tone.copy(alpha=.12f),9.dp.toPx(),Offset(x,y))
                drawCircle(c.bg,6.dp.toPx(),Offset(x,y)); drawCircle(tone,3.5.dp.toPx(),Offset(x,y))
            }
            listOf(top,bottom).forEach { y ->
                drawLine(if(state==RowState.Past)c.line else if(state==RowState.Now)tone else lerp(c.line,tone,.55f),
                    Offset(x,y),Offset(x+20.dp.toPx(),y),1.5.dp.toPx())
                drawCircle(c.bg,8.5.dp.toPx(),Offset(x,y))
                drawCircle(if(state==RowState.Past)c.bg else c.surface,6.dp.toPx(),Offset(x,y))
                val node=if(state==RowState.Past)c.ink5 else if(future)lerp(c.ink5,tone,.55f) else tone
                drawCircle(node,4.75.dp.toPx(),Offset(x,y),style=Stroke(if(future)2.dp.toPx() else 2.5.dp.toPx(),
                    pathEffect=if(future)PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(),2.dp.toPx())) else null))
            }
        }
        if(state==RowState.Now) NowPulse(tone)
    }
}

/**
 * The pulse repeats forever, so it gets its own layer: only this layer records per frame, and the
 * rail and the rest of the screen keep their recorded display lists.
 */
@Composable
private fun BoxScope.NowPulse(tone: Color) {
    val pulse = rememberInfiniteTransition().animateFloat(0f,1f,
        infiniteRepeatable(tween(2200,easing=CubicBezierEasing(.22f,.9f,.24f,1f))))
    Canvas(Modifier.matchParentSize().graphicsLayer()) { pulseRing(tone, pulse.value) }
}

private fun DrawScope.pulseRing(tone: Color, pulseValue: Float) {
    if(pulseValue>=.7f) return
    val x=size.width/2; val top=14.dp.toPx(); val bottom=size.height-top
    val p=pulseValue/.7f
    listOf(top,bottom).forEach { y ->
        drawCircle(tone.copy(alpha=.7f*(1-p)),7.dp.toPx()*(.72f+1.38f*p),Offset(x,y),style=Stroke(1.5.dp.toPx()))
    }
}
