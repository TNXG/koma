package moe.tnxg.koma.ocr

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assume
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.DataInputStream
import java.util.zip.GZIPInputStream

/**
 * 真机核对本地识别链路：随包 assets 的模型 + 字符表 → 设备上的推理后端（NNAPI 或降级 CPU）。
 * 夹具与 JVM 测试同源（教务 CAS 验证码的 ARGB 像素），断言算出的答案而不是原始文本。
 */
@RunWith(AndroidJUnit4::class)
class OcrDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val engine by lazy { OcrEngine.shared(context) }

    /** 模型与样本都不入库：缺任意一个就跳过 */
    private fun assumeFixtures() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val test = InstrumentationRegistry.getInstrumentation().context
        Assume.assumeTrue("缺少 OCR 模型，先执行 pnpm run fetch:ocr", runCatching { app.assets.open("ocr/captcha.onnx").close() }.isSuccess)
        Assume.assumeTrue("缺少验证码样本夹具，跳过", runCatching { test.assets.open("ocr/s_1.bin").close() }.isSuccess)
    }

    @Test
    fun `设备上识别真实验证码`() {
        assumeFixtures()
        val cases = mapOf("s_1" to 32, "s_13" to 14, "t_44" to 0)
        for ((name, expected) in cases) {
            val (pixels, width, height) = load(name)
            val got = (0 until 2).firstNotNullOfOrNull { view ->
                answer(engine.recognize(pixels, width, height, view))
            }
            assertEquals("$name 解出的答案", expected, got)
        }
        assertTrue("后端应落在 nnapi 或 cpu：${engine.provider}", engine.provider == "nnapi" || engine.provider == "cpu")
    }

    private fun load(name: String): Triple<IntArray, Int, Int> {
        val input = DataInputStream(GZIPInputStream(InstrumentationRegistry.getInstrumentation().context.assets.open("ocr/$name.bin")))
        input.use {
            val magic = ByteArray(6)
            input.readFully(magic)
            require(magic.decodeToString() == "TTOCR1") { "$name 夹具格式不对" }
            val w = input.readInt()
            val h = input.readInt()
            val pixels = IntArray(w * h)
            for (i in pixels.indices) pixels[i] = input.readInt()
            return Triple(pixels, w, h)
        }
    }

    /** 与 TS 侧 captchaAnswer 同一套判定：等号截断、操作数 1..19、结果 0..38、取最长算式 */
    private fun answer(raw: String): Int? {
        val text = buildString {
            for (c in raw) {
                val mapped = CONFUSABLE[c] ?: c
                if (mapped in "0123456789+-=?") append(mapped)
            }
        }.substringBefore('=')
        var best: Int? = null
        var bestLen = 0
        for (m in PATTERN.findAll(text)) {
            val a = m.groupValues[1].toInt()
            val b = m.groupValues[3].toInt()
            if (a !in 1..19 || b !in 1..19) continue
            val value = if (m.groupValues[2] == "+") a + b else a - b
            if (value !in 0..38) continue
            if (m.value.length > bestLen) {
                bestLen = m.value.length
                best = value
            }
        }
        return best
    }

    companion object {
        private val CONFUSABLE = mapOf(
            'x' to '+', 'X' to '+', '×' to '+', '＊' to '+', '*' to '+', '十' to '+', '＋' to '+',
            '一' to '-', '－' to '-', '—' to '-', 'q' to '?', 'Q' to '?', '﹖' to '?', '？' to '?',
        )
        private val PATTERN = Regex("(\\d{1,2})([+-])(\\d{1,2})")
    }
}
