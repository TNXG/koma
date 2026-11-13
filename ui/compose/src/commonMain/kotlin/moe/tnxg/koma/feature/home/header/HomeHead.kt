package moe.tnxg.koma.feature.home.header

import androidx.compose.foundation.background
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.calendar.*
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.domain.timeline.describeDay
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
internal fun HomeHead(snapshot: TimetableSnapshot, startDate: String, date: LocalDate,
    today: LocalDate, minutes: Int, motion: ()->HeaderMotion, modifier: Modifier = Modifier,
    titleEnter: ()->Float, source: GraphicsLayer,
    flying: DateGeometry? = null, incomingTop: ()->Float,
    onHeight:(Float)->Unit,
) {
    val density=LocalDensity.current
    var contentHeight by remember { mutableFloatStateOf(90f) }
    var headOrigin by remember { mutableStateOf(Offset.Unspecified) }
    var stackOrigin by remember { mutableStateOf(Offset.Unspecified) }
    var titleHeight by remember(density) { mutableFloatStateOf(0f) }
    val target=remember(titleHeight,stackOrigin,headOrigin,density) {
        if(headOrigin.isSpecified && stackOrigin.isSpecified && titleHeight>0f) with(density) {
            DateGeometry((stackOrigin.x-headOrigin.x).toDp().value,
                (stackOrigin.y-headOrigin.y).toDp().value,titleHeight,26f)
        } else null
    }
    val incoming by remember(motion) { derivedStateOf { motion().incoming } }
    val docks by remember(motion) { derivedStateOf { motion().docks } }
    val progress=remember(motion) { { motion().progress } }
    val incomingDate=remember(incoming) { incoming?.let { LocalDate.parse(it) } }
    Box(modifier.onGloballyPositioned { headOrigin=it.positionInRoot() }) {
        TopVeil(source, { motion().veil }, contentHeight+28f)
        Box(Modifier.fillMaxWidth().onGloballyPositioned {
            val height=with(density){it.size.height.toDp().value}
            if(contentHeight!=height)contentHeight=height
            onHeight(height)
        }.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp)) {
            // Measure outside every animated layer so the target never chases the current title.
            Box(Modifier.fillMaxWidth().onGloballyPositioned { stackOrigin=it.positionInRoot() }) {
                Box(Modifier.fillMaxWidth().graphicsLayer {
                    val enter=titleEnter()
                    alpha=enter
                    translationY=(1-enter)*10.dp.toPx()
                    renderEffect=if(enter<1f) {
                        val radius=4.dp.toPx()*(1-enter)
                        BlurEffect(radius,radius,TileMode.Decal)
                    } else null
                }) {
                    HeaderLayer(snapshot,startDate,date,today,minutes,progress,false,docks) {
                        titleHeight=with(density){it.size.height.toDp().value}
                    }
                    incomingDate?.let { incoming ->
                        Box(Modifier.matchParentSize()) {
                            HeaderLayer(snapshot,startDate,incoming,today,minutes,progress,true,docks)
                        }
                    }
                }
            }
        }
        if(docks && flying!=null && incomingDate!=null && target!=null)
            LiveFlyingDate(incomingDate,flying,target,incomingTop,progress)
    }
}

@Composable
private fun LiveFlyingDate(date:LocalDate,source:DateGeometry,target:DateGeometry,
    rowTop:()->Float,progress:()->Float,
) {
    // Only the flying glyphs need composition for their interpolated text color.
    FlyingDate(date,source,target,rowTop(),progress())
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun HeaderLayer(snapshot: TimetableSnapshot, startDate: String, date: LocalDate,
    today: LocalDate, minutes: Int, progress: ()->Float, incoming: Boolean, docks: Boolean,
    onTitlePositioned: ((androidx.compose.ui.layout.LayoutCoordinates)->Unit)? = null,
) {
    val c = KomaTheme.colors
    val p = progress()
    val summary = remember(snapshot,startDate,date,today,minutes) {
        describeDay(snapshot,startDate,date,today,minutes)
    }
    Column {
        Box(if(onTitlePositioned!=null) Modifier.onGloballyPositioned(onTitlePositioned) else Modifier) {
            Row(Modifier.graphicsLayer {
                alpha = if (!incoming) 1-smooth(0f,.55f,p) else if(docks) smooth(.72f,1f,p) else smooth(.25f,1f,p)
                translationY = (if(!incoming) -10*p else if(docks) 0f else (1-p)*12).dp.toPx()
            }) {
                KomaText(formatMonthDay(date), size=26, lineHeight=1.2f, weight=FontWeight.ExtraBold, letterSpacing=-.52f)
                KomaText(" 周${weekdayLabel(date)}", size=26, lineHeight=1.2f, weight=FontWeight.Bold, color=c.ink5, letterSpacing=-.52f)
            }
        }
        FlowRow(Modifier.padding(top=8.dp).graphicsLayer {
            alpha=if(incoming) smooth(.5f,1f,p) else 1-smooth(0f,.5f,p)
            translationY=(if(incoming) (1-p)*6 else -6*p).dp.toPx()
        }, horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            summary.parts.forEachIndexed { i, part ->
                if(i>0) Box(Modifier.width(1.dp).height(12.dp).background(c.line))
                KomaText(part,size=12.5f,weight=FontWeight.SemiBold,color=if(summary.empty && i==summary.parts.lastIndex)c.ink4 else c.ink2)
            }
        }
    }
}
