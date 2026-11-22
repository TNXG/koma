package moe.tnxg.koma

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import moe.tnxg.koma.app.KomaApp
import moe.tnxg.koma.permission.NotificationPermissionRequester
import moe.tnxg.koma.platform.AndroidPlatformServices
import moe.tnxg.koma.platform.notification.NotificationPermissionHost
import moe.tnxg.koma.platform.photo.PhotoPickerHost
import moe.tnxg.koma.platform.photo.PhotoPickerRequester

class MainActivity : ComponentActivity(), NotificationPermissionHost, PhotoPickerHost {
    private val notificationPermission = NotificationPermissionRequester(this)
    private val photoPicker = PhotoPickerRequester(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { it.remove() }
        }
        enableEdgeToEdge()
        setContent { KomaApp(remember { AndroidPlatformServices(this) }) }
    }

    override fun onStart() {
        super.onStart()
        (application as KomaApplication).todayWidgets.refresh()
    }

    override suspend fun requestNotificationPermission() = notificationPermission.request()

    override suspend fun pickPhotoUri(): Uri? = photoPicker.pickPhotoUri()

    override fun onDestroy() {
        notificationPermission.close()
        photoPicker.close()
        super.onDestroy()
    }
}
