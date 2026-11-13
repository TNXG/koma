package moe.tnxg.koma.feature.home.header

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class HeaderGeometryTest {
    @Test
    fun flightStartsAtMeasuredSourceAndEndsAtScaledFontTarget() {
        // A 40dp measured title line at y=18 has its center at 38, not 33.6.
        assertEquals(172f, dateFlightOffset(172f, 38f, 0f))
        assertEquals(38f, dateFlightOffset(172f, 38f, 1f))
        assertEquals(105f, dateFlightOffset(172f, 38f, .5f))
    }

    @Test
    fun flightKeepsTheLeftEdgeAtTheMeasuredTitleInset() {
        assertEquals(86f, dateFlightOffset(86f, 24f, 0f))
        assertEquals(55f, dateFlightOffset(86f, 24f, .5f))
        assertEquals(24f, dateFlightOffset(86f, 24f, 1f))
    }

    @Test
    fun flightClampsOverscrollToItsMeasuredEndpoints() {
        assertEquals(172f, dateFlightOffset(172f, 38f, -1f))
        assertEquals(38f, dateFlightOffset(172f, 38f, 2f))
    }

    @Test
    fun flightTracksDragDistanceWithoutAccelerationOrLag() {
        assertEquals(80f, dateFlightOffset(100f, 20f, .25f))
        assertEquals(40f, dateFlightOffset(100f, 20f, .75f))
    }

    @Test
    fun flightScaleUsesResolvedFontSizesAtTheCurrentFontScale() {
        assertEquals(1f, dateFlightScale(20f, 44f, 0f))
        assertEquals(1.6f, dateFlightScale(20f, 44f, .5f))
        assertEquals(2.2f, dateFlightScale(20f, 44f, 1f))
    }

    @Test
    fun incomingStartsExactlySeventyTwoDpBeforeTheDock() {
        val positions = mapOf(
            "current" to BlockGeometry(0f, 200f, 30f),
            "next" to BlockGeometry(163f, 200f, 30f),
        )
        assertNull(headerMotion(positions, 0f, 90f, false).incoming)
        val middle = headerMotion(positions, 36f, 90f, false)
        assertEquals("current", middle.current)
        assertEquals("next", middle.incoming)
        assertEquals(.5f, middle.progress)
    }

    @Test
    fun rowAtTheDockSettlesInsteadOfRemainingAnIncomingGhost() {
        val positions = mapOf(
            "next" to BlockGeometry(163f, 200f, 30f),
            "previous" to BlockGeometry(0f, 200f, 30f),
            "later" to BlockGeometry(500f, 200f, 30f),
        )
        val motion = headerMotion(positions, 72f, 90f, false)
        assertEquals("next", motion.current)
        assertNull(motion.incoming)
        assertEquals(0f, motion.progress)
    }

    @Test
    fun programmaticGlideDoesNotFlyAnInterveningDate() {
        val positions = mapOf(
            "current" to BlockGeometry(0f, 200f, 30f),
            "next" to BlockGeometry(120f, 200f, 30f),
        )
        val motion = headerMotion(positions, 0f, 90f, true)
        assertNull(motion.incoming)
        assertEquals(0f, motion.progress)
    }

    @Test
    fun freeBlockCrossfadesWithoutAFlight() {
        val motion = headerMotion(mapOf(
            "free" to BlockGeometry(120f, 100f, 0f),
            "day" to BlockGeometry(0f, 100f, 30f),
        ), 0f, 90f, false)
        assertEquals("free", motion.incoming)
        assertFalse(motion.docks)
    }

    @Test
    fun veilClampsBothScrollBoundaries() {
        assertEquals(0f, headerMotion(emptyMap(), -4f, 90f, false).veil)
        assertEquals(.75f, headerMotion(emptyMap(), 12f, 90f, false).veil)
        assertEquals(1f, headerMotion(emptyMap(), 40f, 90f, false).veil)
    }
}
