package moe.tnxg.koma.spike

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.ArkTS.ArkTS_Napi_NativeModule.*
import kotlin.coroutines.resume

/**
 * 相册选图桥：Kotlin 发起请求（带 id），ArkTS 打开系统 picker，选中后回传 JPEG 字节。
 * 所有 NAPI 句柄与挂起回调都限定在 ArkTS 主线程（与 [OhosCaptchaBridge] 同一约定）。
 */
@OptIn(ExperimentalForeignApi::class)
internal object OhosPhotoPickerBridge {
    private var env: napi_env? = null
    private var picker: napi_ref? = null
    private var nextId = 0
    private val callbacks = mutableMapOf<Int, CancellableContinuation<ByteArray?>>()

    fun initialize(environment: napi_env, function: napi_value) {
        release()
        env = environment
        memScoped {
            val reference = alloc<napi_refVar>()
            check(napi_create_reference(env, function, 1u, reference.ptr) == napi_ok)
            picker = reference.value
        }
    }

    suspend fun pickPhotoBytes(): ByteArray? {
        if (picker == null) return null
        return suspendCancellableCoroutine { continuation ->
            val id = ++nextId
            callbacks[id] = continuation
            try {
                send(id)
            } catch (_: Exception) {
                callbacks.remove(id)
                continuation.resume(null)
            }
        }
    }

    private fun send(id: Int) = memScoped {
        val scope = alloc<napi_handle_scopeVar>()
        check(napi_open_handle_scope(env, scope.ptr) == napi_ok)
        try {
            val function = alloc<napi_valueVar>()
            val receiver = alloc<napi_valueVar>()
            val args = allocArray<napi_valueVar>(1)
            check(napi_get_reference_value(env, picker, function.ptr) == napi_ok)
            check(napi_get_undefined(env, receiver.ptr) == napi_ok)
            napi_create_int32(env, id, args)
            check(napi_call_function(env, receiver.value, function.value, 1u, args, null) == napi_ok)
        } finally {
            napi_close_handle_scope(env, scope.value)
        }
    }

    fun complete(id: Int, bytes: ByteArray?) {
        callbacks.remove(id)?.let { if (it.isActive) it.resume(bytes) }
    }

    fun release() {
        val pending = callbacks.values.toList()
        callbacks.clear()
        pending.forEach { it.cancel() }
        if (picker != null) napi_delete_reference(env, picker)
        picker = null
        env = null
    }
}

@kotlin.native.CName("KomaCompletePhotoPick")
@OptIn(ExperimentalForeignApi::class)
fun completeOhosPhotoPick(id: Int, data: CPointer<ByteVar>?, size: Int) {
    val bytes = if (data == null || size <= 0) null else ByteArray(size) { data[it].toByte() }
    OhosPhotoPickerBridge.complete(id, bytes)
}

@kotlin.native.CName("KomaReleasePhotoPick")
fun releaseOhosPhotoPick() = OhosPhotoPickerBridge.release()
