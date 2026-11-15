package moe.tnxg.koma.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
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
import moe.tnxg.koma.feature.home.profile.ProfileDestinationHost
import moe.tnxg.koma.feature.home.profile.MePage
import moe.tnxg.koma.feature.home.profile.parent
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.app.preferences.HomeDestination
import moe.tnxg.koma.platform.NotificationService
import moe.tnxg.koma.ui.platform.PlatformBackHandler
import moe.tnxg.koma.ui.component.media.LocalImageServices
import moe.tnxg.koma.ui.component.media.ProfilePhotoCache
import moe.tnxg.koma.feature.home.timeline.*
import moe.tnxg.koma.ui.theme.KomaTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.layer.GraphicsLayer
import moe.tnxg.koma.feature.home.scroll.HomeScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import moe.tnxg.koma.feature.home.week.WeekScreen
import moe.tnxg.koma.ui.component.page.LocalPageActive
import moe.tnxg.koma.ui.component.page.pageAnimationSpec

@Composable
fun HomeScreen(snapshot:TimetableSnapshot,startDate:String,store:PreferenceStore,notifications:NotificationService?,
    onReimport:()->Unit,onEditStart:()->Unit,onLogout:()->Unit) {
    val c=KomaTheme.colors
    val density=LocalDensity.current
    val backdrop=rememberLayerBackdrop { drawRect(c.bg);drawContent() }
    val layer=backdrop.graphicsLayer
    val images=LocalImageServices.current
    LaunchedEffect(images,store.coverPhoto,store.avatarPhoto){
        images?.let { ProfilePhotoCache.preload(it,store.coverPhoto,store.avatarPhoto) }
    }
    DisposableEffect(Unit){onDispose{ProfilePhotoCache.clear()}}
    val scope=rememberCoroutineScope()
    var todayDate by remember { mutableStateOf(today()) }
    var minutes by remember { mutableStateOf(nowMinutes()) }
    LaunchedEffect(Unit) { while(true) { delay(30_000);todayDate=today();minutes=nowMinutes() } }
    val termStart=remember(startDate){LocalDate.parse(startDate)}
    val termEnd=remember(termStart,snapshot.semester.totalWeeks){addDays(termStart,snapshot.semester.totalWeeks*7-1)}
    fun clamp(date:LocalDate)=date.coerceIn(termStart,termEnd)
    var selected by remember(snapshot,startDate){mutableStateOf(clamp(todayDate))}
    val prefs=store.value
    var weekly by rememberSaveable { mutableStateOf(prefs.defaultHome==HomeDestination.Week) }
    var profile by rememberSaveable { mutableStateOf(prefs.defaultHome==HomeDestination.Me) }
    var profilePage by rememberSaveable { mutableStateOf(MePage.Main) }
    val targetPage=if(profile)2 else if(weekly)1 else 0
    val pages=rememberPagerState(initialPage=targetPage,pageCount={3})
    val dockBackdrop=rememberLayerBackdrop{drawRect(c.bg);drawContent()}
    var weekRecenter by remember{mutableIntStateOf(0)}
    var weekOverlay by remember{mutableStateOf(false)}
    LaunchedEffect(targetPage,prefs.reduceMotion) {
        if(prefs.reduceMotion) pages.scrollToPage(targetPage)
        else pages.animateScrollToPage(targetPage,animationSpec=pageAnimationSpec(false))
    }
    PlatformBackHandler(profile) {
        if(profilePage!=MePage.Main) profilePage=profilePage.parent() else profile=false
    }
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
                if(changed && !prefs.reduceMotion)launch {enter.snapTo(.7f);enter.animateTo(1f,tween(180,easing=CubicBezierEasing(.22f,.9f,.24f,1f)))}
                scroll.jump(anchors[next.toString()]?:next.toString(),!prefs.reduceMotion)
            }
        }
    }
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Box(Modifier.fillMaxSize().zIndex(if(!profile && weekly && weekOverlay)1f else 0f).layerBackdrop(dockBackdrop)) {
        HorizontalPager(pages,Modifier.fillMaxSize(),
            userScrollEnabled=false,beyondViewportPageCount=2){page->
            if(page==0)Box(Modifier.fillMaxSize()){
                HomeTimeline(blocks,scroll,minutes,selected==todayDate,backdrop)
                HomeHeader(snapshot,startDate,selected,todayDate,minutes,scroll,layer){enter.value}
                // Hidden edge controls must not exclude the system back gesture over profile pages.
                if(!profile) BoxWithConstraints(Modifier.fillMaxSize()){
                    val islandHeight=if(ISLAND_MAX_HEIGHT<maxHeight*.44f)ISLAND_MAX_HEIGHT else maxHeight*.44f
                    DateIsland(days,counts,selected,todayDate,recenter,islandHeight,::jump,{calendar=true},layer,
                        Modifier.align(Alignment.BottomEnd).padding(bottom=maxHeight*.22f).navigationBarsPadding())
                }
            }else if(page==1){
                WeekScreen(snapshot,startDate,selected,todayDate,minutes,{if(weekly && !profile)selected=clamp(it)},
                    {profile=false;weekly=false;jump(it)},{profile=false;weekly=false;jump(todayDate)},weekly && !profile,weekRecenter,{weekOverlay=it})
            }else{
                CompositionLocalProvider(LocalPageActive provides profile) {
                    ProfileDestinationHost(profilePage,store,snapshot,startDate,notifications,
                        {profilePage=it},onEditStart,onReimport,onLogout)
                }
            }
        }
        }
        if(!profile || profilePage==MePage.Main) HomeDock(if(profile)2 else if(weekly)1 else 0,
            {profile=false;weekly=false;jump(todayDate)},
            {profile=false;jumpJob?.cancel();if(weekly)weekRecenter++ else{weekRecenter=0;weekly=true}},
            {profile=true;profilePage=MePage.Main},
            Modifier.align(Alignment.BottomCenter),page=dockBackdrop)
        DateSheet(calendar,selected,{jump(it);calendar=false},{calendar=false},"选一天，跳到那天的课程")
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
