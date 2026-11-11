package moe.tnxg.koma.ocr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import java.nio.FloatBuffer

/**
 * 验证码 OCR：ddddocr 默认模型（`assets/ocr/captcha.onnx`，MIT）+ ONNX Runtime 本地推理。
 * 优先 NNAPI（设备有 NPU/DSP 时交给系统调度），拿不到或跑不动一律退回 CPU：
 * 会话创建、首次推理任一失败都会重建 CPU 会话重试一次。识别全程离线，图片不出设备。
 *
 * 模型输入 1×1×64×W 灰度，输出 [W,1,C] 分类分数；这里取逐帧 argmax、按 CTC 去重，
 * 只留白名单字符（数字与运算符），其余丢弃——算式解析在 TS 侧（`domain/captcha.ts`）。
 */
internal class OcrEngine(
    private val model: ByteArray,
    charset: String,
    private val useNpu: Boolean,
) {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val table: IntArray = IntArray(CLASSES)
    private var session: OrtSession
    private var inputName: String
    var provider: String = "cpu"
        private set

    init {
        for (line in charset.lineSequence()) {
            val at = line.indexOf('\t')
            if (at <= 0) continue
            val index = line.substring(0, at).trim().toIntOrNull() ?: continue
            val text = line.substring(at + 1)
            if (index in table.indices && text.isNotEmpty()) table[index] = text.codePointAt(0)
        }
        val opened = open()
        session = opened.first
        provider = opened.second
        inputName = session.inputNames.first()
        /* 注意：这里以及构造路径上都不能碰 Android API（Log 也算），JVM 单测会直接构造本类 */
    }

    /** 当前会话的 provider；NPU 不可用时为 cpu */
    private fun open(): Pair<OrtSession, String> {
        if (useNpu) {
            try {
                return create(true) to "nnapi"
            } catch (_: Throwable) {
                /* 没有 NNAPI 实现或模型不支持：降级 */
            }
        }
        return create(false) to "cpu"
    }

    private fun create(nnapi: Boolean): OrtSession {
        val opts = OrtSession.SessionOptions()
        try {
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            if (nnapi) opts.addNnapi()
            return env.createSession(model, opts)
        } finally {
            opts.close()
        }
    }

    /**
     * 整图识别：ARGB 像素 → OCR 文本（可能为空串）。
     * view：0 = 原图；1 = 裁掉左右两端噪声再识别（主视图读不出来时换它，实测只补不回退）
     */
    fun recognize(pixels: IntArray, width: Int, height: Int, view: Int = 0): String {
        val (left, right) = VIEWS[view.coerceIn(0, VIEWS.size - 1)]
        val data = OcrImage.tensor(
            pixels, width, height,
            fromX = (width * left).toInt(),
            toX = width - (width * right).toInt(),
        )
        return try {
            infer(data)
        } catch (t: Throwable) {
            /* NPU 会话跑不动（算子不支持、驱动异常）：换 CPU 再试一次 */
            if (provider == "cpu") throw t
            Log.i(TAG, "推理失败，退回 CPU：${t.message}")
            val s = create(false)
            session.close()
            session = s
            provider = "cpu"
            infer(data)
        }
    }

    private fun infer(data: FloatArray): String = run(session, data)

    private fun run(session: OrtSession, data: FloatArray): String {
        val width = data.size / OcrImage.HEIGHT
        val shape = longArrayOf(1, 1, OcrImage.HEIGHT.toLong(), width.toLong())
        OnnxTensor.createTensor(env, FloatBuffer.wrap(data), shape).use { input ->
            session.run(mapOf(inputName to input)).use { out ->
                val tensor = out[0] as OnnxTensor
                val dims = tensor.info.shape
                val classes = dims.last().toInt()
                var frames = 1
                for (d in dims) frames *= d.toInt()
                frames /= classes
                return decode(tensor.floatBuffer, frames, classes)
            }
        }
    }

    /** 逐帧 argmax + CTC 去重；不在白名单的字符直接丢 */
    private fun decode(buf: FloatBuffer, frames: Int, classes: Int): String {
        val sb = StringBuilder()
        var prev = -1
        for (t in 0 until frames) {
            val base = t * classes
            var best = 0
            var bestScore = buf.get(base)
            for (c in 1 until classes) {
                val v = buf.get(base + c)
                if (v > bestScore) {
                    bestScore = v
                    best = c
                }
            }
            if (best == prev) continue
            prev = best
            val cp = if (best < table.size) table[best] else 0
            if (cp != 0) sb.appendCodePoint(cp)
        }
        return sb.toString()
    }

    companion object {
        private const val TAG = "KomaOcr"
        private const val CLASSES = 8210
        private const val MODEL = "ocr/captcha.onnx"
        private const val CHARSET = "ocr/charset.txt"

        /** 预处理视图：0 = 原图；1 = 左裁 2%、右裁 22%（去掉左边那撇噪声与末尾「=?」） */
        private val VIEWS = arrayOf(0f to 0f, 0.02f to 0.22f)

        @Volatile
        private var shared: OcrEngine? = null

        /** 进程内单例：13MB 模型只读一次、会话只建一次 */
        fun shared(context: Context): OcrEngine = shared ?: synchronized(this) {
            shared ?: OcrEngine(
                model = context.assets.open(MODEL).use { it.readBytes() },
                charset = context.assets.open(CHARSET).use { it.readBytes().decodeToString() },
                useNpu = true,
            ).also {
                shared = it
                Log.i(TAG, "OCR 会话就绪 provider=${it.provider}")
            }
        }
    }
}
