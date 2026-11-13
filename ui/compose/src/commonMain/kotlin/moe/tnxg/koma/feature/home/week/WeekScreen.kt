package moe.tnxg.koma.feature.home.week

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.calendar.*
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.domain.week.buildWeekSchedule
import moe.tnxg.koma.feature.home.dock.HomeDock
import moe.tnxg.koma.feature.home.dock.dockPress
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.platform.PlatformBackHandler
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
internal fun WeekScreen(
    snapshot: TimetableSnapshot, startDate: String, selected: LocalDate, today: LocalDate, minutes: Int,
    onSelect: (LocalDate) -> Unit, onDay: (LocalDate) -> Unit, onToday: () -> Unit, onMe: () -> Unit,
) {
    val c = KomaTheme.colors
    val total = snapshot.semester.totalWeeks.coerceAtLeast(1)
    val initial = weekIndex(startDate, selected).coerceIn(1, total)
    val pager = rememberPagerState(initialPage = initial - 1, pageCount = { total })
    val week = pager.settledPage + 1
    val scope = rememberCoroutineScope()
    val schedule = remember(snapshot, startDate, week) { buildWeekSchedule(snapshot, startDate, week) }
    val count = remember(schedule) { schedule.days.sumOf { day -> day.groups.sumOf { it.courses.size } } }
    var selector by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<List<Course>>(emptyList()) }
    var pickedDate by remember { mutableStateOf(selected) }
    val currentWeek = weekIndex(startDate, today)
    val latestSelect by rememberUpdatedState(onSelect)
    val selectedWeekday = isoWeekday(selected)
    val arrow by animateFloatAsState(if (selector) 180f else 0f, tween(200))
    LaunchedEffect(week) {
        latestSelect(schedule.days[selectedWeekday - 1].date)
    }
    PlatformBackHandler(true, onToday)
    fun goTo(target: Int) {
        scope.launch { pager.animateScrollToPage((target - 1).coerceIn(0, total - 1), animationSpec = tween(220)) }
    }
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.dockPress { selector = !selector }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Bottom) {
                    KomaText("第 $week 周", size = 23, weight = FontWeight.ExtraBold, letterSpacing = -.6f)
                    KomaIcon(KomaGlyph.Down, if (selector) "收起周次选择" else "选择周次",
                        Modifier.padding(start = 4.dp, bottom = 4.dp).size(14.dp)
                            .graphicsLayer { rotationZ = arrow }, c.ink4)
                    KomaText("$count 节课", Modifier.padding(start = 7.dp, bottom = 3.dp),
                        size = 10.5f, weight = FontWeight.Medium, color = c.ink4)
                }
                Spacer(Modifier.width(12.dp).weight(1f))
                KomaText(snapshot.semester.name, size = 12.5f, weight = FontWeight.SemiBold, color = c.ink3, maxLines = 1)
            }
            WeekDayHeader(schedule.days, schedule.days[selectedWeekday - 1].date, today, week,
                { selector = true }, onDay)
            Box(Modifier.fillMaxWidth().weight(1f).navigationBarsPadding().padding(bottom = 54.dp)) {
                HorizontalPager(pager, Modifier.fillMaxSize(), key = { it }) { page ->
                    val data = remember(snapshot, startDate, page) { buildWeekSchedule(snapshot, startDate, page + 1) }
                    Box(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
                        WeekGrid(data, snapshot.timeGrid, today, minutes, Modifier.fillMaxSize()) { day, courses ->
                            pickedDate = day; picked = courses
                        }
                        if (data.days.all { it.groups.isEmpty() }) {
                            KomaText("本周暂无课程", Modifier.align(Alignment.Center).padding(bottom = 24.dp),
                                size = 13, color = c.ink3)
                        }
                    }
                }
            }
        }
        if (currentWeek in 1..total && week != currentWeek) {
            val shape = RoundedCornerShape(99.dp)
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp)
                .shadow(14.dp, shape, ambientColor = c.accent.copy(alpha = .22f), spotColor = c.accent.copy(alpha = .28f))
                .background(c.accent, shape).border(1.dp, Color.White.copy(alpha = .12f), shape).clip(shape)
                .semantics { contentDescription = "回到本周" }
                .dockPress { goTo(currentWeek) }.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                KomaIcon(KomaGlyph.Location, null, Modifier.size(13.dp), Color.White)
                KomaText("回本周", size = 11, weight = FontWeight.Bold, color = Color.White)
            }
        }
        HomeDock(true, onToday, { goTo(currentWeek.coerceIn(1, total)) }, onMe,
            Modifier.align(Alignment.BottomCenter), fade = false)
        WeekSelector(selector, snapshot, startDate, week, today, { selector = false; goTo(it) }, { selector = false })
        WeekCourseSheet(picked, pickedDate, snapshot.timeGrid) { picked = emptyList() }
    }
}
