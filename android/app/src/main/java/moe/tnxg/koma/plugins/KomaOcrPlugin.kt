package moe.tnxg.koma.plugins

import android.graphics.BitmapFactory
import android.util.Base64
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.tnxg.koma.ocr.OcrEngine

/**
 * 验证码 OCR 桥：把 data URI / base64 图片交给本地 ONNX 模型，回识别出的文字。
 * 图片只在内存里过一道，不落盘、不上传；模型随包分发（ddddocr，MIT）。
 */
@CapacitorPlugin(name = "KomaOcr")
class KomaOcrPlugin : Plugin() {

    @PluginMethod
    fun isAvailable(call: PluginCall) {
        call.resolve(JSObject().put("available", true))
    }

    /** { image, view } → { text, provider }；view = 0 原图 / 1 裁掉两端噪声，provider = nnapi / cpu */
    @PluginMethod
    fun recognize(call: PluginCall) {
        val image = call.getString("image")
        if (image.isNullOrBlank()) {
            call.reject("缺少图片")
            return
        }
        val view = call.getInt("view") ?: 0
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    val decoded = decode(image)
                    val engine = OcrEngine.shared(app)
                    val text = engine.recognize(decoded.first, decoded.second, decoded.third, view)
                    text to engine.provider
                }
                call.resolve(JSObject().put("text", result.first).put("provider", result.second))
            } catch (t: Throwable) {
                call.reject(t.message ?: "识别失败")
            }
        }
    }

    /** data URI 或裸 base64 → ARGB 像素 + 宽高 */
    private fun decode(image: String): Triple<IntArray, Int, Int> {
        val base64 = if (image.startsWith("data:")) image.substringAfter(',', "") else image
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalArgumentException("图片解不开")
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            return Triple(pixels, bitmap.width, bitmap.height)
        } finally {
            bitmap.recycle()
        }
    }
}
