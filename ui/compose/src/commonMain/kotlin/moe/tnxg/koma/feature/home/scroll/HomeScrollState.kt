package moe.tnxg.koma.feature.home.scroll

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.domain.timeline.DayBlock
import moe.tnxg.koma.domain.timeline.TimelineBlock
import moe.tnxg.koma.domain.timeline.WeekBlock
import moe.tnxg.koma.feature.home.header.BlockGeometry
import moe.tnxg.koma.feature.home.header.DateGeometry
import moe.tnxg.koma.feature.home.header.headerMotion
import moe.tnxg.koma.ui.component.scroll.ScrollAnimationState

@Stable
internal class HomeScrollState(
    val list: LazyListState,
    initialBlocks: List<TimelineBlock>,
    private val density: Density,
) {
    var blocks by mutableStateOf(initialBlocks)
        private set
    private var indices = indexBlocks(initialBlocks)
    val navigation = ScrollAnimationState()
    val dateHeights = mutableStateMapOf<String, Float>()
    val dateGeometry = mutableStateMapOf<String, DateGeometry>()
    var headHeight by mutableFloatStateOf(89.3f)
    var initialPositioned by mutableStateOf(false)

    val motion by derivedStateOf {
        val info = list.layoutInfo
        val positions = buildMap {
            for (item in info.visibleItemsInfo) {
                val block = blocks.getOrNull(item.index) ?: continue
                if (block is WeekBlock) continue
                put(block.anchor, BlockGeometry(
                    with(density) { (item.offset - info.viewportStartOffset).toDp().value },
                    with(density) { item.size.toDp().value },
                    if (block is DayBlock) dateHeights[block.anchor] ?: 30f else 0f,
                ))
            }
            if (values.none { it.top <= headHeight + 1 }) {
                var index = (info.visibleItemsInfo.firstOrNull()?.index ?: 0) - 1
                while (index >= 0) {
                    val previous = blocks[index--]
                    if (previous !is WeekBlock) {
                        put(previous.anchor, BlockGeometry(-1f, 0f, 0f))
                        break
                    }
                }
            }
        }
        val topScroll = if (list.firstVisibleItemIndex > 0) 24f
            else with(density) { list.firstVisibleItemScrollOffset.toDp().value }
        val t = (topScroll / 24).coerceIn(0f, 1f)
        headerMotion(positions, 0f, headHeight, navigation.running)
            .copy(veil = 1 - (1 - t) * (1 - t))
    }

    fun update(next: List<TimelineBlock>) {
        if (blocks !== next) {
            indices = indexBlocks(next)
            blocks = next
        }
    }

    fun topOf(anchor: String?): Float {
        val index = indices[anchor] ?: return 0f
        val info = list.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return 0f
        return with(density) { (item.offset - info.viewportStartOffset).toDp().value }
    }

    suspend fun jump(anchor: String, animated: Boolean) {
        val index = indices[anchor] ?: return
        val block = blocks[index]
        val rowHeight = if (block is DayBlock) dateHeights[anchor] ?: 30f else 0f
        val offset = with(density) { (8f + rowHeight).dp.roundToPx() }
        if (!animated) return list.scrollToItem(index, offset)
        // Gliding across distant days composes every block in between, so only the last stretch is animated.
        val visible = list.layoutInfo.visibleItemsInfo
        val first = visible.firstOrNull()?.index ?: index
        val last = visible.lastOrNull()?.index ?: index
        if (index < first - GLIDE_ITEMS) list.scrollToItem(index + GLIDE_ITEMS)
        else if (index > last + GLIDE_ITEMS) list.scrollToItem(index - GLIDE_ITEMS)
        list.animateScrollToItem(index, offset)
    }

    private fun indexBlocks(items: List<TimelineBlock>): Map<String, Int> = buildMap {
        items.forEachIndexed { index, block -> if (block !is WeekBlock) put(block.anchor, index) }
    }

    private companion object {
        const val GLIDE_ITEMS = 2
    }
}
