package moe.tnxg.koma

import android.app.Application
import android.system.Os
import android.util.Log
import moe.tnxg.koma.platform.AndroidPlatformStartup

class KomaApplication : Application() {
    override fun onCreate() {
        // ONNX Runtime 1.29+ 官方包默认上传遥测，且在 native 初始化时读取该变量。
        // 必须早于任何 ContentProvider 与 OcrEngine 创建，才能连设备标识一起关掉。
        runCatching { Os.setenv("ORT_DISABLE_TELEMETRY", "1", true) }
            .onFailure { Log.w("Koma", "无法设置 ORT_DISABLE_TELEMETRY", it) }
        super.onCreate()
        AndroidPlatformStartup.initialize(this)
    }
}
