package moe.tnxg.koma.platform

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import moe.tnxg.koma.notifications.NotificationChannels
import moe.tnxg.koma.ocr.OcrEngine

/** Process-wide native initialization; OCR work never blocks the first Compose frame. */
object AndroidPlatformStartup {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun initialize(context: Context) {
        val app = context.applicationContext
        NotificationChannels.ensureAll(app)
        scope.launch {
            try {
                OcrEngine.shared(app)
            } catch (error: Exception) {
                Log.w("KomaOcr", "OCR 预加载失败：${error.javaClass.simpleName}")
            }
        }
    }
}
