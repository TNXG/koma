package moe.tnxg.koma.feature.home.date

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IslandRevealTest {
    private val fling = 300f

    @Test
    fun slowDragSnapsToTheSidePastHalf() {
        assertTrue(islandRevealTarget(slide = .51f, velocity = 0f, fling = fling))
        assertFalse(islandRevealTarget(slide = .49f, velocity = 0f, fling = fling))
    }

    @Test
    fun leftwardFlingOpensEvenBeforeHalf() {
        assertTrue(islandRevealTarget(slide = .1f, velocity = -900f, fling = fling))
    }

    @Test
    fun rightwardFlingClosesEvenAfterHalf() {
        assertFalse(islandRevealTarget(slide = .9f, velocity = 900f, fling = fling))
    }

    @Test
    fun velocityAtTheThresholdDecides() {
        assertTrue(islandRevealTarget(slide = .3f, velocity = -fling, fling = fling))
        assertFalse(islandRevealTarget(slide = .7f, velocity = fling, fling = fling))
    }
}
