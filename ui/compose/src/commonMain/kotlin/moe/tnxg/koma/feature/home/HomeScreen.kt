package moe.tnxg.koma.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.calendar.*
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.domain.timeline.*
import moe.tnxg.koma.feature.calendar.DateSheet
import moe.tnxg.koma.feature.home.date.DateIsland
import moe.tnxg.koma.feature.home.date.ISLAND_MAX_HEIGHT
import moe.tnxg.koma.feature.home.dock.HomeDock
import moe.tnxg.koma.feature.home.header.*
import moe.tnxg.koma.feature.home.profile.MeDrawer
import moe.tnxg.koma.feature.home.timeline.*
import moe.tnxg.koma.ui.theme.KomaTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.layer.GraphicsLayer
import moe.tnxg.koma.feature.home.scroll.HomeScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import moe.tnxg.koma.feature.home.week.WeekScreen

@Composable
fun HomeScreen(snapshot:TimetableSnapshot,startDate:String,onReimport:()->Unit,onEditStart:()->Unit,onLogout:()->Unit) {
    val c=KomaTheme.colors
    val density=LocalDensity.current
    val backdrop=rememberGraphicsLayer()
    val scope=rememberCoroutineScope()
    var todayDate by remember { mutableStateOf(today()) }
    var minutes by remember { mutableStateOf(nowMinutes()) }
    LaunchedEffect(Unit) { while(true) { delay(30_000);todayDate=today();minutes=nowMinutes() } }
    val termStart=remember(startDate){LocalDate.parse(startDate)}
    val termEnd=remember(termStart,snapshot.semester.totalWeeks){addDays(termStart,snapshot.semester.totalWeeks*7-1)}
    fun clamp(date:LocalDate)=date.coerceIn(termStart,termEnd)
    var selected by remember(snapshot,startDate){mutableStateOf(clamp(todayDate))}
    var weekly by rememberSaveable { mutableStateOf(false) }
    var profile by remember {mutableStateOf(false)}
    var calendar by remember {mutableStateOf(false)}
    var recenter by remember {mutableIntStateOf(0)}
    val enter=remember{Animatable(1f)}
    var jumpJob by remember { mutableStateOf<Job?>(null) }
    val blocks=remember(snapshot,startDate,todayDate,minutes){buildTimeline(snapshot,startDate,todayDate,minutes)}
    val anchors=remember(blocks){blockAnchors(blocks)}
    val counts=remember(blocks){countCoursesByDay(blocks)}
    val days=remember(termStart,termEnd){List(daysBetween(termStart,termEnd)+1){addDays(termStart,it)}}
    val initialIndex=remember(snapshot,startDate) {
        val anchor=anchors[selected.toString()]?:selected.toString()
        blocks.indexOfFirst { it !is WeekBlock && it.anchor==anchor }.coerceAtLeast(0)
    }
    val list=rememberLazyListState(initialFirstVisibleItemIndex=initialIndex)
    val scroll=remember(snapshot,startDate,list,density){HomeScrollState(list,blocks,density)}
    SideEffect { scroll.update(blocks) }
    LaunchedEffect(scroll) {
        snapshotFlow { Triple(scroll.motion.current,scroll.navigation.running,scroll.initialPositioned) }
            .collect { (current,gliding,positioned) ->
                if(!weekly && positioned && !gliding && current!=null && anchors[selected.toString()]!=current)
                    selected=LocalDate.parse(current)
            }
    }
    val selectedAnchor=anchors[selected.toString()]?:selected.toString()
    val selectedHeight=scroll.dateHeights[selectedAnchor]
    LaunchedEffect(scroll,selectedAnchor,selectedHeight) {
        if(!scroll.initialPositioned && (blocks[initialIndex] !is DayBlock || selectedHeight!=null)) {
            scroll.jump(selectedAnchor,false)
            scroll.initialPositioned=true
        }
    }
    fun jump(day:LocalDate) {
        val next=clamp(day);val changed=next!=selected
        selected=next;recenter++
        jumpJob?.cancel()
        jumpJob=scope.launch {
            scroll.navigation.run {
                if(changed)launch {enter.snapTo(.7f);enter.animateTo(1f,tween(180,easing=CubicBezierEasing(.22f,.9f,.24f,1f)))}
                scroll.jump(anchors[next.toString()]?:next.toString(),true)
            }
        }
    }
    Box(Modifier.fillMaxSize().background(c.bg)) {
        if (!weekly) {
            HomeTimeline(blocks,scroll,minutes,selected==todayDate,backdrop)
            HomeHeader(snapshot,startDate,selected,todayDate,minutes,scroll,backdrop) { enter.value }
            HomeDock(false,{jump(todayDate)},{jumpJob?.cancel();weekly=true},{profile=true},Modifier.align(Alignment.BottomCenter))
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val islandHeight=if(ISLAND_MAX_HEIGHT<maxHeight*.44f)ISLAND_MAX_HEIGHT else maxHeight*.44f
                DateIsland(days,counts,selected,todayDate,recenter,islandHeight,::jump,{calendar=true},backdrop,
                    Modifier.align(Alignment.BottomEnd).padding(bottom=maxHeight*.22f).navigationBarsPadding())
            }
        } else {
            WeekScreen(snapshot,startDate,selected,todayDate,minutes,{selected=clamp(it)},
                {weekly=false;jump(it)},{weekly=false;jump(todayDate)},{profile=true})
        }
        DateSheet(calendar,selected,{jump(it);calendar=false},{calendar=false},"选一天，跳到那天的课程")
        MeDrawer(profile,snapshot.studentName,snapshot.semester.name,startDate,{profile=false},
            {profile=false;onReimport()},{profile=false;onEditStart()},{profile=false;onLogout()})
    }
}

@Composable
private fun HomeHeader(snapshot:TimetableSnapshot,startDate:String,date:LocalDate,
    today:LocalDate,minutes:Int,scroll:HomeScrollState,backdrop:GraphicsLayer,titleEnter:()->Float,
) {
    val incoming by remember(scroll) { derivedStateOf { scroll.motion.incoming } }
    HomeHead(snapshot,startDate,date,today,minutes,{ scroll.motion },Modifier.fillMaxWidth(),
        titleEnter,backdrop,incoming?.let { scroll.dateGeometry[it] },{ scroll.topOf(incoming) }) {
        if(scroll.headHeight!=it)scroll.headHeight=it
    }
}
