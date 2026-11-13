package moe.tnxg.koma.feature.home.date

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DateFollowStateTest {
    @Test
    fun longManualGestureCannotBeInterruptedWhenInitialHoldExpires() {
        val follow = DateFollowState()
        follow.scrollChanged(moving = true, automatic = false, now = 100)
        assertNull(follow.remainingHoldMillis(now = 2500))
        follow.scrollChanged(moving = false, automatic = false, now = 2600)
        assertEquals(1800L, follow.remainingHoldMillis(now = 2600))
        assertEquals(1L, follow.remainingHoldMillis(now = 4399))
        assertEquals(0L, follow.remainingHoldMillis(now = 4400))
    }

    @Test
    fun cancelledAutomaticScrollBecomesManualUntilGestureAndFlingEnd() {
        val follow = DateFollowState()
        follow.scrollChanged(moving = true, automatic = true, now = 100)
        assertEquals(0L, follow.remainingHoldMillis(now = 200))
        follow.scrollChanged(moving = true, automatic = false, now = 300)
        assertNull(follow.remainingHoldMillis(now = 2200))
        follow.scrollChanged(moving = false, automatic = false, now = 2300)
        assertEquals(1800L, follow.remainingHoldMillis(now = 2300))
    }

    @Test
    fun automaticCenteringDoesNotRenewManualHold() {
        val follow = DateFollowState()
        follow.scrollChanged(moving = true, automatic = true, now = 100)
        follow.scrollChanged(moving = false, automatic = true, now = 200)
        follow.scrollChanged(moving = false, automatic = false, now = 200)
        assertEquals(0L, follow.remainingHoldMillis(now = 200))
    }

    @Test
    fun explicitRecenterClearsHoldWithoutTakingAnActiveGesture() {
        val follow = DateFollowState()
        follow.scrollChanged(moving = true, automatic = false, now = 100)
        follow.requestRecenter()
        assertNull(follow.remainingHoldMillis(now = 200))
        follow.scrollChanged(moving = false, automatic = false, now = 300)
        assertEquals(1800L, follow.remainingHoldMillis(now = 300))
        follow.requestRecenter()
        assertEquals(0L, follow.remainingHoldMillis(now = 300))
    }
}
