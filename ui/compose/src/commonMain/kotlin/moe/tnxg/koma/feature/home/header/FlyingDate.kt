package moe.tnxg.koma.feature.home.header

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.calendar.formatMonthDay
import moe.tnxg.koma.domain.calendar.weekdayLabel
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

internal data class DateGeometry(val x:Float,val y:Float,val height:Float,val font:Float)

@Composable
internal fun FlyingDate(date:LocalDate,geometry:DateGeometry,target:DateGeometry,rowTop:Float,p:Float) {
    val c=KomaTheme.colors
    val e=dateFlightOffset(0f,1f,p)
    val density=LocalDensity.current
    val sourceFont=with(density){geometry.font.sp.toPx()}
    val targetFont=with(density){target.font.sp.toPx()}
    Row(Modifier.graphicsLayer {
        transformOrigin=TransformOrigin(0f,.5f)
        // The source date is relative to its row; both rowTop and target are viewport-local.
        translationX=dateFlightOffset(target.x+geometry.x,target.x,p).dp.toPx()
        val sourceCenter=rowTop+geometry.y+geometry.height/2
        val center=dateFlightOffset(sourceCenter,target.y+target.height/2,p)
        translationY=(center-geometry.height/2).dp.toPx()
        scaleX=dateFlightScale(sourceFont,targetFont,p);scaleY=scaleX
        alpha=1-smooth(.82f,1f,p)
    }) {
        KomaText(formatMonthDay(date),size=geometry.font,lineHeight=1.2f,weight=if(geometry.font==17f)FontWeight.ExtraBold else FontWeight.Bold,
            color=if(geometry.font==17f)c.ink else lerp(c.ink3,c.ink,e),letterSpacing=if(geometry.font==17f)-.34f else 0f)
        KomaText(" 周${weekdayLabel(date)}",size=geometry.font,lineHeight=1.2f,weight=FontWeight.SemiBold,color=c.ink5)
    }
}
