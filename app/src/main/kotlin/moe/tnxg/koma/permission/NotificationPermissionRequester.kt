package moe.tnxg.koma.permission

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/** Registered during Activity construction so Android can restore pending permission results. */
class NotificationPermissionRequester(activity: ComponentActivity) {
    private val mutex = Mutex()
    private var pending: CancellableContinuation<Unit>? = null
    private val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        val continuation = pending
        pending = null
        if (continuation?.isActive == true) continuation.resume(Unit)
    }

    suspend fun request() = mutex.withLock {
        suspendCancellableCoroutine<Unit> { continuation ->
            pending = continuation
            continuation.invokeOnCancellation { if (pending === continuation) pending = null }
            try {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } catch (error: Exception) {
                pending = null
                continuation.cancel(error)
            }
        }
    }

    fun close() {
        pending?.cancel(CancellationException("授权 Activity 已关闭"))
        pending = null
    }
}
