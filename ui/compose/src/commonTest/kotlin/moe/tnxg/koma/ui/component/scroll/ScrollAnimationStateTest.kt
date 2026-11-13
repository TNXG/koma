package moe.tnxg.koma.ui.component.scroll

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ScrollAnimationStateTest {
    @Test
    fun touchCancellationReleasesAutomaticTracking() = runTest {
        val state = ScrollAnimationState()
        val navigation = launch { state.run { awaitCancellation() } }
        runCurrent()
        assertTrue(state.running)
        navigation.cancelAndJoin()
        assertFalse(state.running)
    }

    @Test
    fun olderAnimationFinishingCannotReleaseNewerNavigation() = runTest {
        val state = ScrollAnimationState()
        val first = CompletableDeferred<Unit>()
        val second = CompletableDeferred<Unit>()
        val old = launch { state.run { first.await() } }
        runCurrent()
        val current = launch { state.run { second.await() } }
        runCurrent()
        first.complete(Unit)
        old.join()
        assertTrue(state.running)
        second.complete(Unit)
        current.join()
        assertFalse(state.running)
    }
}
