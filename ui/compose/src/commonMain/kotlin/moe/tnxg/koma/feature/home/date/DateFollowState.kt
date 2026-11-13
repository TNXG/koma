package moe.tnxg.koma.feature.home.date

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class DateFollowState {
    private var manualScrolling by mutableStateOf(false)
    private var touchedAt by mutableStateOf<Long?>(null)

    fun scrollChanged(moving: Boolean, automatic: Boolean, now: Long) {
        val manual = moving && !automatic
        if (manual || manualScrolling) touchedAt = now
        manualScrolling = manual
    }

    fun requestRecenter() {
        touchedAt = null
    }

    // An active drag/fling owns the island, even after 1800 ms have elapsed.
    fun remainingHoldMillis(now: Long): Long? = if (manualScrolling) null else {
        touchedAt?.let { (1800L - (now - it)).coerceAtLeast(0L) } ?: 0L
    }
}
