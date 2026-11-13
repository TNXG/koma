package moe.tnxg.koma.feature.home.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.feature.home.icon.HomeIcon
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
internal fun HomeDock(weekly:Boolean, onToday:()->Unit, onWeek:()->Unit, onMe:()->Unit, modifier: Modifier=Modifier, fade:Boolean=true) {
    val c=KomaTheme.colors
    val dark=c.bg.luminance()<.1f
    val dock=if(dark)listOf(Color(0xff202126).copy(alpha=.98f),Color(0xff1e1f24).copy(alpha=.98f))
        else listOf(Color(0xfffafafa).copy(alpha=.98f),Color(0xfffbfbfb).copy(alpha=.98f))
    val edge=if(dark)Color.White.copy(alpha=.06f) else c.surface
    Box(modifier.fillMaxWidth()) {
        if (fade) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(
            0f to c.bg.copy(alpha=0f),.34f to c.bg.copy(alpha=.85f),.58f to c.bg,1f to c.bg))
            .navigationBarsPadding().height(120.dp))
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth(.92f).navigationBarsPadding()
            .padding(bottom = 8.dp).dockSurface(dark,dock,edge,99).padding(3.dp)) {
            DockTab(KomaGlyph.Home,"今天",!weekly,Modifier.weight(1f),onToday)
            DockTab(KomaGlyph.Calendar,"课表",weekly,Modifier.weight(1f),onWeek)
            DockTab(KomaGlyph.Settings,"我的",false,Modifier.weight(1f),onMe)
        }
    }
}

private fun Modifier.dockSurface(dark:Boolean, colors:List<Color>,edge:Color,radius:Int):Modifier {
    val shape=RoundedCornerShape(radius.dp)
    return then(if(dark)Modifier else Modifier.shadow(20.dp,shape,ambientColor=Color(0xfff2f2f1),spotColor=Color(0xfff2f2f1)))
        .background(Brush.verticalGradient(colors),shape).border(1.dp,edge,shape)
}

@Composable
private fun DockTab(icon: KomaGlyph,label:String,selected:Boolean,modifier:Modifier,onClick:()->Unit) {
    val c=KomaTheme.colors;val color=if(selected)c.accent else c.ink
    Column(modifier.background(if(selected)c.accentSoft else Color.Transparent,RoundedCornerShape(99.dp)).dockPress(onClick=onClick)
        .padding(horizontal=4.dp,vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)) {
        HomeIcon(icon,color,Modifier.size(19.dp))
        KomaText(label,size=9.5f,weight=FontWeight.Bold,color=color)
    }
}
