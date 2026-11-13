package moe.tnxg.koma.platform.photo

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/** Registered during Activity construction so Android can restore a pending picker result. */
class PhotoPickerRequester(activity: ComponentActivity) : PhotoPickerHost {
    private val mutex = Mutex()
    private var pending: CancellableContinuation<Uri?>? = null
    private val launcher = activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val continuation = pending
        pending = null
        if (continuation?.isActive == true) continuation.resume(uri)
    }

    override suspend fun pickPhotoUri(): Uri? = mutex.withLock {
        suspendCancellableCoroutine { continuation ->
            pending = continuation
            continuation.invokeOnCancellation { if (pending === continuation) pending = null }
            try {
                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } catch (error: Exception) {
                pending = null
                continuation.cancel(error)
            }
        }
    }

    fun close() {
        pending?.cancel(CancellationException("选择照片的 Activity 已关闭"))
        pending = null
    }
}
