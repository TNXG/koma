package moe.tnxg.koma.data.education.http

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class HttpResult(val status: Int, val url: HttpUrl, val headers: Headers, val bytes: ByteArray) {
    val text: String get() = bytes.toString(Charsets.UTF_8)
}

/** Carries the rejected response so the login layer can show the server's own reason. */
internal class EduHttpException(val status: Int, val url: HttpUrl, val body: String) :
    IOException(httpStatusText(status))

private fun httpStatusText(status: Int): String = when (status) {
    401, 403 -> "登录已失效，请重新登录"
    404 -> "教务接口不存在，请更新应用后重试"
    423 -> "登录被暂时锁定，请稍后重试"
    429 -> "请求太频繁，请稍后重试"
    in 500..599 -> "教务服务暂时不可用，请稍后重试"
    else -> "教务请求没有完成，请稍后重试"
}

internal class EduHttp(private val client: OkHttpClient) {
    suspend fun call(url: String, body: RequestBody? = null, headers: Map<String, String> = emptyMap()): HttpResult {
        val target = url.toHttpUrl()
        require(target.isHttps) { "登录只允许安全连接" }
        val request = Request.Builder().url(target).header("User-Agent", USER_AGENT)
            .apply { headers.forEach { (key, value) -> header(key, value) } }
            .apply { if (body != null) post(body) }.build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        e.printStackTrace()
                        continuation.resumeWithException(IOException("教务连接失败，请检查网络后重试", e))
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val result = response.use { HttpResult(it.code, it.request.url, it.headers, it.body.bytes()) }
                        if (continuation.isActive) {
                            if (result.status >= 400) continuation.resumeWithException(
                                EduHttpException(result.status, result.url, result.text))
                            else continuation.resume(result)
                        }
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            })
        }
    }
    /** A stopped redirect returns its response without requesting the destination. */
    suspend fun follow(
        url: String,
        body: RequestBody? = null,
        headers: Map<String, String> = emptyMap(),
        stopAt: (HttpUrl) -> Boolean = { false },
    ): HttpResult {
        var target = url.toHttpUrl()
        var nextBody = body
        var nextHeaders = headers
        for (hop in 0..MAX_REDIRECTS) {
            val result = call(target.toString(), nextBody, nextHeaders)
            if (result.status !in REDIRECT_STATUSES) return result
            val location = result.headers["Location"] ?: return result
            val destination = result.url.resolve(location) ?: throw IOException("登录跳转地址无效")
            if (!destination.isHttps) throw IOException("登录只允许安全连接")
            val sameOrigin = result.url.scheme == destination.scheme && result.url.host == destination.host &&
                result.url.port == destination.port
            // 307/308 preserve the method and payload. Never replay a credential-bearing
            // payload on another origin; silently changing it to GET is not equivalent.
            if (!sameOrigin && nextBody != null && result.status in 307..308) {
                throw IOException("登录跳转不能向其他站点发送表单")
            }
            if (stopAt(destination)) return result
            if (hop == MAX_REDIRECTS) throw IOException("登录跳转次数过多，请重试")
            if (result.status in 301..303) {
                nextBody = null
                nextHeaders = nextHeaders.filterKeys { name ->
                    !name.equals("Content-Type", true) && !name.equals("Content-Length", true) &&
                        !name.equals("Transfer-Encoding", true)
                }
            }
            // Headers are caller-supplied and can contain credentials under any name.
            // Destination cookies are selected independently by CookieVault on each call.
            if (!sameOrigin) nextHeaders = emptyMap()
            target = destination
        }
        error("Unreachable redirect state")
    }
    companion object {
        private const val MAX_REDIRECTS = 16
        private val REDIRECT_STATUSES = setOf(301, 302, 303, 307, 308)
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
        fun client(vault: CookieVault): OkHttpClient = OkHttpClient.Builder().cookieJar(vault)
            .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS).build()
    }
}
