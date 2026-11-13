package moe.tnxg.koma.platform.photo

import kotlin.test.Test
import kotlin.test.assertEquals

class PhotoSamplingTest {
    @Test
    fun sampleSizeDownsamplesLargeDimensionsToPowerOfTwo() {
        assertEquals(1, sampleSizeFor(width = 1080, height = 1440, maxDimension = 1440))
        assertEquals(2, sampleSizeFor(width = 2880, height = 2880, maxDimension = 1440))
        // 6000 / 4 = 1500 still exceeds the decode budget.
        assertEquals(8, sampleSizeFor(width = 6000, height = 4000, maxDimension = 1440))
        assertEquals(1, sampleSizeFor(width = 0, height = 0, maxDimension = 1440))
    }
}
