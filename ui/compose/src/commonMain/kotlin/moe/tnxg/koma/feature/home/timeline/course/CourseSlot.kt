package moe.tnxg.koma.feature.home.timeline.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.domain.calendar.*
import moe.tnxg.koma.domain.timeline.*
import moe.tnxg.koma.domain.visual.courseVisual
import moe.tnxg.koma.feature.home.icon.HomeIcon
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme
import moe.tnxg.koma.app.preferences.LocalAppPreferences
import moe.tnxg.koma.app.preferences.ScheduleDensity

@Composable
internal fun CourseSlot(row: CourseRow, first: Boolean, last: Boolean, minutes: Int, viewIsToday: Boolean) {
    val c=KomaTheme.colors
    val prefs=LocalAppPreferences.current
    val visual=courseVisual(row.course.name)
    val tone=if(prefs.colorCourses)courseTone(visual.tone) else c.accent
    val padding=when(prefs.density){ScheduleDensity.Compact->8.dp;ScheduleDensity.Standard->12.dp;ScheduleDensity.Relaxed->18.dp}
    val past=row.state==RowState.Past
    val timeColor=if(past)c.ink5 else if(row.state==RowState.Now)tone else c.ink3
    val start = row.start
    val end = row.end
    val hint=when {
        row.state==RowState.Now && end!=null->"现在 ${formatTime(minutes)}，还剩 ${formatDuration(end-minutes)}"
        row.state==RowState.Next && start!=null->{
            val gap=start-minutes
            if(viewIsToday && gap in 1..60) "还有 ${formatDuration(gap)}，${formatTime(start)} 开始"
            else "下一站，${formatTime(start)} 开出"
        }
        else->null
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(46.dp).fillMaxHeight().padding(end=7.dp,top=8.5.dp,bottom=8.5.dp),
            verticalArrangement=Arrangement.SpaceBetween,horizontalAlignment=Alignment.End) {
            KomaText(row.start?.let(::formatTime).orEmpty(),size=11,lineHeight=1f,weight=FontWeight.SemiBold,color=timeColor)
            val period=if(row.course.startPeriod==row.course.endPeriod)"${row.course.startPeriod} 节" else "${row.course.startPeriod}–${row.course.endPeriod} 节"
            KomaText(period,size=10,lineHeight=1f,weight=FontWeight.SemiBold,color=if(row.state==RowState.Now)lerp(c.ink4,tone,.6f) else c.ink5)
            KomaText(row.end?.let(::formatTime).orEmpty(),size=11,lineHeight=1f,weight=FontWeight.SemiBold,color=timeColor)
        }
        CourseRail(row.state,tone,row.pct,first,last,Modifier.width(18.dp).fillMaxHeight())
        Box(Modifier.weight(1f).padding(vertical=7.dp)) {
            val shape=RoundedCornerShape(16.dp)
            val fill=if(past)Brush.linearGradient(listOf(c.surface2,c.surface2)) else Brush.linearGradient(listOf(lerp(c.surface,tone,.09f),lerp(c.surface,tone,.03f)))
            Column(Modifier.fillMaxWidth().background(fill,shape)
                .then(if(row.state==RowState.Now)Modifier.border(1.5.dp,tone.copy(alpha=.32f),shape) else Modifier)
                .drawWithContent { drawContent(); drawCircle(c.bg,6.dp.toPx(),Offset(0f,size.height/2)); drawCircle(c.bg,6.dp.toPx(),Offset(size.width,size.height/2)) }
                .padding(start=16.dp,end=14.dp,top=padding,bottom=padding)) {
                KomaText(row.course.name,Modifier.padding(end=26.dp),size=15.5f,lineHeight=1.28f,weight=FontWeight.Bold,color=if(past)c.ink4 else c.ink,letterSpacing=-.155f)
                row.course.location?.takeIf{prefs.showRoom && it.isNotEmpty()}?.let { text ->
                    Row(Modifier.padding(top=4.dp,end=24.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        HomeIcon(KomaGlyph.Location,if(past)c.ink5 else lerp(c.ink4,tone,.55f),Modifier.size(13.dp))
                        KomaText(text,size=12.5f,weight=FontWeight.Medium,color=if(past)c.ink5 else c.ink3,maxLines=1)
                    }
                }
                row.course.teacher?.takeIf{prefs.showTeacher && it.isNotEmpty()}?.let { text ->
                    Row(Modifier.padding(top=3.dp,end=24.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        HomeIcon(KomaGlyph.Person,if(past)c.ink5 else lerp(c.ink4,tone,.55f),Modifier.size(13.dp))
                        KomaText(text,size=12.5f,weight=FontWeight.Medium,color=if(past)c.ink5 else c.ink3,maxLines=1)
                    }
                }
                if(hint!=null) KomaText(hint,Modifier.padding(top=9.dp).background(if(row.state==RowState.Now)tone else tone.copy(alpha=.12f),RoundedCornerShape(8.dp)).padding(horizontal=8.dp,vertical=3.dp),
                    size=11.5f,weight=if(row.state==RowState.Now)FontWeight.Bold else FontWeight.SemiBold,color=if(row.state==RowState.Now)c.surface else lerp(c.ink,tone,.75f))
            }
            HomeIcon(courseIcon(visual.icon),if(past)c.ink5 else tone.copy(alpha=.72f),Modifier.align(Alignment.TopEnd).padding(top=12.dp,end=12.dp).size(24.dp).graphicsLayer{rotationZ=-4f})
        }
    }
}
