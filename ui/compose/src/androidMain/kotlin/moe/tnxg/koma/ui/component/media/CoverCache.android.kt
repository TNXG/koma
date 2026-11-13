package moe.tnxg.koma.ui.component.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import moe.tnxg.koma.platform.photo.sampleSizeFor
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

private const val MAX_REDIRECTS = 8
private const val CONNECT_TIMEOUT_MS = 15_000
private const val READ_TIMEOUT_MS = 25_000
private const val MAX_COVER_BYTES = 16 * 1024 * 1024
private const val MAX_DECODE_DIMENSION = 1440

internal actual suspend fun downloadCoverBytes(url: String): ByteArray =
    withContext(Dispatchers.IO) { downloadWithRedirects(url) }

internal actual suspend fun decodePhotoBytes(bytes: ByteArray): ImageBitmap =
    withContext(Dispatchers.Default) { decodeSampledBitmap(bytes) }

private suspend fun downloadWithRedirects(initialUrl: String): ByteArray {
    var currentUrl = URL(initialUrl)
    for (hop in 0..MAX_REDIRECTS) {
        coroutineContext.ensureActive()
        if (currentUrl.protocol != "https" && currentUrl.protocol != "http") {
            throw IOException("不支持的壁纸协议: ${currentUrl.protocol}")
        }
        val connection = (currentUrl.openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            useCaches = false
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept", "image/*")
            setRequestProperty("User-Agent", "Koma/0.2.0 (Android)")
        }
        val cancelHook = coroutineContext[Job]?.invokeOnCompletion { cause ->
            if (cause != null) {
                runCatching { connection.disconnect() }
            }
        }
        try {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == HttpURLConnection.HTTP_SEE_OTHER ||
                code == 307 ||
                code == 308
            ) {
                val location = connection.getHeaderField("Location")
                    ?: throw IOException("壁纸跳转地址缺失")
                if (hop == MAX_REDIRECTS) {
                    throw IOException("壁纸跳转次数过多")
                }
                currentUrl = URL(currentUrl, location)
                continue
            }
            if (code !in 200..299) {
                throw IOException("壁纸请求失败 ($code)")
            }
            return readBoundedBytes(connection)
        } finally {
            cancelHook?.dispose()
            runCatching { connection.disconnect() }
        }
    }
    throw IOException("壁纸跳转次数过多")
}

private suspend fun readBoundedBytes(connection: HttpURLConnection): ByteArray {
    val contentLength = connection.contentLengthLong
    if (contentLength > MAX_COVER_BYTES) {
        throw IOException("壁纸文件过大")
    }
    val initialCapacity = if (contentLength in 1..MAX_COVER_BYTES) contentLength.toInt() else 64 * 1024
    val buffer = ByteArrayOutputStream(initialCapacity)
    val chunk = ByteArray(8192)
    connection.inputStream.use { input ->
        while (true) {
            coroutineContext.ensureActive()
            val read = input.read(chunk)
            if (read < 0) break
            if (buffer.size() + read > MAX_COVER_BYTES) {
                throw IOException("壁纸文件过大")
            }
            buffer.write(chunk, 0, read)
        }
    }
    val bytes = buffer.toByteArray()
    if (bytes.isEmpty()) throw IOException("壁纸响应为空")
    return bytes
}

private fun decodeSampledBitmap(bytes: ByteArray): ImageBitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw IOException("图片格式无法解码")
    }
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_DECODE_DIMENSION)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        ?: throw IOException("图片解码失败")
    return bitmap.asImageBitmap()
}
