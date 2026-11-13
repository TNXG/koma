package moe.tnxg.koma.feature.home.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.domain.calendar.*
import moe.tnxg.koma.domain.timeline.*
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme
import kotlin.math.roundToInt

@Composable
internal fun GapStopRow(stop: GapStop, first: Boolean, last: Boolean) {
    val c=KomaTheme.colors
    val line=when(stop.state){RowState.Past->c.line;RowState.Now->c.accent2;else->c.ink5}
    Row(Modifier.fillMaxWidth().height((22+stop.minutes*.16f).coerceIn(40f,92f).roundToInt().dp),verticalAlignment=Alignment.CenterVertically) {
        Spacer(Modifier.width(46.dp))
        Canvas(Modifier.width(18.dp).fillMaxHeight()) {
            val w=size.width/18;val h=size.height/100
            val path=Path().apply { moveTo(9*w,0f);cubicTo(2*w,12*h,16*w,26*h,9*w,40*h);cubicTo(2*w,54*h,16*w,68*h,9*w,80*h);cubicTo(5*w,88*h,7*w,96*h,9*w,100*h) }
            val fade=(12.dp.toPx()/size.height).coerceAtMost(.49f)
            val brush=Brush.verticalGradient(
                0f to line.copy(alpha=if(first)0f else 1f),fade to line,
                (1-fade) to line,1f to line.copy(alpha=if(last)0f else 1f))
            drawPath(path,brush,style=Stroke(2.dp.toPx(),cap=StrokeCap.Round))
        }
        KomaText("空闲 ${formatDuration(stop.minutes)}",Modifier.padding(start=10.dp),size=11.5f,weight=FontWeight.SemiBold,
            color=when(stop.state){RowState.Past->c.ink5;RowState.Now->c.accent;else->c.ink4},letterSpacing=.115f)
    }
}

@Composable
internal fun FreeDays(block: FreeBlock) {
    val c=KomaTheme.colors;val shape=RoundedCornerShape(20.dp)
    Column(Modifier.fillMaxWidth().padding(bottom=28.dp).shadow(4.dp,shape,ambientColor=c.ink.copy(alpha=.04f),spotColor=c.ink.copy(alpha=.04f))
        .background(Brush.linearGradient(listOf(c.surface,lerp(c.surface,c.accent,.05f))),shape).padding(horizontal=20.dp,vertical=18.dp)) {
        KomaText(block.label,size=17,weight=FontWeight.ExtraBold,letterSpacing=-.34f)
        KomaText(formatMonthDay(block.start)+(if(block.days>1)"—${formatMonthDay(block.end)}" else "")+" · ${block.days} 天",Modifier.padding(top=4.dp),size=13,weight=FontWeight.SemiBold,color=c.ink3)
    }
}

@Composable
internal fun WeekDivider(week: Int) {
    val c=KomaTheme.colors
    Row(Modifier.fillMaxWidth().padding(top=4.dp,bottom=28.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(c.line))
        KomaText("第 $week 周",size=11,weight=FontWeight.SemiBold,color=c.ink5)
        Box(Modifier.weight(1f).height(1.dp).background(c.line))
    }
}
