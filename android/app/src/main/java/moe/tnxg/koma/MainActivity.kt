package moe.tnxg.koma

import android.os.Bundle
import com.getcapacitor.BridgeActivity
import moe.tnxg.koma.plugins.EduNativeHttpPlugin
import moe.tnxg.koma.plugins.KomaCredentialsPlugin

class MainActivity : BridgeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // app-local 插件必须在 super.onCreate 之前注册，否则 Bridge 初始化时拿不到
        registerPlugin(EduNativeHttpPlugin::class.java)
        registerPlugin(KomaCredentialsPlugin::class.java)
        super.onCreate(savedInstanceState)
    }
}
