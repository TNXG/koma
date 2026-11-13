package moe.tnxg.koma.feature.home.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import moe.tnxg.koma.feature.home.header.DateGeometry
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.domain.calendar.*
import moe.tnxg.koma.domain.timeline.*
import moe.tnxg.koma.feature.home.header.smooth
import moe.tnxg.koma.feature.home.timeline.course.CourseSlot
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
internal fun DaySection(block: DayBlock, minutes: Int, viewIsToday: Boolean,
    progress: Float, hidden: Boolean,
    onHeadHeight:(Float)->Unit, onDateGeometry:(DateGeometry)->Unit,
) {
    val c=KomaTheme.colors
    val density=LocalDensity.current
    val p=progress
    val e=p
    val size=if(block.relative==null)17f else 12.5f
    Column(Modifier.fillMaxWidth().padding(bottom=28.dp)) {
        Row(Modifier.fillMaxWidth().onGloballyPositioned { onHeadHeight(with(density){it.size.height.toDp().value}) }
            .padding(bottom=10.dp).graphicsLayer{alpha=if(hidden)0f else 1f},verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            block.relative?.let { KomaText(it,Modifier.graphicsLayer{alpha=1-(p*2.4f).coerceIn(0f,1f);translationY=-8.dp.toPx()*e},size=17,lineHeight=1f,weight=FontWeight.ExtraBold,letterSpacing=-.34f) }
            Row(Modifier.onGloballyPositioned {
                onDateGeometry(with(density){DateGeometry(
                    it.positionInParent().x.toDp().value,it.positionInParent().y.toDp().value,
                    it.size.height.toDp().value,size)})
            }.graphicsLayer { alpha=if(p>0f)0f else 1f }) {
                KomaText(formatMonthDay(block.date),size=size,lineHeight=1.2f,weight=if(block.relative==null)FontWeight.ExtraBold else FontWeight.Bold,
                    color=if(block.relative==null)c.ink else lerp(c.ink3,c.ink,e),letterSpacing=if(size==17f)-.34f else 0f)
                KomaText(" 周${weekdayLabel(block.date)}",size=size,lineHeight=1.2f,weight=FontWeight.SemiBold,color=c.ink5)
            }
            Spacer(Modifier.weight(1f))
            if(block.courses.isNotEmpty()) {
                val first=block.courses.first().start
                KomaText("${block.courses.size} 站"+(first?.let{"，${formatTime(it)} 发车"}?:""),
                    Modifier.graphicsLayer{alpha=1-(p*2.4f).coerceIn(0f,1f);translationY=-8.dp.toPx()*e},size=12,weight=FontWeight.SemiBold,color=c.ink5,maxLines=1)
            }
        }
        if(block.stops.isEmpty()) KomaText("今天没有课，线路休整中",Modifier.padding(top=2.dp,bottom=6.dp),size=13.5f,weight=FontWeight.SemiBold,color=c.ink4)
        block.stops.forEachIndexed { i, stop -> when(stop) {
            is CourseRow->CourseSlot(stop,i==0,i==block.stops.lastIndex,minutes,viewIsToday)
            is GapStop->GapStopRow(stop,i==0,i==block.stops.lastIndex)
        } }
    }
}
