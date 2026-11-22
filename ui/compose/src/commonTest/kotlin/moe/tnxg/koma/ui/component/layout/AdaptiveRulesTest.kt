package moe.tnxg.koma.ui.component.layout

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdaptiveRulesTest {
    @Test
    fun compactPhonesStayFullBleed() {
        assertNull(AdaptiveRules.contentColumnWidth(359f))
        assertNull(AdaptiveRules.contentColumnWidth(599f))
    }

    @Test
    fun mediumAndWideWindowsCapTheContentColumnAt640dp() {
        assertEquals(640.dp, AdaptiveRules.contentColumnWidth(600f))
        assertEquals(640.dp, AdaptiveRules.contentColumnWidth(840f))
        assertEquals(640.dp, AdaptiveRules.contentColumnWidth(1280f))
    }

    @Test
    fun bottomSheetsCapTheirWidthAboveTheCompactTier() {
        assertNull(AdaptiveRules.bottomSheetWidth(599f))
        assertEquals(640.dp, AdaptiveRules.bottomSheetWidth(600f))
        assertEquals(640.dp, AdaptiveRules.bottomSheetWidth(1280f))
    }

    @Test
    fun zeroAndNegativeWidthsNeverLimit() {
        assertNull(AdaptiveRules.contentColumnWidth(0f))
        assertNull(AdaptiveRules.bottomSheetWidth(-1f))
    }

    @Test
    fun orientationDependentTiersMatchTheWindowShape() {
        // 平板竖屏：宽 ≥ 600 但不是横扁窗口 → 仍是单列限宽，不算分栏宽屏档。
        assertFalse(AdaptiveRules.isWide(768f, 1024f))
        // 横扁窗口（高宽比 < 1.2）按宽屏档处理。
        assertTrue(AdaptiveRules.isWide(900f, 700f))
        assertTrue(AdaptiveRules.isWide(1280f, 800f))
    }
}
