package moe.tnxg.koma.feature.home.date

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.ui.component.scroll.ScrollAnimationState
import kotlin.math.abs

@Composable
internal fun IslandStrip(
    days: List<LocalDate>, counts: Map<String, Int>, selected: LocalDate, today: LocalDate,
    recenter: Int, expanded: Boolean, onPoke: () -> Unit, onPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()
    val snap = rememberSnapFlingBehavior(state)
    val follow = remember { DateFollowState() }
    val automatic = remember { ScrollAnimationState() }
    val haptics = LocalHapticFeedback.current
    val selectedIndex = remember(days, selected) { days.indexOf(selected) }
    var lastRecenter by remember { mutableIntStateOf(recenter) }
    var lastPicked by remember { mutableStateOf(selected.toString()) }

    // The selected chip's height is known after the first measure; center it once items exist.
    suspend fun center(animated: Boolean) {
        if (selectedIndex < 0) return
        val layout = state.layoutInfo
        val item = layout.visibleItemsInfo.firstOrNull()?.size ?: return
        val offset = -((layout.viewportStartOffset + layout.viewportEndOffset - item) / 2).coerceAtMost(0)
        automatic.run {
            if (animated) state.animateScrollToItem(selectedIndex, offset) else state.scrollToItem(selectedIndex, offset)
        }
    }

    LaunchedEffect(expanded) {
        if (!expanded) return@LaunchedEffect
        snapshotFlow { state.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
        center(animated = false)
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress to automatic.running }.collect { (moving, centering) ->
            follow.scrollChanged(moving, centering, Clock.System.now().toEpochMilliseconds())
        }
    }
    LaunchedEffect(selectedIndex, recenter, expanded) {
        if (!expanded || selectedIndex < 0) return@LaunchedEffect
        if (recenter != lastRecenter) {
            follow.requestRecenter()
            lastRecenter = recenter
        }
        days.getOrNull(selectedIndex)?.let { lastPicked = it.toString() }
        // Let an active gesture finish before pulling the strip back to the selected day.
        while (true) {
            val hold = snapshotFlow {
                if (state.isScrollInProgress) null
                else follow.remainingHoldMillis(Clock.System.now().toEpochMilliseconds())
            }.first { it != null } ?: continue
            if (hold == 0L) break
            delay(hold)
        }
        center(animated = true)
    }
    LaunchedEffect(state, expanded) {
        snapshotFlow { state.isScrollInProgress }.collect { moving ->
            if (moving) {
                onPoke()
                return@collect
            }
            if (!expanded || automatic.running) return@collect
            val layout = state.layoutInfo
            val middle = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            val best = layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) } ?: return@collect
            val day = days.getOrNull(best.index) ?: return@collect
            if (day.toString() == lastPicked) return@collect
            lastPicked = day.toString()
            if (day == selected) return@collect
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onPoke()
            onPick(day)
        }
    }

    LazyColumn(
        modifier.islandFeather(), state = state, flingBehavior = snap,
        contentPadding = PaddingValues(vertical = ISLAND_CHIP_HEIGHT * 2f),
    ) {
        items(days, key = { it.toString() }) { day ->
            IslandChip(day, counts[day.toString()], day == selected, today) {
                onPoke()
                onPick(day)
            }
        }
    }
}
