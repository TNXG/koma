package moe.tnxg.koma.feature.home.date

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.calendar.daysBetween
import moe.tnxg.koma.domain.calendar.weekdayLabel
import moe.tnxg.koma.feature.home.dock.dockPress
import moe.tnxg.koma.feature.home.icon.HomeIcon
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

internal val ISLAND_HANDLE_WIDTH = 26.dp
internal val ISLAND_BODY_WIDTH = 58.dp
internal val ISLAND_MAX_HEIGHT = 320.dp

private const val IDLE_COLLAPSE_MILLIS = 6_000L

/**
 * Floating date control docked to the right edge. Collapsed it leaves only a handle showing the
 * selected day; expanded it reveals a vertically snapping date strip plus today/calendar actions.
 */
@Composable
internal fun DateIsland(
    days: List<LocalDate>, counts: Map<String, Int>, selected: LocalDate, today: LocalDate,
    recenter: Int, height: Dp, onPick: (LocalDate) -> Unit, onCalendar: () -> Unit,
    backdrop: GraphicsLayer?, modifier: Modifier = Modifier,
) {
    val c = KomaTheme.colors
    val density = LocalDensity.current
    val dark = c.bg.luminance() < .1f
    var open by remember { mutableStateOf(false) }
    var idle by remember { mutableIntStateOf(0) }
    fun poke() { idle++ }
    LaunchedEffect(open, idle) {
        if (!open) return@LaunchedEffect
        delay(IDLE_COLLAPSE_MILLIS)
        open = false
    }
    LaunchedEffect(selected) { if (open) poke() }

    val slide by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(420, easing = CubicBezierEasing(.22f, .9f, .24f, 1f)),
    )
    val shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
    val edge = if (dark) Color.White.copy(alpha = .07f) else c.surface
    val handleTint = c.surface.copy(alpha = if (dark) .94f else .88f)
    val bodyTint = c.surface.copy(alpha = if (dark) .9f else .74f)

    // The row slides right while collapsed; the blur must follow the translated position.
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.width(ISLAND_HANDLE_WIDTH + ISLAND_BODY_WIDTH).height(height)
        .onGloballyPositioned { origin = it.positionInRoot() }) {
        Row(
            Modifier.fillMaxHeight().fillMaxWidth()
                .graphicsLayer { translationX = (1f - slide) * ISLAND_BODY_WIDTH.toPx() }
                .then(
                    if (dark) Modifier else Modifier.shadow(18.dp, shape,
                        ambientColor = Color(0xfff2f2f1), spotColor = Color(0xfff2f2f1))
                )
                .clip(shape)
                .islandBackdrop(backdrop) {
                    val shift = with(density) { (ISLAND_BODY_WIDTH * (1f - slide)).toPx() }
                    Offset(origin.x + shift, origin.y)
                }
                .background(Brush.horizontalGradient(listOf(handleTint, bodyTint)), shape)
                .border(1.dp, edge, shape),
        ) {
            IslandHandle(selected, today, open) { open = !open; poke() }
            IslandBody(days, counts, selected, today, recenter, open, ::poke, onPick, onCalendar)
        }
    }
}

@Composable
private fun IslandHandle(date: LocalDate, today: LocalDate, expanded: Boolean, onToggle: () -> Unit) {
    val c = KomaTheme.colors
    val hint = if (expanded) "收起日期" else "展开日期"
    val label = "${date.monthNumber}月${date.dayOfMonth}日${if (date == today) "今天" else ""}，$hint"
    Box(Modifier.width(ISLAND_HANDLE_WIDTH).fillMaxHeight()
        .semantics { contentDescription = label; role = Role.Button }
        .dockPress(.97f, onToggle)) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)) {
            KomaText(date.dayOfMonth.toString(), size = 14, lineHeight = 1.15f,
                weight = FontWeight.ExtraBold, letterSpacing = -.28f)
            KomaText(if (date == today) "今" else weekdayLabel(date), size = 9, lineHeight = 1f,
                weight = FontWeight.Bold, color = c.ink4)
            // Chevron states the handle's direction: it opens leftwards, then folds back.
            HomeIcon(if (expanded) KomaGlyph.Right else KomaGlyph.Left, c.ink3,
                Modifier.padding(top = 4.dp).size(13.dp), null)
        }
        if (date != today) {
            Box(Modifier.align(Alignment.TopCenter).offset(y = 14.dp).size(4.dp)
                .background(c.accent, CircleShape))
        }
    }
}

@Composable
private fun IslandBody(
    days: List<LocalDate>, counts: Map<String, Int>, selected: LocalDate, today: LocalDate,
    recenter: Int, expanded: Boolean, onPoke: () -> Unit,
    onPick: (LocalDate) -> Unit, onCalendar: () -> Unit,
) {
    val c = KomaTheme.colors
    Column(Modifier.width(ISLAND_BODY_WIDTH).fillMaxHeight()
        .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 9.dp)) {
        // Full-width actions at both ends of the strip, so neither target is a tiny tap area.
        IslandAction(Modifier.fillMaxWidth().height(38.dp), onClick = { onPoke(); onCalendar() }) {
            HomeIcon(KomaGlyph.Calendar, c.ink4, Modifier.size(17.dp), "打开月历")
        }
        Spacer(Modifier.height(7.dp))
        IslandStrip(days, counts, selected, today, recenter, expanded, onPoke, onPick,
            Modifier.weight(1f).fillMaxWidth())
        Spacer(Modifier.height(7.dp))
        IslandAction(Modifier.fillMaxWidth().height(38.dp), active = selected != today,
            onClick = { onPoke(); onPick(today) }) {
            KomaText("今", size = 13, lineHeight = 1f, weight = FontWeight.ExtraBold,
                color = if (selected != today) c.accent else c.ink4)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            KomaText(offsetLabel(today, selected), size = 9, lineHeight = 1f,
                weight = FontWeight.SemiBold, color = c.ink5)
        }
    }
}

@Composable
private fun IslandAction(
    modifier: Modifier, active: Boolean = false, onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val c = KomaTheme.colors
    Box(modifier.fillMaxHeight()
        .background(if (active) c.accentSoft else c.surface2.copy(alpha = .62f), RoundedCornerShape(10.dp))
        .dockPress(.94f, onClick), contentAlignment = Alignment.Center) { content() }
}

private fun offsetLabel(today: LocalDate, selected: LocalDate): String {
    val diff = daysBetween(today, selected)
    return when {
        diff == 0 -> "今天"
        diff > 0 -> "+$diff 天"
        else -> "$diff 天"
    }
}
