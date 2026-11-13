package moe.tnxg.koma.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrImageTest {
    @Test
    fun normalizesSolidBlackAndWhiteWithoutChangingAspectRatio() {
        val black = OcrImage.tensor(IntArray(8) { 0xff000000.toInt() }, 4, 2)
        val white = OcrImage.tensor(IntArray(8) { 0xffffffff.toInt() }, 4, 2)
        assertEquals(8192, black.size)
        assertEquals(8192, white.size)
        assertTrue(black.all { it == 0f })
        assertTrue(white.all { it == 1f })
    }

    @Test
    fun croppingExcludesNoiseColumns() {
        val pixels = intArrayOf(0xffffffff.toInt(), 0xff000000.toInt(), 0xff000000.toInt(), 0xffffffff.toInt())
        val cropped = OcrImage.tensor(pixels, 4, 1, 1, 3)
        assertEquals(8192, cropped.size)
        assertTrue(cropped.all { it == 0f })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidDimensions() {
        OcrImage.tensor(intArrayOf(), 0, 0)
    }
}
