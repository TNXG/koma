package moe.tnxg.koma

import android.app.Application
import moe.tnxg.koma.platform.AndroidPlatformStartup

class KomaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidPlatformStartup.initialize(this)
    }
}
