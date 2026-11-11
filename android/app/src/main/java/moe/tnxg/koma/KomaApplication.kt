package moe.tnxg.koma

import android.app.Application
import moe.tnxg.koma.notifications.NotificationChannels

class KomaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 渠道随进程启动注册；幂等，已存在时不改用户设置
        NotificationChannels.ensureAll(this)
    }
}
