package moe.tnxg.koma.spike

import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import moe.tnxg.koma.ocr.OcrImage
import org.jetbrains.skia.Image
import platform.ArkTS.ArkTS_Napi_NativeModule.*
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64

/** All NAPI handles and pending callbacks are confined to the ArkTS main thread. */
internal object OhosCaptchaBridge {
    private var env: napi_env? = null
    private var recognizer: napi_ref? = null
    private var nextId = 0
    private val callbacks = mutableMapOf<Int, CancellableContinuation<String?>>()
    private val main = CoroutineScope(Dispatchers.Main)

    fun initialize(environment: napi_env, function: napi_value) {
        release()
        env = environment
        memScoped {
            val reference = alloc<napi_refVar>()
            check(napi_create_reference(env, function, 1u, reference.ptr) == napi_ok)
            recognizer = reference.value
        }
    }

    suspend fun recognize(dataUrl: String, view: Int): String? {
        val tensor = withContext(Dispatchers.Default) {
            val bytes = Base64.decode(dataUrl.substringAfter(','))
            val image = Image.makeFromEncoded(bytes)
            try {
                val pixels = IntArray(image.width * image.height)
                image.toComposeImageBitmap().readPixels(pixels)
                val from = if (view == 0) 0 else (image.width * .02f).toInt()
                val to = if (view == 0) image.width else image.width - (image.width * .22f).toInt()
                OcrImage.tensor(pixels, image.width, image.height, from, to)
            } finally { image.close() }
        }
        return withContext(Dispatchers.Main) {
            check(recognizer != null) { "验证码识别尚未初始化" }
            suspendCancellableCoroutine { continuation ->
                val id = ++nextId
                callbacks[id] = continuation
                continuation.invokeOnCancellation { main.launch { callbacks.remove(id) } }
                try {
                    send(id, tensor)
                } catch (error: Exception) {
                    callbacks.remove(id)
                    continuation.resume(null)
                }
            }
        }
    }

    private fun send(id: Int, tensor: FloatArray) = memScoped {
        val scope = alloc<napi_handle_scopeVar>()
        check(napi_open_handle_scope(env, scope.ptr) == napi_ok)
        try {
            val function = alloc<napi_valueVar>()
            val receiver = alloc<napi_valueVar>()
            val args = allocArray<napi_valueVar>(3)
            val data = alloc<COpaquePointerVar>()
            check(napi_get_reference_value(env, recognizer, function.ptr) == napi_ok)
            check(napi_get_undefined(env, receiver.ptr) == napi_ok)
            check(napi_create_int32(env, id, args) == napi_ok)
            check(napi_create_arraybuffer(env, (tensor.size * Float.SIZE_BYTES).convert(), data.ptr, args + 1) == napi_ok)
            tensor.usePinned { platform.posix.memcpy(data.value, it.addressOf(0), (tensor.size * Float.SIZE_BYTES).convert()) }
            check(napi_create_int32(env, tensor.size / OcrImage.HEIGHT, args + 2) == napi_ok)
            check(napi_call_function(env, receiver.value, function.value, 3u, args, null) == napi_ok)
        } finally { napi_close_handle_scope(env, scope.value) }
    }

    fun complete(id: Int, text: String?) {
        callbacks.remove(id)?.let { if (it.isActive) it.resume(text) }
    }

    fun release() {
        val pending = callbacks.values.toList()
        callbacks.clear()
        pending.forEach { it.cancel() }
        if (recognizer != null) napi_delete_reference(env, recognizer)
        recognizer = null
        env = null
    }
}

@CName("KomaCompleteCaptcha")
fun completeOhosCaptcha(id: Int, text: CPointer<ByteVar>?) = OhosCaptchaBridge.complete(id, text?.toKString())

@CName("KomaReleaseCaptcha")
fun releaseOhosCaptcha() = OhosCaptchaBridge.release()
