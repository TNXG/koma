package moe.tnxg.koma.ui.component.scroll

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class ScrollAnimationState {
    var running by mutableStateOf(false)
        private set
    private var generation = 0L

    suspend fun run(animation: suspend () -> Unit) {
        val current = ++generation
        running = true
        try {
            animation()
        } finally {
            if (generation == current) running = false
        }
    }
}
