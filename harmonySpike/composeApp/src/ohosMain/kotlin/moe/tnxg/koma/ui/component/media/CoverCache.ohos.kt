package moe.tnxg.koma.ui.component.media

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image

private const val CONNECT_TIMEOUT_MS = 15_000L
private const val MAX_COVER_BYTES = 16 * 1024 * 1024

/** 复用教务同款 Ktor Curl 引擎下载随机封面（服务端会 302 跳图，Curl 引擎默认跟随）。 */
private val coverClient = HttpClient(Curl) {
    followRedirects = true
    expectSuccess = false
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
        requestTimeoutMillis = CONNECT_TIMEOUT_MS * 2L
    }
}

/** OHOS 端封面下载：走已接通的 Ktor 网络层，替代 M1 时期的占位失败桩。 */
internal actual suspend fun downloadCoverBytes(url: String): ByteArray =
    withContext(Dispatchers.Default) {
        val bytes = coverClient.get(url).readBytes()
        check(bytes.isNotEmpty() && bytes.size <= MAX_COVER_BYTES) { "封面响应大小异常" }
        bytes
    }

/** 图片解码走 OHOS skiko 后端（与 iOS/Desktop 同源），行为与 Android 端一致。 */
internal actual suspend fun decodePhotoBytes(bytes: ByteArray): ImageBitmap =
    withContext(Dispatchers.Default) {
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }
