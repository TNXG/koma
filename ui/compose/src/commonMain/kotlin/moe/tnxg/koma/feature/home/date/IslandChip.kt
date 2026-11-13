package moe.tnxg.koma.feature.home.date

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.calendar.weekdayLabel
import moe.tnxg.koma.feature.home.dock.dockPress
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

internal val ISLAND_CHIP_HEIGHT = 52.dp

@Composable
internal fun IslandChip(day: LocalDate, count: Int?, selected: Boolean, today: LocalDate, onPick: () -> Unit) {
    val c = KomaTheme.colors
    val past = day < today
    val onAlpha by animateFloatAsState(if (selected) 1f else .46f, tween(240))
    val number by animateColorAsState(if (selected) c.accent else if (past) c.ink3 else c.ink, tween(200))
    val weekday by animateColorAsState(if (selected) c.accent2 else if (past) c.ink4 else c.ink4, tween(200))
    val badge by animateColorAsState(if (selected) c.accent else c.ink5, tween(200))
    Box(
        Modifier.fillMaxWidth().height(ISLAND_CHIP_HEIGHT)
            .graphicsLayer { alpha = onAlpha }
            .background(if (selected) c.accentSoft else c.surface.copy(alpha = 0f), RoundedCornerShape(13.dp))
            .dockPress(1f, onPick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            KomaText(day.dayOfMonth.toString(), size = 16, lineHeight = 1.1f, weight = FontWeight.Bold, color = number)
            KomaText(if (day == today) "今" else weekdayLabel(day), Modifier.padding(top = 1.dp),
                size = 9, lineHeight = 1f, weight = FontWeight.Bold, color = weekday)
        }
        if (count != null && count > 0) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 9.dp, end = 8.dp).size(4.dp)
                .background(badge, CircleShape))
        }
    }
}
