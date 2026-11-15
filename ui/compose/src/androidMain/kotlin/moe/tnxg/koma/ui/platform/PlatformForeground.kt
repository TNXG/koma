package moe.tnxg.koma.ui.platform

import android.app.Activity
import android.app.Application
import android.content.ContextWrapper
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun PlatformForeground(onForeground: () -> Unit) {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onForeground)
    DisposableEffect(context) {
        var current = context
        while (current is ContextWrapper && current !is Activity) current = current.baseContext
        val activity = current as? Activity
        val application = context.applicationContext as Application
        val observer = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(target: Activity) { if (target === activity) callback.value() }
            override fun onActivityCreated(target: Activity, state: Bundle?) {}
            override fun onActivityStarted(target: Activity) {}
            override fun onActivityPaused(target: Activity) {}
            override fun onActivityStopped(target: Activity) {}
            override fun onActivitySaveInstanceState(target: Activity, state: Bundle) {}
            override fun onActivityDestroyed(target: Activity) {}
        }
        application.registerActivityLifecycleCallbacks(observer)
        callback.value()
        onDispose { application.unregisterActivityLifecycleCallbacks(observer) }
    }
}
