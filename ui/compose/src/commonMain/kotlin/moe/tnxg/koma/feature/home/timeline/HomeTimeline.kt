package moe.tnxg.koma.feature.home.timeline

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.domain.timeline.DayBlock
import moe.tnxg.koma.domain.timeline.FreeBlock
import moe.tnxg.koma.domain.timeline.TimelineBlock
import moe.tnxg.koma.domain.timeline.WeekBlock
import moe.tnxg.koma.feature.home.scroll.HomeScrollState

@Composable
internal fun HomeTimeline(blocks: List<TimelineBlock>, scroll: HomeScrollState,
    minutes: Int, viewIsToday: Boolean, backdrop: GraphicsLayer) {
    val density = LocalDensity.current
    val bottomInset = with(density) { WindowInsets.safeDrawing.getBottom(this).toDp() }
    LazyColumn(
        Modifier.fillMaxSize().drawWithContent {
            backdrop.record { this@drawWithContent.drawContent() }
            drawLayer(backdrop)
        },
        state = scroll.list,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp,
            top = (scroll.headHeight + 8).dp, bottom = 120.dp + bottomInset),
    ) {
        itemsIndexed(blocks, key = { _, block -> blockKey(block) },
            contentType = { _, block -> when (block) {
                is DayBlock -> 0; is FreeBlock -> 1; is WeekBlock -> 2
            } }) { _, block ->
            when (block) {
                is WeekBlock -> WeekDivider(block.week)
                is FreeBlock -> FreeDays(block)
                is DayBlock -> {
                    val progress by remember(scroll, block.anchor) {
                        derivedStateOf {
                            val motion = scroll.motion
                            if (motion.incoming == block.anchor) motion.progress else 0f
                        }
                    }
                    val merged by remember(scroll, block.anchor) {
                        derivedStateOf {
                            scroll.topOf(block.anchor) <= scroll.headHeight + 1 && !scroll.navigation.running
                        }
                    }
                    DaySection(block, minutes, viewIsToday, progress, merged,
                        { height -> if (scroll.dateHeights[block.anchor] != height) scroll.dateHeights[block.anchor] = height },
                        { geometry -> if (scroll.dateGeometry[block.anchor] != geometry) scroll.dateGeometry[block.anchor] = geometry })
                }
            }
        }
    }
}

private fun blockKey(block: TimelineBlock): String = when (block) {
    is DayBlock -> "day:${block.anchor}"
    is FreeBlock -> "free:${block.anchor}"
    is WeekBlock -> "week:${block.anchor}"
}
