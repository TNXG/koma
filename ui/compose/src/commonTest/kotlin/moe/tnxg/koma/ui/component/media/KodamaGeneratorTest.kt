package moe.tnxg.koma.ui.component.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KodamaGeneratorTest {
    @Test
    fun hashMatchesUpstreamJsBehaviorAndIsNonNegative() {
        assertEquals(0L, kodamaHash(""))
        assertEquals(kodamaHash("Sakura"), kodamaHash("Sakura"))
        val samples = listOf("a", "Sakura", "Atlas", "Luna", "Phoenix", "Steve", "2026-koma-seed-99999999")
        for (sample in samples) {
            assertTrue(kodamaHash(sample) >= 0L, "Expected non-negative hash for $sample")
        }
    }

    @Test
    fun oneSeedAlwaysResolvesToTheSameFace() {
        val seed = "0f1e2d3c4b5a69788796a5b4c3d2e1f0"
        val first = kodamaConfigForSeed(seed)
        val second = kodamaConfigForSeed(seed)
        assertEquals(first, second)
        assertEquals(resolveKodama(first).slots, resolveKodama(second).slots)
        assertEquals(resolveKodama(first).gradient, resolveKodama(second).gradient)
    }

    @Test
    fun differentSeedsVaryEveryDimension() {
        val configs = List(48) { kodamaConfigForSeed("seed-$it") }
        assertTrue(configs.map { it.background }.toSet().size > 1)
        assertTrue(configs.map { it.detailLevel }.toSet().size > 1)
        assertTrue(configs.map { it.depth }.toSet().size > 1)
        assertTrue(configs.map { it.mood }.toSet().size > 2)
        assertTrue(configs.map { it.animations }.toSet().size > 2)
        assertTrue(configs.all { it.animations.isNotEmpty() }, "every avatar keeps at least one animation")
    }

    @Test
    fun resolveKodamaAppliesMoodOverridesWithoutDroppingOtherDimensions() {
        val base = kodamaConfigForSeed("koma-test-seed")
            .copy(mood = KodamaMood.Cool, background = KodamaBackground.Solid)
        val resolved = resolveKodama(base)
        assertEquals(KodamaBackground.Solid, resolved.config.background)
        assertEquals(base.depth, resolved.config.depth)
        assertEquals(base.detailLevel, resolved.config.detailLevel)
        assertEquals(KodamaEyeType.Round, resolved.slots.eyes)
        assertEquals(KodamaAccessoryType.Sunglasses, resolved.slots.accessory)
        assertEquals(KodamaMouthType.Smirk, resolved.slots.mouth)
        assertTrue(resolved.gradient.fromArgb != resolved.gradient.toArgb)
    }

    @Test
    fun detailLevelGatesExpectedFeatureSlots() {
        assertTrue(canRenderKodamaSlot(KodamaSlotKind.Eyes, KodamaDetailLevel.Minimal))
        assertFalse(canRenderKodamaSlot(KodamaSlotKind.Mouth, KodamaDetailLevel.Minimal))
        assertTrue(canRenderKodamaSlot(KodamaSlotKind.Mouth, KodamaDetailLevel.Basic))
        assertFalse(canRenderKodamaSlot(KodamaSlotKind.Eyebrows, KodamaDetailLevel.Basic))
        assertTrue(canRenderKodamaSlot(KodamaSlotKind.Eyebrows, KodamaDetailLevel.Standard))
        assertFalse(canRenderKodamaSlot(KodamaSlotKind.Cheeks, KodamaDetailLevel.Standard))
        assertTrue(canRenderKodamaSlot(KodamaSlotKind.Cheeks, KodamaDetailLevel.Full))
        assertTrue(canRenderKodamaSlot(KodamaSlotKind.Accessory, KodamaDetailLevel.Full))
    }
}
