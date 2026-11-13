package moe.tnxg.koma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import moe.tnxg.koma.app.KomaApp
import moe.tnxg.koma.permission.NotificationPermissionRequester
import moe.tnxg.koma.platform.AndroidPlatformServices
import moe.tnxg.koma.platform.notification.NotificationPermissionHost

class MainActivity : ComponentActivity(), NotificationPermissionHost {
    private val notificationPermission = NotificationPermissionRequester(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KomaApp(remember { AndroidPlatformServices(this) }) }
    }

    override suspend fun requestNotificationPermission() = notificationPermission.request()

    override fun onDestroy() {
        notificationPermission.close()
        super.onDestroy()
    }
}
