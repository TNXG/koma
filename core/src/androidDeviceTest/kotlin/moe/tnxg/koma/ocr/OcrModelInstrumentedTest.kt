package moe.tnxg.koma.ocr

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the shipped model and tensor shapes without optional downloaded test fixtures. */
@RunWith(AndroidJUnit4::class)
class OcrModelInstrumentedTest {
    @Test
    fun shippedModelAcceptsOriginalAndCroppedPreprocessing() {
        val engine = OcrEngine.shared(ApplicationProvider.getApplicationContext<Context>())
        val pixels = IntArray(200 * 40) { 0xffffffff.toInt() }
        for (view in 0..1) {
            val text = engine.recognize(pixels, 200, 40, view)
            assertTrue("空白图像不应产生算式", text.isEmpty())
        }
        assertTrue(engine.provider == "cpu" || engine.provider == "nnapi")
    }
}
