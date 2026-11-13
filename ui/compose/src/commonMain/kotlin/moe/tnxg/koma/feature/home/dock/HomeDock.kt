// Layering adapted from compose-miuix-ui / SukiSU FloatingBottomBar (Apache-2.0).
package moe.tnxg.koma.feature.home.dock

import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import moe.tnxg.koma.feature.home.icon.HomeIcon
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/** 底板采样页面；镜片采样页面与已玻璃化的强调色按钮副本。 */
@Composable
internal fun HomeDock(selected:Int, onToday:()->Unit, onWeek:()->Unit, onMe:()->Unit,
    modifier: Modifier=Modifier, page: LayerBackdrop) {
    val c=KomaTheme.colors
    val dark=c.bg.luminance()<.1f
    val density=LocalDensity.current
    val pill=remember{CircleShape}
    val surface=c.surface.copy(alpha=.4f)
    val faceStroke=rememberDockHighlight(-45f)
    val lensStroke=rememberDockHighlight(90f)
    val tabs=rememberLayerBackdrop()
    val combined=rememberCombinedBackdrop(page,tabs)
    val shader=isRuntimeShaderSupported()
    val target=selected
    val latestTarget by rememberUpdatedState(target)
    val scope=rememberCoroutineScope()
    val motion=remember(scope){DockMotion(scope,target)}
    val progress=motion.progress
    val isLtr=LocalLayoutDirection.current==LayoutDirection.Ltr
    var tabWidth by remember{mutableFloatStateOf(0f)}
    var totalWidth by remember{mutableFloatStateOf(0f)}
    val rubberBand=with(density){4.dp.toPx()}
    val panelOffset by remember(motion,rubberBand){
        derivedStateOf {
            val fraction=if(totalWidth>0f)(motion.offset.value/totalWidth).coerceIn(-1f,1f) else 0f
            rubberBand*fraction.sign*EaseOut.transform(abs(fraction))
        }
    }
    val onActivate by rememberUpdatedState<(Int)->Unit>({ chosen ->
        motion.animateTo(chosen)
        when(chosen){0->onToday();1->onWeek();2->onMe()}
    })
    LaunchedEffect(target){
        if(!motion.dragging && motion.index.targetValue!=target.toFloat())motion.animateTo(target)
    }

    Box(modifier.fillMaxWidth()) {
        Box(Modifier.align(Alignment.BottomCenter).width(236.dp).navigationBarsPadding()
            .padding(bottom=8.dp),contentAlignment=Alignment.CenterStart) {
            Row(
                Modifier.onGloballyPositioned{
                    totalWidth=it.size.width.toFloat()
                    tabWidth=((totalWidth-with(density){8.dp.toPx()})/3f).coerceAtLeast(0f)
                }.selectableGroup()
                    .graphicsLayer{translationX=panelOffset}
                    .dropShadow(pill,Shadow(radius=10.dp,color=Color.Black,alpha=if(dark).2f else .1f))
                    .then(
                        if(shader) Modifier.dockGlass(page,pill,surface,
                            highlightOf={faceStroke.value.copy(alpha=.75f)},layerBlock={
                                val scale=1f+16.dp.toPx()/size.width.coerceAtLeast(1f)*progress.value
                                scaleX=scale;scaleY=scale
                            })
                        else Modifier.background(c.surface,pill)
                    )
                    .pointerInput(motion,tabWidth,isLtr){
                        fun indexAt(x:Float):Int {
                            if(tabWidth<=0f)return latestTarget
                            val logicalX=if(isLtr)x else totalWidth-x
                            return ((logicalX-4.dp.toPx())/tabWidth).toInt().coerceIn(0,2)
                        }
                        inspectDockGestures(
                            onStart={position->motion.press();motion.moveTo(indexAt(position.x).toFloat())},
                            onEnd={onActivate(motion.index.targetValue.roundToInt().coerceIn(0,2))},
                            onCancel={motion.moveTo(latestTarget.toFloat());motion.release()},
                        ){change,delta->
                            if(tabWidth>0f && change.position.x in 0f..totalWidth &&
                                change.previousPosition.x in 0f..totalWidth){
                                motion.moveTo(motion.index.targetValue+delta.x/tabWidth*if(isLtr)1f else -1f)
                                motion.dragBy(delta.x)
                            }
                        }
                    }
                    .height(64.dp).padding(4.dp),
                verticalAlignment=Alignment.CenterVertically,
            ) {
                DockTab(KomaGlyph.Home,"今天",selected==0,shader,Modifier.weight(1f)){onActivate(0)}
                DockTab(KomaGlyph.Calendar,"课表",selected==1,shader,Modifier.weight(1f)){onActivate(1)}
                DockTab(KomaGlyph.Person,"我的",selected==2,shader,Modifier.weight(1f)){onActivate(2)}
            }
            if(shader) {
                // alpha 在录制节点外：屏幕上隐藏，镜片仍可读取完整底板和强调色按钮。
                Row(Modifier.fillMaxWidth().clearAndSetSemantics{}.alpha(0f).layerBackdrop(tabs)
                    .graphicsLayer{translationX=panelOffset}
                    .dockGlass(page,pill,surface,highlightOf={null})
                    .height(56.dp).padding(horizontal=4.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    DockTabContent(KomaGlyph.Home,"今天",c.accent,Modifier.weight(1f),{1f+.2f*progress.value})
                    DockTabContent(KomaGlyph.Calendar,"课表",c.accent,Modifier.weight(1f),{1f+.2f*progress.value})
                    DockTabContent(KomaGlyph.Person,"我的",c.accent,Modifier.weight(1f),{1f+.2f*progress.value})
                }
            }
            if(shader && tabWidth>0f) {
                Box(Modifier.padding(horizontal=4.dp)
                    .graphicsLayer{translationX=motion.index.value*tabWidth*if(isLtr)1f else -1f;translationX+=panelOffset}
                    .indicatorGlass(combined,pill,dark,{progress.value},
                        {motion.scaleX.value/(1f-(motion.index.velocity/10f*.75f).coerceIn(-.2f,.2f))},
                        {motion.scaleY.value*(1f-(motion.index.velocity/10f*.25f).coerceIn(-.2f,.2f))}){lensStroke.value}
                    .innerShadow(pill){
                        radius=8.dp.toPx()*progress.value
                        color=Color.Black.copy(alpha=.15f)
                        alpha=progress.value
                    }
                    .height(56.dp).width(with(density){tabWidth.toDp()}))
            }
        }
    }
}

@Composable
private fun DockTab(icon: KomaGlyph,label:String,selected:Boolean,glass:Boolean,
    modifier:Modifier,onClick:()->Unit) {
    val c=KomaTheme.colors
    DockTabContent(icon,label,if(!glass && selected)c.accent else c.ink,
        modifier.semantics(mergeDescendants=true){
            this.selected=selected
            role=Role.Tab
            onClick{onClick();true}
        }.onKeyEvent{event->
            if(event.key==Key.Enter || event.key==Key.NumPadEnter || event.key==Key.Spacebar){
                if(event.type==KeyEventType.KeyUp)onClick()
                true
            }else false
        }.focusable()
            .then(if(!glass && selected)Modifier.background(c.accent.copy(alpha=.15f),CircleShape) else Modifier))
}

@Composable
private fun DockTabContent(icon:KomaGlyph,label:String,color:Color,modifier:Modifier,
    scale:()->Float={1f}) {
    Column(modifier.fillMaxHeight().graphicsLayer{scaleX=scale();scaleY=scale()}
        .padding(horizontal=4.dp,vertical=6.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        HomeIcon(icon,color,Modifier.size(19.dp))
        KomaText(label,Modifier.padding(top=2.dp),size=9.5f,weight=FontWeight.Bold,color=color)
    }
}
